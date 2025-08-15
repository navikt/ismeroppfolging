package no.nav.syfo.kartleggingssporsmal.application

import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET
import no.nav.syfo.kartleggingssporsmal.application.KartleggingssporsmalService.Companion.KARTLEGGINGSSPORSMAL_STOPPUNKT_END_DAYS
import no.nav.syfo.kartleggingssporsmal.application.KartleggingssporsmalService.Companion.KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS
import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfelle
import no.nav.syfo.kartleggingssporsmal.infrastructure.database.KartleggingssporsmalRepository
import no.nav.syfo.shared.infrastructure.database.getKartleggingssporsmalStoppunkt
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate

class KartleggingssporsmalServiceTest {
    private val behandlendeEnhetClient = ExternalMockEnvironment.instance.behandlendeEnhetClient
    private val database = ExternalMockEnvironment.instance.database
    private val kartleggingssporsmalRepository = KartleggingssporsmalRepository(database)
    private val kartleggingssporsmalService = KartleggingssporsmalService(
        behandlendeEnhetClient = behandlendeEnhetClient,
        kartleggingssporsmalRepository = kartleggingssporsmalRepository,
    )

    @BeforeEach
    fun setUp() {
        database.resetDatabase()
    }

    @Test
    fun `processOppfolgingstilfelle should store in db when oppfolgingstilfelle is relevant for planlagt kandidat`() {
        val oppfolgingstilfelleInsideStoppunktInterval = createOppfolgingstilfelle(
            antallSykedager = KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS.toInt() + 1,
        )

        kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleInsideStoppunktInterval)

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(1, stoppunkter.size)
        assertEquals(oppfolgingstilfelleInsideStoppunktInterval.personident, stoppunkter.first().personident)
        assertEquals(oppfolgingstilfelleInsideStoppunktInterval.tilfelleBitReferanseUuid, stoppunkter.first().tilfelleBitReferanseUuid)
        assertEquals(LocalDate.now(), stoppunkter.first().stoppunktAt)
    }

    @Test
    fun `processOppfolgingstilfelle should store in db when oppfolgingstilfelle is exactly at stoppunkt start`() {
        val oppfolgingstilfelleAtStoppunktStart = createOppfolgingstilfelle(
            antallSykedager = KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS.toInt(),
        )

        kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleAtStoppunktStart)

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(1, stoppunkter.size)
        assertEquals(oppfolgingstilfelleAtStoppunktStart.personident, stoppunkter.first().personident)
        assertEquals(oppfolgingstilfelleAtStoppunktStart.tilfelleBitReferanseUuid, stoppunkter.first().tilfelleBitReferanseUuid)
        assertEquals(LocalDate.now(), stoppunkter.first().stoppunktAt)
    }

    @Test
    fun `processOppfolgingstilfelle should store in db when oppfolgingstilfelle is exactly at stoppunkt end`() {
        val oppfolgingstilfelleAtStoppunktEnd = createOppfolgingstilfelle(
            antallSykedager = KARTLEGGINGSSPORSMAL_STOPPUNKT_END_DAYS.toInt(),
        )

        kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleAtStoppunktEnd)

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(1, stoppunkter.size)
        assertEquals(oppfolgingstilfelleAtStoppunktEnd.personident, stoppunkter.first().personident)
        assertEquals(oppfolgingstilfelleAtStoppunktEnd.tilfelleBitReferanseUuid, stoppunkter.first().tilfelleBitReferanseUuid)
        assertEquals(LocalDate.now(), stoppunkter.first().stoppunktAt)
    }

    @Test
    fun `processOppfolgingstilfelle should store in db when tilfelle ending exactly 30 days ago`() {
        val oppfolgingstilfelleExactly30DaysAgo = createOppfolgingstilfelle(
            tilfelleStart = LocalDate.now().minusDays(KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS + 30),
            tilfelleEnd = LocalDate.now().minusDays(30),
        )

        kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleExactly30DaysAgo)

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(1, stoppunkter.size)
        assertEquals(oppfolgingstilfelleExactly30DaysAgo.personident, stoppunkter.first().personident)
        assertEquals(oppfolgingstilfelleExactly30DaysAgo.tilfelleBitReferanseUuid, stoppunkter.first().tilfelleBitReferanseUuid)
        assertEquals(LocalDate.now(), stoppunkter.first().stoppunktAt)
    }

    @Test
    fun `processOppfolgingstilfelle should store in db when antallSykedager is null but tilfelle interval within stoppunkt interval`() {
        val oppfolgingstilfelleWithNullSykedager = createOppfolgingstilfelle(
            antallSykedager = null,
            tilfelleStart = LocalDate.now(),
            tilfelleEnd = LocalDate.now().plusDays(KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS),
        )

        kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleWithNullSykedager)

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(1, stoppunkter.size)
        assertEquals(oppfolgingstilfelleWithNullSykedager.personident, stoppunkter.first().personident)
        assertEquals(oppfolgingstilfelleWithNullSykedager.tilfelleBitReferanseUuid, stoppunkter.first().tilfelleBitReferanseUuid)
        assertEquals(LocalDate.now(), stoppunkter.first().stoppunktAt)
    }

    @Test
    fun `processOppfolgingstilfelle should ignore when oppfolgingstilfelle is before stoppunkt interval`() {
        val oppfolgingstilfelleBeforeStoppunktInterval = createOppfolgingstilfelle(
            antallSykedager = KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS.toInt() - 1,
        )

        kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleBeforeStoppunktInterval)

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(0, stoppunkter.size)
    }

    @Test
    fun `processOppfolgingstilfelle should ignore when oppfolgingstilfelle is after stoppunkt interval`() {
        val oppfolgingstilfelleOutsideStoppunktInterval = createOppfolgingstilfelle(
            antallSykedager = KARTLEGGINGSSPORSMAL_STOPPUNKT_END_DAYS.toInt() + 1,
        )

        kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleOutsideStoppunktInterval)

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(0, stoppunkter.size)
    }

    @Test
    fun `processOppfolgingstilfelle should ignore when person is dead`() {
        val oppfolgingstilfelleDod = createOppfolgingstilfelle(
            antallSykedager = KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS.toInt(),
            dodsdato = LocalDate.now().minusDays(1),
        )

        kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleDod)

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(0, stoppunkter.size)
    }

    @Test
    fun `processOppfolgingstilfelle should ignore when not in pilot office`() {
        val oppfolgingstilfelleNotPilot = createOppfolgingstilfelle(
            personident = ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET,
            antallSykedager = KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS.toInt(),
        )

        kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleNotPilot)

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(0, stoppunkter.size)
    }

    @Test
    fun `processOppfolgingstilfelle should ignore when multiple negative conditions - dead and not in pilot`() {
        val oppfolgingstilfelleDodNotPilot = createOppfolgingstilfelle(
            personident = ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET,
            antallSykedager = KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS.toInt(),
            dodsdato = LocalDate.now().minusDays(1),
        )

        kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleDodNotPilot)

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(0, stoppunkter.size)
    }
}
