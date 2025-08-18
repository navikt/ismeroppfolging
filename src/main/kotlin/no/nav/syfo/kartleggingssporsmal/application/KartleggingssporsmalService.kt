package no.nav.syfo.kartleggingssporsmal.application

import kotlinx.coroutines.runBlocking
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt.Companion.KARTLEGGINGSSPORSMAL_STOPPUNKT_END_DAYS
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt.Companion.KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS
import no.nav.syfo.kartleggingssporsmal.domain.Oppfolgingstilfelle
import org.slf4j.LoggerFactory
import java.util.*

class KartleggingssporsmalService(
    private val behandlendeEnhetClient: IBehandlendeEnhetClient,
) {

    // TODO: Change return type after testing is done and we start persisting data
    fun processOppfolgingstilfelle(oppfolgingstilfelle: Oppfolgingstilfelle): Boolean {
        return if (isRelevantForPlanlagtKandidat(oppfolgingstilfelle)) {
            val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt(
                personident = oppfolgingstilfelle.personident,
                tilfelleBitReferanseUuid = oppfolgingstilfelle.tilfelleBitReferanseUuid,
                tilfelleStart = oppfolgingstilfelle.tilfelleStart,
                tilfelleEnd = oppfolgingstilfelle.tilfelleEnd,
            )
            log.info(
                """
                Oppfolgingstilfelle with uuid: ${oppfolgingstilfelle.uuid} has generated a stoppunkt.
                Stoppunkt dato: ${kartleggingssporsmalStoppunkt.stoppunktAt}
                Tilfelle start: ${oppfolgingstilfelle.tilfelleStart}
                Tilfelle end: ${oppfolgingstilfelle.tilfelleEnd}
                Antall sykedager: ${oppfolgingstilfelle.antallSykedager}
                """.trimIndent()
            )
            true
        } else {
            log.info("Oppfolgingstilfelle with uuid: ${oppfolgingstilfelle.uuid} is not relevant for planlagt kandidat")
            false
        }
    }

    private fun isRelevantForPlanlagtKandidat(
        oppfolgingstilfelle: Oppfolgingstilfelle,
    ): Boolean {
        val enhet = runBlocking {
            val behandlendeEnhetDTO = behandlendeEnhetClient.getEnhet(
                callId = UUID.randomUUID().toString(),
                personident = oppfolgingstilfelle.personident,
            ) ?: throw IllegalStateException("Mangler enhet for person med oppfolgingstilfelle-uuid: ${oppfolgingstilfelle.uuid}")

            behandlendeEnhetDTO.oppfolgingsenhetDTO?.enhet
                ?: behandlendeEnhetDTO.geografiskEnhet
        }
        return oppfolgingstilfelleInsideStoppunktInterval(oppfolgingstilfelle) &&
            !oppfolgingstilfelle.isDod() &&
            !oppfolgingstilfelle.hasTilfelleWithEndMoreThanThirtyDaysAgo() &&
            isInPilot(enhet.enhetId)
    }

    private fun oppfolgingstilfelleInsideStoppunktInterval(oppfolgingstilfelle: Oppfolgingstilfelle): Boolean {
        // TODO: Hva med de som er før start-tidspunkt på intervallet, og så får en veldig lang sykmelding som strekker seg over intervallet?
        return oppfolgingstilfelle.durationInDays() >= KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS &&
            oppfolgingstilfelle.durationInDays() <= KARTLEGGINGSSPORSMAL_STOPPUNKT_END_DAYS
    }

    private fun isInPilot(enhetId: String) = enhetId in pilotkontorer

    companion object {
        private val log = LoggerFactory.getLogger(KartleggingssporsmalService::class.java)
        private const val PILOTKONTOR_TEST = "0314"
        private val pilotkontorer = listOf(PILOTKONTOR_TEST)
    }
}
