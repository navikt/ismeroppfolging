package no.nav.syfo.kartleggingssporsmal.infrastructure.database

import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.kartleggingssporsmal.domain.KandidatStatus
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfelleFromKafka
import no.nav.syfo.shared.infrastructure.database.getKartleggingssporsmalStoppunkt
import no.nav.syfo.shared.infrastructure.database.markStoppunktAsProcessed
import no.nav.syfo.shared.infrastructure.database.setStoppunktDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
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
            val createdStoppunkt = kartleggingssporsmalRepository.createStoppunkt(kartleggingssporsmalStoppunkt)
            database.markStoppunktAsProcessed(createdStoppunkt)
            val unprocessed = kartleggingssporsmalRepository.getUnprocessedStoppunkter()
            assertEquals(unprocessed.size, 0)
        }
    }

    @Test
    fun `createKandidatAndMarkStoppunktAsProcessed should create a kandidat in the database and mark stoppunkt as processed`() {
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            tilfelleStart = LocalDate.now().minusDays(6 * 7),
            antallSykedager = 6 * 7 + 1,
        )
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
        assertNotNull(kartleggingssporsmalStoppunkt)

        runBlocking {
            kartleggingssporsmalRepository.createStoppunkt(kartleggingssporsmalStoppunkt)
            val createdStoppunkt = database.getKartleggingssporsmalStoppunkt().first()

            val kandidat = KartleggingssporsmalKandidat(
                personident = ARBEIDSTAKER_PERSONIDENT,
                status = KandidatStatus.KANDIDAT,
            )
            val createdKandidat = kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                kandidat = kandidat,
                stoppunktId = createdStoppunkt.id,
            )

            assertEquals(createdKandidat.personident, oppfolgingstilfelle.personident)
            assertEquals(createdKandidat.status, KandidatStatus.KANDIDAT)
            assertEquals(createdKandidat.uuid, kandidat.uuid)
            assertNull(createdKandidat.varsletAt)

            val fetchedKandidat = kartleggingssporsmalRepository.getKandidat(ARBEIDSTAKER_PERSONIDENT)
            assertNotNull(fetchedKandidat)
            assertEquals(fetchedKandidat.personident, oppfolgingstilfelle.personident)
            assertEquals(fetchedKandidat.status, KandidatStatus.KANDIDAT)
            assertNull(fetchedKandidat.varsletAt)

            val processedStoppunkt = database.getKartleggingssporsmalStoppunkt().first()
            assertNotNull(processedStoppunkt.processedAt)
        }
    }

    @Test
    fun `getKandidat should retrieve the newest kandidate when several exists`() {
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            tilfelleStart = LocalDate.now().minusDays(6 * 7),
            antallSykedager = 6 * 7 + 1,
        )
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
        assertNotNull(kartleggingssporsmalStoppunkt)

        runBlocking {
            kartleggingssporsmalRepository.createStoppunkt(kartleggingssporsmalStoppunkt)
            val createdStoppunkt = database.getKartleggingssporsmalStoppunkt().first()

            val kandidat = KartleggingssporsmalKandidat(
                personident = ARBEIDSTAKER_PERSONIDENT,
                status = KandidatStatus.KANDIDAT,
            )
            val otherKandidat = kandidat.copy(
                uuid = UUID.randomUUID(),
                createdAt = OffsetDateTime.now().minusHours(1),
            )
            kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                kandidat = kandidat,
                stoppunktId = createdStoppunkt.id,
            )
            kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                kandidat = otherKandidat,
                stoppunktId = createdStoppunkt.id,
            )

            val fetchedKandidat = kartleggingssporsmalRepository.getKandidat(ARBEIDSTAKER_PERSONIDENT)
            assertNotNull(fetchedKandidat)
            assertEquals(fetchedKandidat.uuid, kandidat.uuid)
        }
    }
}
