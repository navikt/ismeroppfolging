package no.nav.syfo.kartleggingssporsmal.infrastructure.database

import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidatStatusendring
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfelleFromKafka
import no.nav.syfo.shared.infrastructure.database.getKartleggingssporsmalStoppunkt
import no.nav.syfo.shared.infrastructure.database.markStoppunktAsProcessed
import no.nav.syfo.shared.infrastructure.database.setStoppunktDate
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

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

            val kandidat = KartleggingssporsmalKandidat.create(personident = ARBEIDSTAKER_PERSONIDENT)
            val createdKandidat = kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                kandidat = kandidat,
                stoppunktId = createdStoppunkt.id,
            )

            assertEquals(createdKandidat.personident, oppfolgingstilfelle.personident)
            assertTrue(createdKandidat.status is KartleggingssporsmalKandidatStatusendring.Kandidat)
            assertEquals(createdKandidat.uuid, kandidat.uuid)
            assertNull(createdKandidat.varsletAt)

            val fetchedKandidat = kartleggingssporsmalRepository.getKandidat(ARBEIDSTAKER_PERSONIDENT)
            assertNotNull(fetchedKandidat)
            assertEquals(fetchedKandidat.personident, oppfolgingstilfelle.personident)
            assertTrue(fetchedKandidat.status is KartleggingssporsmalKandidatStatusendring.Kandidat)
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
        val kartleggingssporsmalStoppunkt1 = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
        val kartleggingssporsmalStoppunkt2 = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
        assertNotNull(kartleggingssporsmalStoppunkt1)
        assertNotNull(kartleggingssporsmalStoppunkt2)

        runBlocking {
            kartleggingssporsmalRepository.createStoppunkt(kartleggingssporsmalStoppunkt1)
            kartleggingssporsmalRepository.createStoppunkt(kartleggingssporsmalStoppunkt2)
            val createdStoppunkter = database.getKartleggingssporsmalStoppunkt()

            val kandidat = KartleggingssporsmalKandidat.create(personident = ARBEIDSTAKER_PERSONIDENT)
            val otherKandidat = KartleggingssporsmalKandidat.create(personident = ARBEIDSTAKER_PERSONIDENT)
                .copy(createdAt = OffsetDateTime.now().minusHours(1))
            kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                kandidat = kandidat,
                stoppunktId = createdStoppunkter[0].id,
            )
            kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                kandidat = otherKandidat,
                stoppunktId = createdStoppunkter[1].id,
            )

            val fetchedKandidat = kartleggingssporsmalRepository.getKandidat(ARBEIDSTAKER_PERSONIDENT)
            assertNotNull(fetchedKandidat)
            assertEquals(fetchedKandidat.uuid, kandidat.uuid)
        }
    }

    @Test
    fun `updateSvarForKandidat should update the svarAt field for the kandidat in the database`() {
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            tilfelleStart = LocalDate.now().minusDays(6 * 7),
            antallSykedager = 6 * 7 + 1,
        )
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
        assertNotNull(kartleggingssporsmalStoppunkt)

        runBlocking {
            kartleggingssporsmalRepository.createStoppunkt(kartleggingssporsmalStoppunkt)
            val createdStoppunkt = database.getKartleggingssporsmalStoppunkt().first()

            val kandidat = KartleggingssporsmalKandidat.create(personident = ARBEIDSTAKER_PERSONIDENT)
                .copy(varsletAt = OffsetDateTime.now())
            val createdKandidat = kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                kandidat = kandidat,
                stoppunktId = createdStoppunkt.id,
            )

            val kandidatSvarMottatt = createdKandidat.registrerSvarMottatt(OffsetDateTime.now())
            kartleggingssporsmalRepository.createKandidatStatusendring(kandidatSvarMottatt)

            val hentetKandidat = kartleggingssporsmalRepository.getKandidat(createdKandidat.uuid)
            assertTrue(hentetKandidat?.status is KartleggingssporsmalKandidatStatusendring.SvarMottatt)
        }
    }

    @Test
    fun `updateSvarForKandidat should throw an exception when the kandidat does not exist`() {
        val kandidat = KartleggingssporsmalKandidat.create(personident = ARBEIDSTAKER_PERSONIDENT)
        val kandidatSvarMottatt = kandidat.registrerSvarMottatt(OffsetDateTime.now())
        runBlocking {
            assertThrows<NoSuchElementException> {
                kartleggingssporsmalRepository.createKandidatStatusendring(kandidatSvarMottatt)
            }
        }
    }

    @Test
    fun `getKandidat by uuid should return null when the kandidat does not exist`() {
        runBlocking {
            val fetchedKandidat = kartleggingssporsmalRepository.getKandidat(UUID.randomUUID())
            assertNull(fetchedKandidat)
        }
    }
}
