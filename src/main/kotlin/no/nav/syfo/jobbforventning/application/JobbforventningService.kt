package no.nav.syfo.jobbforventning.application

import kotlinx.coroutines.runBlocking
import no.nav.syfo.jobbforventning.domain.Oppfolgingstilfelle
import no.nav.syfo.shared.util.DAYS_IN_WEEK
import org.slf4j.LoggerFactory
import java.util.*

class JobbforventningService(
    private val behandlendeEnhetClient: IBehandlendeEnhetClient,
) {

    // TODO: Change return type after testing is done and we start persisting data
    fun processOppfolgingstilfelle(oppfolgingstilfelle: Oppfolgingstilfelle): Boolean {
        return if (isRelevantForPlanlagtKandidat(oppfolgingstilfelle)) {
            log.info("Oppfolgingstilfelle with uuid: ${oppfolgingstilfelle.uuid} is relevant for planlagt kandidat")
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
        return oppfolgingstilfelle.durationInDays() >= JOBBFORVENTNING_STOPPUNKT_START_DAYS &&
            oppfolgingstilfelle.durationInDays() <= JOBBFORVENTNING_STOPPUNKT_END_DAYS
    }

    private fun isInPilot(enhetId: String) = enhetId in pilotkontorer

    companion object {
        private val log = LoggerFactory.getLogger(JobbforventningService::class.java)
        private const val PILOTKONTOR_TEST = "0314"
        private val pilotkontorer = listOf(PILOTKONTOR_TEST)

        // TODO: Move to Jobbforventning domain class
        const val JOBBFORVENTNING_STOPPUNKT_START_DAYS = 6L * DAYS_IN_WEEK
        const val JOBBFORVENTNING_STOPPUNKT_INTERVAL_DAYS = 30L
        const val JOBBFORVENTNING_STOPPUNKT_END_DAYS =
            JOBBFORVENTNING_STOPPUNKT_START_DAYS + JOBBFORVENTNING_STOPPUNKT_INTERVAL_DAYS
    }
}
