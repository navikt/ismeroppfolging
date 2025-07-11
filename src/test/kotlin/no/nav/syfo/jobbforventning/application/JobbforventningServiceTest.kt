package no.nav.syfo.jobbforventning.application

import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET
import no.nav.syfo.jobbforventning.application.JobbforventningService.Companion.JOBBFORVENTNING_STOPPUNKT_END_DAYS
import no.nav.syfo.jobbforventning.application.JobbforventningService.Companion.JOBBFORVENTNING_STOPPUNKT_START_DAYS
import no.nav.syfo.jobbforventning.generators.createOppfolgingstilfelle
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

class JobbforventningServiceTest {
    private val behandlendeEnhetClient = ExternalMockEnvironment.instance.behandlendeEnhetClient
    private val jobbforventningService = JobbforventningService(
        behandlendeEnhetClient = behandlendeEnhetClient,
    )

    @Test
    fun `processOppfolgingstilfelle should return true when oppfolgingstilfelle is relevant for planlagt kandidat`() {
        val oppfolgingstilfelleInsideStoppunktInterval = createOppfolgingstilfelle(
            antallSykedager = JOBBFORVENTNING_STOPPUNKT_START_DAYS.toInt() + 1,
        )

        val result = jobbforventningService.processOppfolgingstilfelle(oppfolgingstilfelleInsideStoppunktInterval)

        assertTrue(result)
    }

    @Test
    fun `processOppfolgingstilfelle should return true when oppfolgingstilfelle is exactly at stoppunkt start`() {
        val oppfolgingstilfelleAtStoppunktStart = createOppfolgingstilfelle(
            antallSykedager = JOBBFORVENTNING_STOPPUNKT_START_DAYS.toInt(),
        )

        val result = jobbforventningService.processOppfolgingstilfelle(oppfolgingstilfelleAtStoppunktStart)

        assertTrue(result)
    }

    @Test
    fun `processOppfolgingstilfelle should return true when oppfolgingstilfelle is exactly at stoppunkt end`() {
        val oppfolgingstilfelleAtStoppunktEnd = createOppfolgingstilfelle(
            antallSykedager = JOBBFORVENTNING_STOPPUNKT_END_DAYS.toInt(),
        )

        val result = jobbforventningService.processOppfolgingstilfelle(oppfolgingstilfelleAtStoppunktEnd)

        assertTrue(result)
    }

    @Test
    fun `processOppfolgingstilfelle should handle tilfelle ending exactly 30 days ago`() {
        val oppfolgingstilfelleExactly30DaysAgo = createOppfolgingstilfelle(
            tilfelleStart = LocalDate.now().minusDays(JOBBFORVENTNING_STOPPUNKT_START_DAYS + 30),
            tilfelleEnd = LocalDate.now().minusDays(30),
        )

        val result = jobbforventningService.processOppfolgingstilfelle(oppfolgingstilfelleExactly30DaysAgo)

        assertTrue(result)
    }

    @Test
    fun `processOppfolgingstilfelle should return true when antallSykedager is null but tilfelle interval within stoppunkt interval`() {
        val oppfolgingstilfelleWithNullSykedager = createOppfolgingstilfelle(
            antallSykedager = null,
            tilfelleStart = LocalDate.now(),
            tilfelleEnd = LocalDate.now().plusDays(JOBBFORVENTNING_STOPPUNKT_START_DAYS),
        )

        val result = jobbforventningService.processOppfolgingstilfelle(oppfolgingstilfelleWithNullSykedager)

        assertTrue(result)
    }

    @Test
    fun `processOppfolgingstilfelle should return false when oppfolgingstilfelle is before stoppunkt interval`() {
        val oppfolgingstilfelleBeforeStoppunktInterval = createOppfolgingstilfelle(
            antallSykedager = JOBBFORVENTNING_STOPPUNKT_START_DAYS.toInt() - 1,
        )

        val result = jobbforventningService.processOppfolgingstilfelle(oppfolgingstilfelleBeforeStoppunktInterval)

        assertFalse(result)
    }

    @Test
    fun `processOppfolgingstilfelle should return false when oppfolgingstilfelle is after stoppunkt interval`() {
        val oppfolgingstilfelleOutsideStoppunktInterval = createOppfolgingstilfelle(
            antallSykedager = JOBBFORVENTNING_STOPPUNKT_END_DAYS.toInt() + 1,
        )

        val result = jobbforventningService.processOppfolgingstilfelle(oppfolgingstilfelleOutsideStoppunktInterval)

        assertFalse(result)
    }

    @Test
    fun `processOppfolgingstilfelle should return false when person is dead`() {
        val oppfolgingstilfelleDod = createOppfolgingstilfelle(
            antallSykedager = JOBBFORVENTNING_STOPPUNKT_START_DAYS.toInt(),
            dodsdato = LocalDate.now().minusDays(1),
        )

        val result = jobbforventningService.processOppfolgingstilfelle(oppfolgingstilfelleDod)

        assertFalse(result)
    }

    @Test
    fun `processOppfolgingstilfelle should return false when not in pilot office`() {
        val oppfolgingstilfelleNotPilot = createOppfolgingstilfelle(
            personident = ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET,
            antallSykedager = JOBBFORVENTNING_STOPPUNKT_START_DAYS.toInt(),
        )

        val result = jobbforventningService.processOppfolgingstilfelle(oppfolgingstilfelleNotPilot)

        assertFalse(result)
    }

    @Test
    fun `processOppfolgingstilfelle should handle multiple negative conditions - dead and not in pilot`() {
        val oppfolgingstilfelleDodNotPilot = createOppfolgingstilfelle(
            personident = ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET,
            antallSykedager = JOBBFORVENTNING_STOPPUNKT_START_DAYS.toInt(),
            dodsdato = LocalDate.now().minusDays(1),
        )

        val result = jobbforventningService.processOppfolgingstilfelle(oppfolgingstilfelleDodNotPilot)

        assertFalse(result)
    }
}
