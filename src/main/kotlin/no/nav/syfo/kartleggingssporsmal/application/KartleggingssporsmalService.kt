package no.nav.syfo.kartleggingssporsmal.application

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.syfo.kartleggingssporsmal.domain.*
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt.Companion.KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.pdl.model.getAlder
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.vedtak14a.Vedtak14aResponseDTO
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.util.toLocalDateOslo
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.*
import kotlin.IllegalArgumentException

class KartleggingssporsmalService(
    private val behandlendeEnhetClient: IBehandlendeEnhetClient,
    private val kartleggingssporsmalRepository: IKartleggingssporsmalRepository,
    private val oppfolgingstilfelleClient: IOppfolgingstilfelleClient,
    private val esyfoVarselProducer: IEsyfovarselProducer,
    private val kartleggingssporsmalKandidatProducer: IKartleggingssporsmalKandidatProducer,
    private val pdlClient: IPdlClient,
    private val vedtak14aClient: IVedtak14aClient,
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
        } else {
            log.info("Oppfolgingstilfelle with uuid: ${oppfolgingstilfelle.uuid} is not in pilot")
        }
    }

    suspend fun processStoppunkter(): List<Result<KartleggingssporsmalStoppunkt>> {
        val unprocessedStoppunkter = kartleggingssporsmalRepository.getUnprocessedStoppunkter()
        val callId = UUID.randomUUID().toString()

        // De kan ha flyttet i tiden mellom stoppunktet ble laget og nå, og da skal de ikke bli kandidat
        val pilotStoppunkter = findPilotStoppunkter(unprocessedStoppunkter, callId)

        return pilotStoppunkter.map { (stoppunktId, stoppunkt, isInPilot) ->
            runCatching {
                val isKandidat = if (isInPilot) {
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
                    false
                }

                if (isKandidat) {
                    val kandidat = KartleggingssporsmalKandidat(
                        personident = stoppunkt.personident,
                        status = KandidatStatus.KANDIDAT
                    )
                    val persistedKandidat = kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                        kandidat = kandidat,
                        stoppunktId = stoppunktId,
                    )
                    if (isKandidatPublishingEnabled) {
                        val statusEndring = kartleggingssporsmalRepository.getKandidatStatusendringer(
                            persistedKandidat.uuid
                        ).firstOrNull()
                            ?: throw IllegalStateException("Klarte ikke finne statusendring for kandidat ${persistedKandidat.uuid}")
                        if (
                            kartleggingssporsmalKandidatProducer.send(
                                kandidat = persistedKandidat,
                                statusEndring = statusEndring,
                            ).isSuccess
                        ) {
                            kartleggingssporsmalRepository.updatePublishedAtForKandidatStatusendring(statusEndring)
                            if (esyfoVarselProducer.sendKartleggingssporsmal(persistedKandidat).isSuccess) {
                                kartleggingssporsmalRepository.updateVarsletAtForKandidat(persistedKandidat)
                            }
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
            val statusendring = KartleggingssporsmalKandidatStatusendring(status = KandidatStatus.SVAR_MOTTATT, svarAt = svarAt)
            val kandidat = existingKandidat.registrerStatusEndring(statusendring)
            val createdStatusendring = kartleggingssporsmalRepository.createKandidatSvar(kandidat, statusendring)
            kartleggingssporsmalKandidatProducer.send(kandidat, createdStatusendring)
                .map { kandidat ->
                    kartleggingssporsmalRepository.updatePublishedAtForKandidatStatusendring(createdStatusendring)
                }
        }
    }

    suspend fun registrerFerdigBehandlet(personIdent: Personident) {
        val existingKandidat = kartleggingssporsmalRepository.getKandidat(personIdent)

        if (existingKandidat == null || existingKandidat.status == KandidatStatus.FERDIG_BEHANDLET) {
            throw IllegalArgumentException("Kandidat finnes ikke, eller er allerede ferdig behandlet")
        } else {
            val statusendring = KartleggingssporsmalKandidatStatusendring(status = KandidatStatus.FERDIG_BEHANDLET)
            val updatedKandidat = existingKandidat.registrerStatusEndring(statusendring)
            val createdStatusendring = kartleggingssporsmalRepository.createKandidatSvar(updatedKandidat, statusendring)
            kartleggingssporsmalKandidatProducer.send(updatedKandidat, createdStatusendring)
                .map { kandidat ->
                    kartleggingssporsmalRepository.updatePublishedAtForKandidatStatusendring(createdStatusendring)
                }
        }
    }

    private suspend fun findPilotStoppunkter(
        unprocessedStoppunkter: List<Pair<Int, KartleggingssporsmalStoppunkt>>,
        callId: String,
    ): List<Triple<Int, KartleggingssporsmalStoppunkt, Boolean>> {
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

            val isInPilot = isInPilot(behandlendeEnhet?.enhetId)
            if (!isInPilot) {
                log.warn("Stoppunkt with uuid ${stoppunkt.uuid} is not longer valid for pilot")
            }

            Triple(stoppunktId, stoppunkt, isInPilot)
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
            !hasGjeldende14aVedtak(vedtak14a) &&
            !isAlreadyKandidatInTilfelle(oppfolgingstilfelle)
    }

    private suspend fun isAlreadyKandidatInTilfelle(oppfolgingstilfelle: Oppfolgingstilfelle.OppfolgingstilfelleFromApi): Boolean {
        val existingKandidat = kartleggingssporsmalRepository.getKandidat(oppfolgingstilfelle.personident)
        return existingKandidat != null &&
            oppfolgingstilfelle.datoInsideTilfelle(
                dato = existingKandidat.createdAt.toLocalDateOslo()
            )
    }

    private fun isYoungerThan67(alder: Int?): Boolean = alder != null && alder < 67

    private fun hasGjeldende14aVedtak(vedtak14a: Vedtak14aResponseDTO?): Boolean = vedtak14a != null

    private fun isInPilot(enhetId: String?) = enhetId in pilotkontorer

    suspend fun getKandidat(personident: Personident): KartleggingssporsmalKandidat? {
        return kartleggingssporsmalRepository.getKandidat(personident)
    }

    companion object {
        private val log = LoggerFactory.getLogger(KartleggingssporsmalService::class.java)
        private const val KONTOR_NAV_LIER = "0626"
        private const val KONTOR_NAV_ASKER = "0220"
        private val pilotkontorer = listOf(KONTOR_NAV_LIER, KONTOR_NAV_ASKER)
    }
}
