package no.nav.syfo.kartleggingssporsmal.application

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidatStatusendring
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt.Companion.KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt.Companion.KARTLEGGINGSSPORSMAL_MINIMUM_NUMBER_OF_DAYS_LEFT_IN_OPPFOLGINGSTILFELLE
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt.Companion.KARTLEGGINGSSPORSMAL_STOPPUNKT_LIMIT_DAYS_EVEN_IF_FEW_DAYS_LEFT
import no.nav.syfo.kartleggingssporsmal.domain.Oppfolgingstilfelle
import no.nav.syfo.kartleggingssporsmal.domain.Skjemavariant
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.behandlendeenhet.Enhet
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.pdl.model.getAlder
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.vedtak14a.Vedtak14aResponseDTO
import no.nav.syfo.senoppfolging.application.SenOppfolgingService
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.util.toLocalDateOslo
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class KartleggingssporsmalService(
    private val behandlendeEnhetClient: IBehandlendeEnhetClient,
    private val kartleggingssporsmalRepository: IKartleggingssporsmalRepository,
    private val oppfolgingstilfelleClient: IOppfolgingstilfelleClient,
    private val esyfoVarselProducer: IEsyfovarselProducer,
    private val kartleggingssporsmalKandidatProducer: IKartleggingssporsmalKandidatProducer,
    private val pdlClient: IPdlClient,
    private val vedtak14aClient: IVedtak14aClient,
    private val senOppfolgingService: SenOppfolgingService,
    private val isKandidatPublishingEnabled: Boolean,
) {

    suspend fun processOppfolgingstilfelle(oppfolgingstilfelle: Oppfolgingstilfelle.OppfolgingstilfelleFromKafka) {
        val behandlendeEnhet = behandlendeEnhetClient.getEnhet(
            callId = UUID.randomUUID().toString(),
            personident = oppfolgingstilfelle.personident,
        ).let { response ->
            if (response == null) {
                log.error("Mangler enhet for person med oppfolgingstilfelle-uuid: ${oppfolgingstilfelle.uuid}")
                null
            } else {
                response.oppfolgingsenhetDTO?.enhet
                    ?: response.geografiskEnhet
            }
        }

        if (isInPilot(behandlendeEnhet?.enhetId)) {
            val kartleggingssporsmalStoppunkt: KartleggingssporsmalStoppunkt? = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)

            if (kartleggingssporsmalStoppunkt != null) {
                kartleggingssporsmalRepository.createStoppunkt(stoppunkt = kartleggingssporsmalStoppunkt)

                log.info("Oppfolgingstilfelle with uuid: ${oppfolgingstilfelle.uuid} has generated a stoppunkt.")
            } else {
                log.info("Oppfolgingstilfelle with uuid: ${oppfolgingstilfelle.uuid} is not relevant for kartleggingssporsmal")
            }
        }
    }

    suspend fun processStoppunkter(): List<Result<KartleggingssporsmalStoppunkt>> {
        val unprocessedStoppunkter = kartleggingssporsmalRepository.getUnprocessedStoppunkter()
        val callId = UUID.randomUUID().toString()

        // De kan ha flyttet i tiden mellom stoppunktet ble laget og nå, og da skal de ikke bli kandidat
        val pilotStoppunkter = findEnhetForStoppunkter(unprocessedStoppunkter, callId)

        return pilotStoppunkter.map { (stoppunktId, stoppunkt, enhet) ->
            runCatching {
                val isKandidat = if (isInPilot(enhet?.enhetId)) {
                    coroutineScope {
                        val oppfolgingstilfelleRequest = async {
                            oppfolgingstilfelleClient.getOppfolgingstilfelle(
                                personident = stoppunkt.personident,
                                callId = callId,
                            )
                        }

                        val pdlRequest = async {
                            pdlClient.getPerson(stoppunkt.personident)
                        }

                        val vedtak14aRequest = async {
                            vedtak14aClient.hentGjeldende14aVedtak(stoppunkt.personident)
                        }

                        val oppfolgingstilfelle = oppfolgingstilfelleRequest.await().getOrThrow()
                        val pdlPerson = pdlRequest.await().getOrThrow()
                        val vedtak14a = vedtak14aRequest.await().getOrThrow()

                        isKandidat(
                            oppfolgingstilfelle = oppfolgingstilfelle,
                            alder = pdlPerson.getAlder(),
                            vedtak14a = vedtak14a,
                        )
                    }
                } else {
                    log.warn("Stoppunkt with uuid ${stoppunkt.uuid} is not longer valid for pilot")
                    false
                }

                if (isKandidat) {
                    val skjemavariant = if (enhet?.enhetId in pilotkontorerWithFritekstSkjema) {
                        Skjemavariant.FLERVALG_FRITEKST_V1
                    } else {
                        Skjemavariant.FLERVALG_V1
                    }
                    val kandidat = KartleggingssporsmalKandidat.create(
                        personident = stoppunkt.personident,
                        skjemavariant = skjemavariant,
                    )
                    val persistedKandidat = kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                        kandidat = kandidat,
                        stoppunktId = stoppunktId,
                    )
                    if (isKandidatPublishingEnabled && shouldSendVarsel(enhet?.enhetId)) {
                        kartleggingssporsmalKandidatProducer.send(
                            kandidat = persistedKandidat,
                        ).map {
                            kartleggingssporsmalRepository.updatePublishedAtForKandidatStatusendring(persistedKandidat)
                            sendVarsel(persistedKandidat)
                        }
                    } else {
                        log.info("Kandidat publishing is disabled, not sending kandidat with uuid ${kandidat.uuid} to kafka topic")
                    }
                } else {
                    kartleggingssporsmalRepository.markStoppunktAsProcessed(stoppunktId)
                }
                stoppunkt
            }
        }
    }

    suspend fun registrerSvar(kandidatUuid: UUID, svarAt: OffsetDateTime, svarId: UUID) {
        val existingKandidat = kartleggingssporsmalRepository.getKandidat(kandidatUuid)

        if (existingKandidat == null) {
            log.error("Mottok svar på kandidat som ikke finnes, med uuid: $kandidatUuid og svarId: $svarId")
        } else {
            val mottattSvarKandidat = existingKandidat.registrerSvarMottatt(svarAt)
            kartleggingssporsmalRepository.createKandidatStatusendring(kandidat = mottattSvarKandidat)
            kartleggingssporsmalKandidatProducer.send(mottattSvarKandidat)
                .map { kandidat ->
                    kartleggingssporsmalRepository.updatePublishedAtForKandidatStatusendring(kandidat)
                }

            esyfoVarselProducer.ferdigstillKartleggingssporsmalVarsel(mottattSvarKandidat)
        }
    }

    suspend fun registrerFerdigbehandlet(
        uuid: UUID,
        veilederident: String,
    ): KartleggingssporsmalKandidat {
        val existingKandidat =
            kartleggingssporsmalRepository.getKandidat(uuid) ?: throw IllegalArgumentException("Kandidat med uuid $uuid finnes ikke")
        val ferdigbehandletKandidat = existingKandidat.ferdigbehandleVurdering(veilederident)

        val updatedKandidat = kartleggingssporsmalRepository.createKandidatStatusendring(kandidat = ferdigbehandletKandidat)
        kartleggingssporsmalKandidatProducer.send(updatedKandidat)
            .map { kandidat ->
                kartleggingssporsmalRepository.updatePublishedAtForKandidatStatusendring(kandidat)
            }
        return updatedKandidat
    }

    private suspend fun sendVarsel(kandidat: KartleggingssporsmalKandidat) {
        val varsletKandidat = kandidat.varsle()
        esyfoVarselProducer.sendKartleggingssporsmal(varsletKandidat).map {
            kartleggingssporsmalRepository.updateVarsletAtForKandidat(it)
        }
    }

    private suspend fun findEnhetForStoppunkter(
        unprocessedStoppunkter: List<Pair<Int, KartleggingssporsmalStoppunkt>>,
        callId: String,
    ): List<Triple<Int, KartleggingssporsmalStoppunkt, Enhet?>> {
        return unprocessedStoppunkter.map { (stoppunktId, stoppunkt) ->
            val behandlendeEnhet = behandlendeEnhetClient.getEnhet(
                callId = callId,
                personident = stoppunkt.personident,
            ).let { response ->
                if (response == null) {
                    log.error("Mangler enhet for person med stoppunkt-uuid: ${stoppunkt.uuid}")
                    null
                } else {
                    response.oppfolgingsenhetDTO?.enhet
                        ?: response.geografiskEnhet
                }
            }

            Triple(stoppunktId, stoppunkt, behandlendeEnhet)
        }
    }

    private suspend fun isKandidat(
        oppfolgingstilfelle: Oppfolgingstilfelle.OppfolgingstilfelleFromApi?,
        alder: Int?,
        vedtak14a: Vedtak14aResponseDTO?,
    ): Boolean {
        return oppfolgingstilfelle != null &&
            oppfolgingstilfelle.isActive() &&
            !oppfolgingstilfelle.isDod() &&
            oppfolgingstilfelle.isArbeidstakerAtTilfelleEnd &&
            oppfolgingstilfelle.durationInDays() >= KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS &&
            isYoungerThan67(alder) &&
            !hasFewDaysLeftInOppfolgingstilfelle(oppfolgingstilfelle) &&
            !hasGjeldende14aVedtak(vedtak14a) &&
            !isAlreadyKandidatInTilfelle(oppfolgingstilfelle) &&
            !hasReceivedSenOppfolgingVarselRecently(oppfolgingstilfelle.personident)
    }

    private suspend fun isAlreadyKandidatInTilfelle(oppfolgingstilfelle: Oppfolgingstilfelle.OppfolgingstilfelleFromApi): Boolean {
        val existingKandidat = getLatestKandidat(oppfolgingstilfelle.personident)
        return existingKandidat != null &&
            oppfolgingstilfelle.datoInsideTilfelle(
                dato = existingKandidat.createdAt.toLocalDateOslo()
            )
    }

    private fun isYoungerThan67(alder: Int?): Boolean = alder != null && alder < 67

    private fun hasFewDaysLeftInOppfolgingstilfelle(oppfolgingstilfelle: Oppfolgingstilfelle): Boolean =
        oppfolgingstilfelle.durationInDays() <= KARTLEGGINGSSPORSMAL_STOPPUNKT_LIMIT_DAYS_EVEN_IF_FEW_DAYS_LEFT &&
            oppfolgingstilfelle.tilfelleEnd <= LocalDate.now().plusDays(KARTLEGGINGSSPORSMAL_MINIMUM_NUMBER_OF_DAYS_LEFT_IN_OPPFOLGINGSTILFELLE)

    private fun hasGjeldende14aVedtak(vedtak14a: Vedtak14aResponseDTO?): Boolean = vedtak14a != null

    private fun hasReceivedSenOppfolgingVarselRecently(personident: Personident): Boolean {
        val senOppfolgingKandidat = senOppfolgingService.getKandidater(personident).firstOrNull()
        return if (senOppfolgingKandidat?.varselAt != null) {
            val thresholdDateTime = OffsetDateTime.now().minusWeeks(OPPARBEIDE_NY_SYKEPENGERETT_WEEKS)
            senOppfolgingKandidat.varselAt.isAfter(thresholdDateTime)
        } else {
            false
        }
    }

    private fun isInPilot(enhetId: String?) = enhetId in pilotkontorer

    private fun shouldSendVarsel(enhetId: String?) = enhetId in pilotkontorerMedVarsel

    suspend fun getLatestKandidat(personident: Personident): KartleggingssporsmalKandidat? {
        return kartleggingssporsmalRepository.getLatestKandidat(personident)
    }

    suspend fun getKandidat(uuid: UUID): KartleggingssporsmalKandidat? {
        return kartleggingssporsmalRepository.getKandidat(uuid)
    }

    suspend fun getKandidatur(personident: Personident): List<KartleggingssporsmalKandidat> {
        return kartleggingssporsmalRepository.getKandidatur(personident)
    }

    suspend fun getKandidatStatus(kandidatUuid: UUID): List<KartleggingssporsmalKandidatStatusendring> =
        kartleggingssporsmalRepository.getKandidatStatusendringer(kandidatUuid)

    companion object {
        private val log = LoggerFactory.getLogger(KartleggingssporsmalService::class.java)
        private const val KONTOR_NAV_LIER = "0626"
        private const val KONTOR_NAV_ASKER = "0220"
        private const val KONTOR_NAV_GRORUD = "0328"
        private const val KONTOR_NAV_NORDSTRAND = "0318"
        private const val KONTOR_NAV_SONDRE_NORDSTRAND = "0319"
        private const val KONTOR_NAV_MADLA = "1165"
        private const val KONTOR_NAV_GJESDAL = "1122"
        private const val KONTOR_NAV_SANDEFJORD = "0710"
        private const val KONTOR_NAV_HOLMESTRAND = "5301"
        private const val KONTOR_NAV_PORSGRUNN = "0805"
        private const val KONTOR_NAV_OVRE_EIKER = "0624"
        private const val KONTOR_NAV_RINGERIKE = "0605"
        private const val KONTOR_NAV_ARENDAL = "0906"
        private const val KONTOR_NAV_MIDT_AGDER = "1014"
        private const val KONTOR_NAV_RANA = "1833"
        private const val KONTOR_NAV_LUROY = "1834"
        private const val KONTOR_NAV_NARVIK = "1805"
        private const val KONTOR_NAV_BODO = "1804"
        private const val KONTOR_NAV_SOR_TROMS = "1903"
        private const val KONTOR_NAV_SOR_VARANGER = "2030"
        private const val KONTOR_NAV_SENJA_SORREISA = "1931"
        private const val KONTOR_NAV_ALESUND = "1504"
        private const val KONTOR_NAV_MOLDE = "1502"
        private const val KONTOR_NAV_KRISTIANSUND = "1505"
        private const val KONTOR_NAV_GJOVIK = "0502"
        private const val KONTOR_NAV_ELVERUM = "0427"
        private const val KONTOR_NAV_SYKEFRAVARSENTER_INNLANDET = "0491"
        private const val KONTOR_NAV_LERKENDAL = "5702"
        private const val KONTOR_NAV_FALKENBORG = "5701"
        private const val KONTOR_NAV_VARNES = "1783"
        private const val KONTOR_NAV_FOSEN = "1630"
        private const val KONTOR_NAV_BERGEN_SENTRUM = "1208"
        private const val KONTOR_NAV_AURLAND_LARDAL = "1422"
        private const val KONTOR_NAV_SOGNDAL = "1420"
        private const val KONTOR_NAV_ARDAL = "1424"
        private const val KONTOR_NAV_LUSTER = "1426"
        private const val KONTOR_NAV_VIK = "1417"
        private const val KONTOR_NAV_ALVER = "1263"
        private const val KONTOR_NAV_OSTEROY = "1253"
        private const val KONTOR_NAV_MASFJORDEN = "1266"
        private const val KONTOR_NAV_SOLUND = "1412"
        private val pilotkontorerMedVarsel = listOf(
            KONTOR_NAV_LIER,
            KONTOR_NAV_ASKER,
            KONTOR_NAV_GRORUD,
            KONTOR_NAV_NORDSTRAND,
            KONTOR_NAV_SONDRE_NORDSTRAND,
            KONTOR_NAV_SANDEFJORD,
            KONTOR_NAV_HOLMESTRAND,
            KONTOR_NAV_PORSGRUNN,
            KONTOR_NAV_MADLA,
            KONTOR_NAV_GJESDAL,
            KONTOR_NAV_OVRE_EIKER,
            KONTOR_NAV_RINGERIKE,
            KONTOR_NAV_ARENDAL,
            KONTOR_NAV_MIDT_AGDER,
            KONTOR_NAV_RANA,
            KONTOR_NAV_LUROY,
            KONTOR_NAV_NARVIK,
            KONTOR_NAV_BODO,
            KONTOR_NAV_SOR_TROMS,
            KONTOR_NAV_SOR_VARANGER,
            KONTOR_NAV_SENJA_SORREISA,
            KONTOR_NAV_ALESUND,
            KONTOR_NAV_MOLDE,
            KONTOR_NAV_KRISTIANSUND,
            KONTOR_NAV_GJOVIK,
            KONTOR_NAV_ELVERUM,
            KONTOR_NAV_SYKEFRAVARSENTER_INNLANDET,
            KONTOR_NAV_LERKENDAL,
            KONTOR_NAV_FALKENBORG,
            KONTOR_NAV_VARNES,
            KONTOR_NAV_FOSEN,
        )
        private val pilotkontorer = listOf(
            KONTOR_NAV_BERGEN_SENTRUM,
            KONTOR_NAV_AURLAND_LARDAL,
            KONTOR_NAV_SOGNDAL,
            KONTOR_NAV_ARDAL,
            KONTOR_NAV_LUSTER,
            KONTOR_NAV_VIK,
            KONTOR_NAV_ALVER,
            KONTOR_NAV_OSTEROY,
            KONTOR_NAV_MASFJORDEN,
            KONTOR_NAV_SOLUND,
        ) + pilotkontorerMedVarsel
        private val pilotkontorerWithFritekstSkjema = listOf(KONTOR_NAV_SANDEFJORD)
        private const val OPPARBEIDE_NY_SYKEPENGERETT_WEEKS = 26L
    }
}
