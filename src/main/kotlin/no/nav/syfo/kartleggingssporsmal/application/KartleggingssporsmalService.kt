package no.nav.syfo.kartleggingssporsmal.application

import kotlinx.coroutines.runBlocking
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.kartleggingssporsmal.domain.Oppfolgingstilfelle
import no.nav.syfo.shared.util.DAYS_IN_WEEK
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class KartleggingssporsmalService(
    private val behandlendeEnhetClient: IBehandlendeEnhetClient,
    private val kartleggingssporsmalRepository: IKartleggingssporsmalRepository,
) {

    fun processOppfolgingstilfelle(oppfolgingstilfelle: Oppfolgingstilfelle) {
        if (isRelevantForPlanlagtKandidat(oppfolgingstilfelle)) {
            val stoppunkt = calculateStoppunktDato(
                tilfelleStart = oppfolgingstilfelle.tilfelleStart,
                tilfelleEnd = oppfolgingstilfelle.tilfelleEnd,
            )
            log.info(
                """
                Oppfolgingstilfelle with uuid: ${oppfolgingstilfelle.uuid} is relevant for planlagt kandidat
                Stoppunkt dato: $stoppunkt
                Tilfelle start: ${oppfolgingstilfelle.tilfelleStart}
                Tilfelle end: ${oppfolgingstilfelle.tilfelleEnd}
                Antall sykedager: ${oppfolgingstilfelle.antallSykedager}
                """.trimIndent()
            )
            val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt(
                personident = oppfolgingstilfelle.personident,
                tilfelleBitReferanseUuid = oppfolgingstilfelle.tilfelleBitReferanseUuid,
                stoppunktAt = LocalDate.now(), // TODO
            )
            kartleggingssporsmalRepository.createStoppunkt(stoppunkt = kartleggingssporsmalStoppunkt)
        } else {
            log.info("Oppfolgingstilfelle with uuid: ${oppfolgingstilfelle.uuid} is not relevant for planlagt kandidat")
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

    // TODO: Move to Kartleggingssporsmal domain class
    private fun calculateStoppunktDato(
        tilfelleStart: LocalDate,
        tilfelleEnd: LocalDate,
    ): LocalDate {
        val today = LocalDate.now()
        val stoppunkt = tilfelleStart.plusDays(KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS)
        return if (stoppunkt.isBefore(today) && today.isBefore(tilfelleEnd)) {
            today
        } else {
            stoppunkt
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KartleggingssporsmalService::class.java)
        private const val PILOTKONTOR_TEST = "0314"
        private val pilotkontorer = listOf(PILOTKONTOR_TEST)

        // TODO: Move to Kartleggingssporsmal domain class
        private const val KARTLEGGINGSSPORSMAL_STOPPUNKT_INTERVAL_DAYS = 30L
        const val KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS = 6L * DAYS_IN_WEEK
        const val KARTLEGGINGSSPORSMAL_STOPPUNKT_END_DAYS =
            KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS + KARTLEGGINGSSPORSMAL_STOPPUNKT_INTERVAL_DAYS
    }
}
