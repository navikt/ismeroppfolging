package no.nav.syfo.kartleggingssporsmal.application

import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_INACTIVE
import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfelle
import no.nav.syfo.shared.util.DAYS_IN_WEEK
import org.junit.jupiter.api.Test
import no.nav.syfo.kartleggingssporsmal.infrastructure.database.KartleggingssporsmalRepository
import no.nav.syfo.shared.infrastructure.database.getKartleggingssporsmalStoppunkt
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

    val stoppunktStartIntervalDays = 6L * DAYS_IN_WEEK
    val stoppunktEndIntervalDays = stoppunktStartIntervalDays + 30L

    @Test
    fun `processOppfolgingstilfelle should generate stoppunkt when oppfolgingstilfelle is relevant for stoppunkt`() {
        val oppfolgingstilfelleInsideStoppunktInterval = createOppfolgingstilfelle(
            antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
        )

        runBlocking {
            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleInsideStoppunktInterval)
        }

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(1, stoppunkter.size)
        assertEquals(oppfolgingstilfelleInsideStoppunktInterval.personident, stoppunkter.first().personident)
        assertEquals(oppfolgingstilfelleInsideStoppunktInterval.tilfelleBitReferanseUuid, stoppunkter.first().tilfelleBitReferanseUuid)
        assertEquals(
            oppfolgingstilfelleInsideStoppunktInterval.tilfelleStart.plusDays(stoppunktStartIntervalDays),
            stoppunkter.first().stoppunktAt,
        )
    }

    @Test
    fun `processOppfolgingstilfelle should generate stoppunkt when oppfolgingstilfelle is exactly at stoppunkt start`() {
        val oppfolgingstilfelleAtStoppunktStart = createOppfolgingstilfelle(
            antallSykedager = stoppunktStartIntervalDays.toInt(),
        )

        runBlocking {
            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleAtStoppunktStart)
        }

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(1, stoppunkter.size)
        assertEquals(oppfolgingstilfelleAtStoppunktStart.personident, stoppunkter.first().personident)
        assertEquals(oppfolgingstilfelleAtStoppunktStart.tilfelleBitReferanseUuid, stoppunkter.first().tilfelleBitReferanseUuid)
        assertEquals(
            oppfolgingstilfelleAtStoppunktStart.tilfelleStart.plusDays(stoppunktStartIntervalDays),
            stoppunkter.first().stoppunktAt,
        )
    }

    @Test
    fun `processOppfolgingstilfelle should generate stoppunkt when oppfolgingstilfelle is exactly at stoppunkt end`() {
        val oppfolgingstilfelleAtStoppunktEnd = createOppfolgingstilfelle(
            antallSykedager = stoppunktEndIntervalDays.toInt(),
        )

        runBlocking {
            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleAtStoppunktEnd)
        }

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(1, stoppunkter.size)
        assertEquals(oppfolgingstilfelleAtStoppunktEnd.personident, stoppunkter.first().personident)
        assertEquals(oppfolgingstilfelleAtStoppunktEnd.tilfelleBitReferanseUuid, stoppunkter.first().tilfelleBitReferanseUuid)
        assertEquals(
            oppfolgingstilfelleAtStoppunktEnd.tilfelleStart.plusDays(stoppunktStartIntervalDays),
            stoppunkter.first().stoppunktAt,
        )
    }

    @Test
    fun `processOppfolgingstilfelle should generate stoppunkt when tilfelle ending exactly 30 days ago`() {
        val oppfolgingstilfelleExactly30DaysAgo = createOppfolgingstilfelle(
            tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays + 30),
            tilfelleEnd = LocalDate.now().minusDays(30),
        )

        runBlocking {
            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleExactly30DaysAgo)
        }

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(1, stoppunkter.size)
        assertEquals(oppfolgingstilfelleExactly30DaysAgo.personident, stoppunkter.first().personident)
        assertEquals(oppfolgingstilfelleExactly30DaysAgo.tilfelleBitReferanseUuid, stoppunkter.first().tilfelleBitReferanseUuid)
        assertEquals(
            oppfolgingstilfelleExactly30DaysAgo.tilfelleStart.plusDays(stoppunktStartIntervalDays),
            stoppunkter.first().stoppunktAt,
        )
    }

    @Test
    fun `processOppfolgingstilfelle should generate stoppunkt when antallSykedager is null but tilfelle interval within stoppunkt interval`() {
        val oppfolgingstilfelleWithNullSykedager = createOppfolgingstilfelle(
            antallSykedager = null,
            tilfelleStart = LocalDate.now(),
            tilfelleEnd = LocalDate.now().plusDays(stoppunktStartIntervalDays),
        )

        runBlocking {
            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleWithNullSykedager)
        }

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(1, stoppunkter.size)
        assertEquals(oppfolgingstilfelleWithNullSykedager.personident, stoppunkter.first().personident)
        assertEquals(oppfolgingstilfelleWithNullSykedager.tilfelleBitReferanseUuid, stoppunkter.first().tilfelleBitReferanseUuid)
        assertEquals(
            oppfolgingstilfelleWithNullSykedager.tilfelleStart.plusDays(stoppunktStartIntervalDays),
            stoppunkter.first().stoppunktAt,
        )
    }

    @Test
    fun `processOppfolgingstilfelle should ignore when oppfolgingstilfelle is before stoppunkt interval`() {
        val oppfolgingstilfelleBeforeStoppunktInterval = createOppfolgingstilfelle(
            antallSykedager = stoppunktStartIntervalDays.toInt() - 1,
        )

        runBlocking {
            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleBeforeStoppunktInterval)
        }

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(0, stoppunkter.size)
    }

    @Test
    fun `processOppfolgingstilfelle should ignore when oppfolgingstilfelle is after stoppunkt interval`() {
        val oppfolgingstilfelleOutsideStoppunktInterval = createOppfolgingstilfelle(
            antallSykedager = stoppunktEndIntervalDays.toInt() + 1,
        )

        runBlocking {
            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleOutsideStoppunktInterval)
        }

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(0, stoppunkter.size)
    }

    @Test
    fun `processOppfolgingstilfelle should ignore when person is dead`() {
        val oppfolgingstilfelleDod = createOppfolgingstilfelle(
            antallSykedager = stoppunktStartIntervalDays.toInt(),
            dodsdato = LocalDate.now().minusDays(1),
        )

        runBlocking {
            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleDod)
        }

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(0, stoppunkter.size)
    }

    @Test
    fun `processOppfolgingstilfelle should ignore when not in pilot enhet`() {
        val oppfolgingstilfelleNotPilot = createOppfolgingstilfelle(
            personident = ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET,
            antallSykedager = stoppunktStartIntervalDays.toInt(),
        )

        runBlocking {
            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleNotPilot)
        }

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(0, stoppunkter.size)
    }

    @Test
    fun `processOppfolgingstilfelle should return false when cannot find behandlende enhet`() {
        val oppfolgingstilfelleNotPilot = createOppfolgingstilfelle(
            personident = ARBEIDSTAKER_PERSONIDENT_INACTIVE,
            antallSykedager = stoppunktStartIntervalDays.toInt(),
        )

        runBlocking {
            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleNotPilot)
        }

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(0, stoppunkter.size)
    }

    @Test
    fun `processOppfolgingstilfelle should ignore when multiple negative conditions - dead and not in pilot`() {
        val oppfolgingstilfelleDodNotPilot = createOppfolgingstilfelle(
            personident = ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET,
            antallSykedager = stoppunktStartIntervalDays.toInt(),
            dodsdato = LocalDate.now().minusDays(1),
        )

        runBlocking {
            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleDodNotPilot)
        }

        val stoppunkter = database.getKartleggingssporsmalStoppunkt()
        assertEquals(0, stoppunkter.size)
    }
}
