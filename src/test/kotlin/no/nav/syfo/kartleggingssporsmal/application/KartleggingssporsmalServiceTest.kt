package no.nav.syfo.kartleggingssporsmal.application

import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_INACTIVE
import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfelleFromKafka
import no.nav.syfo.shared.util.DAYS_IN_WEEK
import org.junit.jupiter.api.Test
import no.nav.syfo.kartleggingssporsmal.infrastructure.database.KartleggingssporsmalRepository
import no.nav.syfo.shared.infrastructure.database.getKartleggingssporsmalStoppunkt
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
    
    @Nested
    @DisplayName("Test processing of oppfolgingstilfelle for generating stoppunkt")
    inner class ProcessOppfolgingstilfelle {
        
        @Test
        fun `processOppfolgingstilfelle should generate stoppunkt when oppfolgingstilfelle is relevant for stoppunkt`() {
            val oppfolgingstilfelleInsideStoppunktInterval = createOppfolgingstilfelleFromKafka(
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
            val oppfolgingstilfelleAtStoppunktStart = createOppfolgingstilfelleFromKafka(
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
            val oppfolgingstilfelleAtStoppunktEnd = createOppfolgingstilfelleFromKafka(
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
            val start = LocalDate.now().minusDays(stoppunktStartIntervalDays + 30)
            val end = LocalDate.now().minusDays(30)
            val oppfolgingstilfelleExactly30DaysAgo = createOppfolgingstilfelleFromKafka(
                tilfelleStart = start,
                tilfelleEnd = end,
                antallSykedager = ChronoUnit.DAYS.between(start, end).toInt() + 1,
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
            val oppfolgingstilfelleWithNullSykedager = createOppfolgingstilfelleFromKafka(
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
        fun `processOppfolgingstilfelle should generate stoppunkt when duration until today is before stoppunkt but new periode sends duration outside interval-end`() {
            val oppfolgingstilfelleWithFutureSykedagerInsideInterval = createOppfolgingstilfelleFromKafka(
                antallSykedager = (stoppunktEndIntervalDays + 1).toInt(),
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays - 2),
            )
            
            runBlocking {
                kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleWithFutureSykedagerInsideInterval)
            }
            
            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(1, stoppunkter.size)
            assertEquals(oppfolgingstilfelleWithFutureSykedagerInsideInterval.personident, stoppunkter.first().personident)
            assertEquals(oppfolgingstilfelleWithFutureSykedagerInsideInterval.tilfelleBitReferanseUuid, stoppunkter.first().tilfelleBitReferanseUuid)
            assertEquals(
                oppfolgingstilfelleWithFutureSykedagerInsideInterval.tilfelleStart.plusDays(stoppunktStartIntervalDays),
                stoppunkter.first().stoppunktAt,
            )
        }
        
        @Test
        fun `processOppfolgingstilfelle should generate stoppunkt today when duration until today is inside interval but new periode sends duration outside interval-end`() {
            val oppfolgingstilfelleWithDurationUntilNowInsideInterval = createOppfolgingstilfelleFromKafka(
                antallSykedager = (stoppunktEndIntervalDays + 10).toInt(),
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays + 10),
            )
            
            runBlocking {
                kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleWithDurationUntilNowInsideInterval)
            }
            
            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(1, stoppunkter.size)
            assertEquals(oppfolgingstilfelleWithDurationUntilNowInsideInterval.personident, stoppunkter.first().personident)
            assertEquals(oppfolgingstilfelleWithDurationUntilNowInsideInterval.tilfelleBitReferanseUuid, stoppunkter.first().tilfelleBitReferanseUuid)
            assertEquals(LocalDate.now(), stoppunkter.first().stoppunktAt)
        }
        
        @Test
        fun `processOppfolgingstilfelle should ignore when too short tilfelle is consumed when 'today' is inside interval`() {
            val oppfolgingstilfelleWithDurationUntilNowInsideInterval = createOppfolgingstilfelleFromKafka(
                antallSykedager = (stoppunktStartIntervalDays - 10).toInt(),
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays + 10),
            )
            
            runBlocking {
                kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleWithDurationUntilNowInsideInterval)
            }
            
            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(0, stoppunkter.size)
        }
        
        @Test
        fun `processOppfolgingstilfelle should ignore when oppfolgingstilfelle is before stoppunkt interval`() {
            val oppfolgingstilfelleBeforeStoppunktInterval = createOppfolgingstilfelleFromKafka(
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
            val oppfolgingstilfelleOutsideStoppunktInterval = createOppfolgingstilfelleFromKafka(
                tilfelleStart = LocalDate.now().minusDays(stoppunktEndIntervalDays + 1),
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
            val oppfolgingstilfelleDod = createOppfolgingstilfelleFromKafka(
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
            val oppfolgingstilfelleNotPilot = createOppfolgingstilfelleFromKafka(
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
            val oppfolgingstilfelleNotPilot = createOppfolgingstilfelleFromKafka(
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
            val oppfolgingstilfelleDodNotPilot = createOppfolgingstilfelleFromKafka(
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
}
