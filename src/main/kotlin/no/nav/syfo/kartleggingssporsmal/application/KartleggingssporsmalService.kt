package no.nav.syfo.kartleggingssporsmal.application

import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.kartleggingssporsmal.domain.Oppfolgingstilfelle
import no.nav.syfo.shared.util.toLocalDateOslo
import org.slf4j.LoggerFactory
import java.util.*

class KartleggingssporsmalService(
    private val behandlendeEnhetClient: IBehandlendeEnhetClient,
    private val kartleggingssporsmalRepository: IKartleggingssporsmalRepository,
) {

    suspend fun processOppfolgingstilfelle(oppfolgingstilfelle: Oppfolgingstilfelle) {
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

    suspend fun processStoppunkt(): List<Result<KartleggingssporsmalStoppunkt>> {
        val unprocessed = kartleggingssporsmalRepository.getUnprocessedStoppunkt()
        return unprocessed.map {
            runCatching {
                log.info("Found stoppunkt to process: ${it.uuid}")
                // TODO: Process stoppunkt and set as processed
                it
            }
        }
    }

    private suspend fun isAlreadyKandidatInTilfelle(oppfolgingstilfelle: Oppfolgingstilfelle): Boolean {
        val existingKandidat = kartleggingssporsmalRepository.getKandidat(oppfolgingstilfelle.personident)
        return existingKandidat != null &&
            oppfolgingstilfelle.datoInsideCurrentTilfelle(
                dato = existingKandidat.createdAt.toLocalDateOslo()
            )
    }

    private fun isInPilot(enhetId: String?) = enhetId in pilotkontorer

    companion object {
        private val log = LoggerFactory.getLogger(KartleggingssporsmalService::class.java)
        private const val KONTOR_NAV_LIER = "0626"
        private const val KONTOR_NAV_ASKER = "0220"
        private val pilotkontorer = listOf(KONTOR_NAV_LIER, KONTOR_NAV_ASKER)
    }
}
