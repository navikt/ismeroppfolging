package no.nav.syfo.kartleggingssporsmal.infrastructure.cronjob

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET
import no.nav.syfo.kartleggingssporsmal.application.KartleggingssporsmalService
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.kartleggingssporsmal.domain.Skjemavariant
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class KandidatStoppunktCronjobTest {

    private val kartleggingssporsmalService = mockk<KartleggingssporsmalService>()

    private val cronjob = KandidatStoppunktCronjob(
        kartleggingssporsmalService = kartleggingssporsmalService,
    )

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Nested
    @DisplayName("run")
    inner class Run {

        @Test
        fun `deduplicates recovery results that overlap with stoppunkt results by personident`() {
            val stoppunkt = KartleggingssporsmalStoppunkt.createFromDatabase(
                uuid = UUID.randomUUID(),
                createdAt = OffsetDateTime.now(),
                personident = ARBEIDSTAKER_PERSONIDENT,
                tilfelleBitReferanseUuid = UUID.randomUUID(),
                stoppunktAt = LocalDate.now(),
                processedAt = OffsetDateTime.now(),
            )
            val kandidat = KartleggingssporsmalKandidat.create(
                personident = ARBEIDSTAKER_PERSONIDENT,
                skjemavariant = Skjemavariant.FLERVALG_V1,
            )

            coEvery { kartleggingssporsmalService.processStoppunkter() } returns listOf(Result.success(stoppunkt))
            coEvery { kartleggingssporsmalService.processKandidaterWithMissingPublishOrVarsel() } returns listOf(Result.success(kandidat))

            val results = runBlocking { cronjob.run() }

            assertEquals(1, results.size)
            assertTrue(results.all { it.isSuccess })
        }

        @Test
        fun `does not deduplicate recovery results with different personident`() {
            val stoppunkt = KartleggingssporsmalStoppunkt.createFromDatabase(
                uuid = UUID.randomUUID(),
                createdAt = OffsetDateTime.now(),
                personident = ARBEIDSTAKER_PERSONIDENT,
                tilfelleBitReferanseUuid = UUID.randomUUID(),
                stoppunktAt = LocalDate.now(),
                processedAt = OffsetDateTime.now(),
            )
            val kandidat = KartleggingssporsmalKandidat.create(
                personident = ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET,
                skjemavariant = Skjemavariant.FLERVALG_V1,
            )

            coEvery { kartleggingssporsmalService.processStoppunkter() } returns listOf(Result.success(stoppunkt))
            coEvery { kartleggingssporsmalService.processKandidaterWithMissingPublishOrVarsel() } returns listOf(Result.success(kandidat))

            val results = runBlocking { cronjob.run() }

            assertEquals(2, results.size)
            assertTrue(results.all { it.isSuccess })
        }

        @Test
        fun `does not deduplicate failed recovery results even with same personident`() {
            val stoppunkt = KartleggingssporsmalStoppunkt.createFromDatabase(
                uuid = UUID.randomUUID(),
                createdAt = OffsetDateTime.now(),
                personident = ARBEIDSTAKER_PERSONIDENT,
                tilfelleBitReferanseUuid = UUID.randomUUID(),
                stoppunktAt = LocalDate.now(),
                processedAt = OffsetDateTime.now(),
            )

            coEvery { kartleggingssporsmalService.processStoppunkter() } returns listOf(Result.success(stoppunkt))
            coEvery { kartleggingssporsmalService.processKandidaterWithMissingPublishOrVarsel() } returns listOf(
                Result.failure(RuntimeException("Kafka publish failed"))
            )

            val results = runBlocking { cronjob.run() }

            assertEquals(2, results.size)
            assertEquals(1, results.count { it.isSuccess })
            assertEquals(1, results.count { it.isFailure })
        }

        @Test
        fun `does not deduplicate recovery results when stoppunkt result failed`() {
            val kandidat = KartleggingssporsmalKandidat.create(
                personident = ARBEIDSTAKER_PERSONIDENT,
                skjemavariant = Skjemavariant.FLERVALG_V1,
            )

            coEvery { kartleggingssporsmalService.processStoppunkter() } returns listOf(
                Result.failure(RuntimeException("Processing failed"))
            )
            coEvery { kartleggingssporsmalService.processKandidaterWithMissingPublishOrVarsel() } returns listOf(Result.success(kandidat))

            val results = runBlocking { cronjob.run() }

            assertEquals(2, results.size)
            assertEquals(1, results.count { it.isSuccess })
            assertEquals(1, results.count { it.isFailure })
        }
    }
}
