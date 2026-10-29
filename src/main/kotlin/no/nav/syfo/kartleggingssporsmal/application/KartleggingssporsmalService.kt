package no.nav.syfo.kartleggingssporsmal.application

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.syfo.kartleggingssporsmal.domain.*
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidatStatusendring.Ferdigbehandlet.VurderingAlternativ
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt.Companion.KARTLEGGINGSSPORSMAL_MINIMUM_NUMBER_OF_DAYS_LEFT_IN_OPPFOLGINGSTILFELLE
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt.Companion.KARTLEGGINGSSPORSMAL_STOPPUNKT_LIMIT_DAYS_EVEN_IF_FEW_DAYS_LEFT
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt.Companion.KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.behandlendeenhet.Enhet
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.pdl.model.getAlder
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.vedtak14a.Vedtak14aResponseDTO
import no.nav.syfo.senoppfolging.application.SenOppfolgingService
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.util.toLocalDateOslo
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

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

        if (shouldProcess(behandlendeEnhet?.enhetId)) {
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
        val stoppunkterWithEnhet = findEnhetForStoppunkter(unprocessedStoppunkter, callId)

        return stoppunkterWithEnhet.map { (stoppunktId, stoppunkt, enhet) ->
            runCatching {
                val isKandidat = if (shouldProcess(enhet?.enhetId)) {
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
                    log.warn("Stoppunkt with uuid ${stoppunkt.uuid} is no longer valid, enhet is missing or excluded")
                    false
                }

                if (isKandidat) {
                    val skjemavariant = if (enhet?.enhetId in pilotkontorerWithFritekst) {
                        Skjemavariant.FLERVALG_FRITEKST_V4
                    } else {
                        Skjemavariant.FLERVALG_V3
                    }
                    val kandidat = KartleggingssporsmalKandidat.create(
                        personident = stoppunkt.personident,
                        skjemavariant = skjemavariant,
                        shouldSendVarsel = shouldSendVarsel(enhet?.enhetId),
                    )
                    val persistedKandidat = kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                        kandidat = kandidat,
                        stoppunktId = stoppunktId,
                    )
                    publishKandidatAndSendVarsel(persistedKandidat)
                } else {
                    kartleggingssporsmalRepository.markStoppunktAsProcessed(stoppunktId)
                }
                stoppunkt
            }
        }
    }

    private suspend fun publishKandidatAndSendVarsel(kandidat: KartleggingssporsmalKandidat) {
        when {
            !isKandidatPublishingEnabled ->
                log.info("Kandidat publishing is disabled, not sending kandidat with uuid ${kandidat.uuid} to kafka topic")
            kandidat.shouldSendVarsel ->
                kartleggingssporsmalKandidatProducer.send(kandidat = kandidat).map {
                    kartleggingssporsmalRepository.updatePublishedAtForKandidatStatusendring(kandidat)
                    sendVarsel(kandidat)
                }
        }
    }

    suspend fun processKandidaterWithMissingPublishOrVarsel(): List<Result<KartleggingssporsmalKandidat>> {
        return if (!isKandidatPublishingEnabled) {
            log.info("Kandidat publishing is disabled, skipping recovery of missing publish/varsel")
            emptyList()
        } else {
            val kandidater = kartleggingssporsmalRepository.getKandidaterWithMissingPublishOrVarsel()
            kandidater.map { kandidat ->
                runCatching {
                    if (kandidat.shouldSendVarsel) {
                        if (kandidat.status.publishedAt == null) {
                            kartleggingssporsmalKandidatProducer.send(kandidat).map {
                                kartleggingssporsmalRepository.updatePublishedAtForKandidatStatusendring(kandidat)
                            }.getOrThrow()
                        }
                        if (kandidat.varsletAt == null) {
                            sendVarsel(kandidat)
                        }
                    }
                    kandidat
                }
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
                .map { kandidat ->
                    val ferdigstiltKandidat = kandidat.ferdigstillVarsel()
                    kartleggingssporsmalRepository.updateVarselFerdigstiltAtForKandidat(ferdigstiltKandidat)
                }
        }
    }

    suspend fun registrerFerdigbehandlet(
        uuid: UUID,
        veilederident: String,
        vurderingAlternativ: VurderingAlternativ,
        vurderingFritekst: String? = null,
    ): KartleggingssporsmalKandidat {
        val existingKandidat =
            kartleggingssporsmalRepository.getKandidat(uuid) ?: throw IllegalArgumentException("Kandidat med uuid $uuid finnes ikke")
        val ferdigbehandletKandidat = existingKandidat.ferdigbehandleVurdering(
            veilederident = veilederident,
            vurderingAlternativ = vurderingAlternativ,
            vurderingFritekst = vurderingFritekst,
        )

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

    private fun shouldProcess(enhetId: String?) = enhetId != null && enhetId !in ekskluderteKontorer

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
        private const val KONTOR_NAV_NITTEDAL = "0233"
        private const val KONTOR_NAV_AURSKOG_HOLAND = "0221"
        private const val KONTOR_NAV_FREDRIKSTAD = "0106"
        private const val KONTOR_NAV_SANDNES = "1102"
        private const val KONTOR_NAV_SKIEN = "0806"
        private const val KONTOR_NAV_TONSBERG = "0704"
        private const val KONTOR_NAV_VESTRE_VARANGER = "2028"

        // Trio-fylker: Troms og Finnmark
        private const val KONTOR_NAV_TROMSO = "1902"
        private const val KONTOR_NAV_SALANGEN_LAVANGEN_DYROY = "1923"
        private const val KONTOR_NAV_MALSELV_BARDU = "1924"
        private const val KONTOR_NAV_BALSFJORD_STORFJORD = "1933"
        private const val KONTOR_NAV_KARLSOY = "1936"
        private const val KONTOR_NAV_LYNGEN = "1938"
        private const val KONTOR_NAV_GAIVUOTNA_KAFJORD = "1940"
        private const val KONTOR_NAV_SKJERVOY = "1941"
        private const val KONTOR_NAV_NORDREISA = "1942"
        private const val KONTOR_NAV_VARDO = "2002"
        private const val KONTOR_NAV_VADSO = "2003"
        private const val KONTOR_NAV_HAMMERFEST_MASOY = "2004"
        private const val KONTOR_NAV_AVJOVARRI = "2011"
        private const val KONTOR_NAV_ALTA_KVANANGEN_LOPPA = "2012"
        private const val KONTOR_NAV_HASVIK = "2015"
        private const val KONTOR_NAV_NORDKAPP = "2019"
        private const val KONTOR_NAV_PORSANGER = "2020"
        private const val KONTOR_NAV_LEBESBY = "2022"
        private const val KONTOR_NAV_GAMVIK = "2023"

        // Trio-fylker: Vestland
        private const val KONTOR_NAV_VINDAFJORD_ETNE = "1160"
        private const val KONTOR_NAV_BERGEN_SOR = "1202"
        private const val KONTOR_NAV_BERGEN_NORD = "1203"
        private const val KONTOR_NAV_BERGEN_VEST = "1206"
        private const val KONTOR_NAV_SVEIO = "1216"
        private const val KONTOR_NAV_BOMLO = "1219"
        private const val KONTOR_NAV_STORD = "1221"
        private const val KONTOR_NAV_FITJAR = "1222"
        private const val KONTOR_NAV_TYSNES = "1223"
        private const val KONTOR_NAV_KVINNHERAD = "1224"
        private const val KONTOR_NAV_ULLENSVANG = "1228"
        private const val KONTOR_NAV_EIDFJORD = "1232"
        private const val KONTOR_NAV_ULVIK = "1233"
        private const val KONTOR_NAV_VOSS = "1235"
        private const val KONTOR_NAV_KVAM = "1238"
        private const val KONTOR_NAV_SAMNANGER = "1242"
        private const val KONTOR_NAV_BJORNAFJORDEN = "1243"
        private const val KONTOR_NAV_AUSTEVOLL = "1244"
        private const val KONTOR_NAV_OYGARDEN = "1246"
        private const val KONTOR_NAV_ASKOY = "1247"
        private const val KONTOR_NAV_VAKSDAL = "1251"
        private const val KONTOR_NAV_KINN = "1401"
        private const val KONTOR_NAV_ASKVOLL = "1428"
        private const val KONTOR_NAV_SUNNFJORD_YTRE_SOGN = "1432"
        private const val KONTOR_NAV_NORDFJORD = "1443"

        // Trio-fylker: Vest-Viken
        private const val KONTOR_NAV_BAERUM = "0219"
        private const val KONTOR_NAV_JEVNAKER = "0532"
        private const val KONTOR_NAV_DRAMMEN = "0602"
        private const val KONTOR_NAV_KONGSBERG = "0604"
        private const val KONTOR_NAV_HOLE = "0612"
        private const val KONTOR_NAV_HALLINGDAL = "0617"
        private const val KONTOR_NAV_MIDT_BUSKERUD = "0623"
        private const val KONTOR_NAV_NUMEDAL = "0632"

        const val KONTOR_NAV_UTLAND = "0393"

        val pilotkontorerMedVarsel = listOf(
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
            KONTOR_NAV_NITTEDAL,
            KONTOR_NAV_AURSKOG_HOLAND,
            KONTOR_NAV_FREDRIKSTAD,
            KONTOR_NAV_SANDNES,
            KONTOR_NAV_SKIEN,
            KONTOR_NAV_TONSBERG,
            KONTOR_NAV_VESTRE_VARANGER,
            KONTOR_NAV_TROMSO,
            KONTOR_NAV_SALANGEN_LAVANGEN_DYROY,
            KONTOR_NAV_MALSELV_BARDU,
            KONTOR_NAV_BALSFJORD_STORFJORD,
            KONTOR_NAV_KARLSOY,
            KONTOR_NAV_LYNGEN,
            KONTOR_NAV_GAIVUOTNA_KAFJORD,
            KONTOR_NAV_SKJERVOY,
            KONTOR_NAV_NORDREISA,
            KONTOR_NAV_VARDO,
            KONTOR_NAV_VADSO,
            KONTOR_NAV_HAMMERFEST_MASOY,
            KONTOR_NAV_AVJOVARRI,
            KONTOR_NAV_ALTA_KVANANGEN_LOPPA,
            KONTOR_NAV_HASVIK,
            KONTOR_NAV_NORDKAPP,
            KONTOR_NAV_PORSANGER,
            KONTOR_NAV_LEBESBY,
            KONTOR_NAV_GAMVIK,
            KONTOR_NAV_VINDAFJORD_ETNE,
            KONTOR_NAV_BERGEN_SOR,
            KONTOR_NAV_BERGEN_NORD,
            KONTOR_NAV_BERGEN_VEST,
            KONTOR_NAV_SVEIO,
            KONTOR_NAV_BOMLO,
            KONTOR_NAV_STORD,
            KONTOR_NAV_FITJAR,
            KONTOR_NAV_TYSNES,
            KONTOR_NAV_KVINNHERAD,
            KONTOR_NAV_ULLENSVANG,
            KONTOR_NAV_EIDFJORD,
            KONTOR_NAV_ULVIK,
            KONTOR_NAV_VOSS,
            KONTOR_NAV_KVAM,
            KONTOR_NAV_SAMNANGER,
            KONTOR_NAV_BJORNAFJORDEN,
            KONTOR_NAV_AUSTEVOLL,
            KONTOR_NAV_OYGARDEN,
            KONTOR_NAV_ASKOY,
            KONTOR_NAV_VAKSDAL,
            KONTOR_NAV_KINN,
            KONTOR_NAV_ASKVOLL,
            KONTOR_NAV_SUNNFJORD_YTRE_SOGN,
            KONTOR_NAV_NORDFJORD,
            KONTOR_NAV_BAERUM,
            KONTOR_NAV_JEVNAKER,
            KONTOR_NAV_DRAMMEN,
            KONTOR_NAV_KONGSBERG,
            KONTOR_NAV_HOLE,
            KONTOR_NAV_HALLINGDAL,
            KONTOR_NAV_MIDT_BUSKERUD,
            KONTOR_NAV_NUMEDAL,
        )
        val ekskluderteKontorer = listOf(KONTOR_NAV_UTLAND)
        val pilotkontorerWithFritekst = listOf(KONTOR_NAV_SANDEFJORD, KONTOR_NAV_ASKER, KONTOR_NAV_SONDRE_NORDSTRAND)
        private const val OPPARBEIDE_NY_SYKEPENGERETT_WEEKS = 26L
    }
}
