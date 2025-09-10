package no.nav.syfo.kartleggingssporsmal.infrastructure.database

import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfelleFromKafka
import no.nav.syfo.shared.infrastructure.database.setStoppunktDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.time.LocalDate
import kotlin.test.Test

class KartleggingssporsmalRepositoryTest {

    val database = ExternalMockEnvironment.instance.database
    val kartleggingssporsmalRepository = KartleggingssporsmalRepository(database)

    @BeforeEach
    fun before() {
        database.resetDatabase()
    }

    @Test
    fun `createStoppunkt should create a stoppunkt in the database`() {
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            tilfelleStart = LocalDate.now(),
            antallSykedager = 6 * 7,
        )
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
        assertNotNull(kartleggingssporsmalStoppunkt)

        runBlocking {
            val createdStoppunkt = kartleggingssporsmalRepository.createStoppunkt(kartleggingssporsmalStoppunkt)

            assertEquals(kartleggingssporsmalStoppunkt.personident, createdStoppunkt.personident)
            assertEquals(kartleggingssporsmalStoppunkt.tilfelleBitReferanseUuid, createdStoppunkt.tilfelleBitReferanseUuid)
            assertEquals(kartleggingssporsmalStoppunkt.stoppunktAt, createdStoppunkt.stoppunktAt)
            assertNull(createdStoppunkt.processedAt)
        }
    }

    @Test
    fun `finds existing unprocessed stoppunkt from today`() {
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            tilfelleStart = LocalDate.now().minusDays(6 * 7),
            antallSykedager = 6 * 7 + 1,
        )
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
        assertNotNull(kartleggingssporsmalStoppunkt)

        runBlocking {
            kartleggingssporsmalRepository.createStoppunkt(kartleggingssporsmalStoppunkt)
            val unprocessed = kartleggingssporsmalRepository.getUnprocessedStoppunkter()
            assertEquals(unprocessed.size, 1)
            assertEquals(unprocessed[0].second.personident, kartleggingssporsmalStoppunkt.personident)
        }
    }

    @Test
    fun `finds existing unprocessed stoppunkt from yesterday`() {
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            tilfelleStart = LocalDate.now().minusDays(6 * 7 + 1),
            antallSykedager = 6 * 7 + 2,
        )
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
        assertNotNull(kartleggingssporsmalStoppunkt)

        runBlocking {
            kartleggingssporsmalRepository.createStoppunkt(kartleggingssporsmalStoppunkt)
            database.setStoppunktDate(kartleggingssporsmalStoppunkt.uuid, LocalDate.now().minusDays(1))

            val unprocessed = kartleggingssporsmalRepository.getUnprocessedStoppunkter()
            assertEquals(unprocessed.size, 1)
            assertEquals(unprocessed[0].second.personident, kartleggingssporsmalStoppunkt.personident)
        }
    }

    @Test
    fun `does not find existing unprocessed stoppunkt from two days ago`() {
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            tilfelleStart = LocalDate.now().minusDays(6 * 7 + 2),
            antallSykedager = 6 * 7 + 3,
        )
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
        assertNotNull(kartleggingssporsmalStoppunkt)

        runBlocking {
            kartleggingssporsmalRepository.createStoppunkt(kartleggingssporsmalStoppunkt)
            database.setStoppunktDate(kartleggingssporsmalStoppunkt.uuid, LocalDate.now().minusDays(2))

            val unprocessed = kartleggingssporsmalRepository.getUnprocessedStoppunkter()
            assertEquals(unprocessed.size, 0)
        }
    }

    @Test
    fun `does not find existing unprocessed stoppunkt for tomorrow`() {
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            tilfelleStart = LocalDate.now().minusDays(6 * 7 - 1),
            antallSykedager = 6 * 7,
        )
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
        assertNotNull(kartleggingssporsmalStoppunkt)

        runBlocking {
            kartleggingssporsmalRepository.createStoppunkt(kartleggingssporsmalStoppunkt)
            val unprocessed = kartleggingssporsmalRepository.getUnprocessedStoppunkter()
            assertEquals(unprocessed.size, 0)
        }
    }

    @Test
    fun `does not find existing processed stoppunkt from today`() {
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            tilfelleStart = LocalDate.now().minusDays(6 * 7),
            antallSykedager = 6 * 7 + 1,
        )
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
        assertNotNull(kartleggingssporsmalStoppunkt)

        runBlocking {
            val created = kartleggingssporsmalRepository.createStoppunkt(kartleggingssporsmalStoppunkt)
            kartleggingssporsmalRepository.markStoppunktAsProcessed(created)
            val unprocessed = kartleggingssporsmalRepository.getUnprocessedStoppunkter()
            assertEquals(unprocessed.size, 0)
        }
    }
}
