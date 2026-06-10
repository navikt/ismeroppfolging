package no.nav.syfo.kartleggingssporsmal.infrastructure.cronjob

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.kartleggingssporsmal.application.IEsyfovarselProducer
import no.nav.syfo.kartleggingssporsmal.application.IKartleggingssporsmalRepository
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidatStatusendring
import no.nav.syfo.kartleggingssporsmal.domain.Skjemavariant
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.OffsetDateTime

class FerdigstillKartleggingssporsmalVarselCronjobTest {

    private val kartleggingssporsmalRepository = mockk<IKartleggingssporsmalRepository>()
    private val esyfovarselProducer = mockk<IEsyfovarselProducer>()

    private val cronjob = FerdigstillKartleggingssporsmalVarselCronjob(
        kartleggingssporsmalRepository = kartleggingssporsmalRepository,
        esyfovarselProducer = esyfovarselProducer,
    )

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    private fun createVarsletKandidatMedSvarMottatt(): KartleggingssporsmalKandidat {
        val kandidat = KartleggingssporsmalKandidat.create(
            personident = ARBEIDSTAKER_PERSONIDENT,
            skjemavariant = Skjemavariant.FLERVALG_V1,
        ).copy(varsletAt = OffsetDateTime.now())
        return kandidat.registrerSvarMottatt(OffsetDateTime.now())
    }

    @Nested
    @DisplayName("run")
    inner class Run {

        @Test
        fun `ferdigstiller varsel for alle kandidater med svar uten ferdigstilt varsel`() {
            val kandidat = createVarsletKandidatMedSvarMottatt()
            every { kartleggingssporsmalRepository.getKandidaterMedSvarUtenFerdigstiltVarsel() } returns listOf(kandidat)
            every { esyfovarselProducer.ferdigstillKartleggingssporsmalVarsel(kandidat) } returns Result.success(kandidat)
            coEvery { kartleggingssporsmalRepository.updateVarselFerdigstiltAtForKandidat(any()) } returns Unit

            val results = runBlocking { cronjob.run() }

            assertEquals(1, results.size)
            assertTrue(results.first().isSuccess)
            val ferdigstiltKandidat = results.first().getOrThrow()
            assertNotNull(ferdigstiltKandidat.varselFerdigstiltAt)
            assertTrue(ferdigstiltKandidat.status is KartleggingssporsmalKandidatStatusendring.SvarMottatt)

            verify(exactly = 1) { esyfovarselProducer.ferdigstillKartleggingssporsmalVarsel(kandidat) }
            coVerify(exactly = 1) { kartleggingssporsmalRepository.updateVarselFerdigstiltAtForKandidat(any()) }
        }

        @Test
        fun `returnerer tom liste naar ingen kandidater mangler ferdigstilling`() {
            every { kartleggingssporsmalRepository.getKandidaterMedSvarUtenFerdigstiltVarsel() } returns emptyList()

            val results = runBlocking { cronjob.run() }

            assertTrue(results.isEmpty())
            verify(exactly = 0) { esyfovarselProducer.ferdigstillKartleggingssporsmalVarsel(any()) }
        }

        @Test
        fun `returnerer failure naar esyfovarselProducer feiler`() {
            val kandidat = createVarsletKandidatMedSvarMottatt()
            every { kartleggingssporsmalRepository.getKandidaterMedSvarUtenFerdigstiltVarsel() } returns listOf(kandidat)
            every {
                esyfovarselProducer.ferdigstillKartleggingssporsmalVarsel(kandidat)
            } returns Result.failure(RuntimeException("Kafka feil"))

            val results = runBlocking { cronjob.run() }

            assertEquals(1, results.size)
            assertTrue(results.first().isFailure)
            coVerify(exactly = 0) { kartleggingssporsmalRepository.updateVarselFerdigstiltAtForKandidat(any()) }
        }

        @Test
        fun `fortsetter aa behandle andre kandidater selv om en feiler`() {
            val kandidat1 = createVarsletKandidatMedSvarMottatt()
            val kandidat2 = createVarsletKandidatMedSvarMottatt()
            every { kartleggingssporsmalRepository.getKandidaterMedSvarUtenFerdigstiltVarsel() } returns listOf(kandidat1, kandidat2)
            every { esyfovarselProducer.ferdigstillKartleggingssporsmalVarsel(kandidat1) } returns Result.failure(RuntimeException("Kafka feil"))
            every { esyfovarselProducer.ferdigstillKartleggingssporsmalVarsel(kandidat2) } returns Result.success(kandidat2)
            coEvery { kartleggingssporsmalRepository.updateVarselFerdigstiltAtForKandidat(any()) } returns Unit

            val results = runBlocking { cronjob.run() }

            assertEquals(2, results.size)
            assertTrue(results[0].isFailure)
            assertTrue(results[1].isSuccess)
            coVerify(exactly = 1) { kartleggingssporsmalRepository.updateVarselFerdigstiltAtForKandidat(any()) }
        }
    }
}
