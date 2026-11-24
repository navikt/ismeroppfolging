package no.nav.syfo.kartleggingssporsmal.application

import io.mockk.*
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidatStatusendring.Ferdigbehandlet.VurderingAlternativ
import no.nav.syfo.kartleggingssporsmal.domain.Skjemavariant
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.KartleggingssporsmalKandidatStatusV2Record
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.OffsetDateTime

class KartleggingssporsmalKandidatV2PublishServiceTest {

    private val repository = mockk<IKartleggingssporsmalRepository>(relaxed = true)
    private val producer = mockk<IKartleggingssporsmalKandidatV2Producer>()

    private fun service(enabled: Boolean = true) = KartleggingssporsmalKandidatV2PublishService(
        kartleggingssporsmalRepository = repository,
        kandidatV2Producer = producer,
        isKandidatV2PublishingEnabled = enabled,
    )

    private fun kandidat() = KartleggingssporsmalKandidat.create(
        personident = ARBEIDSTAKER_PERSONIDENT,
        skjemavariant = Skjemavariant.FLERVALG_FRITEKST_V4,
    )

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Nested
    @DisplayName("publishStatusendringer")
    inner class PublishStatusendringer {

        @Test
        fun `publiserer og markerer alle statusendringer som publisert`() {
            val kandidater = listOf(kandidat(), kandidat())
            every { repository.getKandidaterWithStatusendringNotPublishedV2() } returns kandidater
            every { producer.send(any()) } answers { Result.success(firstArg()) }

            val results = service().publishStatusendringer()

            assertEquals(2, results.size)
            assertTrue(results.all { it.isSuccess })
            kandidater.forEach {
                verify(exactly = 1) { repository.updatePublishedV2AtForKandidatStatusendring(it) }
            }
        }

        @Test
        fun `stopper ved feil for a bevare rekkefolgen`() {
            val kandidater = listOf(kandidat(), kandidat(), kandidat())
            every { repository.getKandidaterWithStatusendringNotPublishedV2() } returns kandidater
            every { producer.send(kandidater[0]) } returns Result.success(kandidater[0])
            every { producer.send(kandidater[1]) } returns Result.failure(RuntimeException("Kafka down"))

            val results = service().publishStatusendringer()

            assertEquals(2, results.size)
            assertTrue(results[1].isFailure)
            verify(exactly = 1) { repository.updatePublishedV2AtForKandidatStatusendring(kandidater[0]) }
            verify(exactly = 0) { repository.updatePublishedV2AtForKandidatStatusendring(kandidater[1]) }
            verify(exactly = 0) { producer.send(kandidater[2]) }
        }

        @Test
        fun `publiserer ikke nar toggle er av`() {
            val results = service(enabled = false).publishStatusendringer()

            assertTrue(results.isEmpty())
            verify(exactly = 0) { repository.getKandidaterWithStatusendringNotPublishedV2() }
        }
    }

    @Nested
    @DisplayName("KartleggingssporsmalKandidatStatusV2Record")
    inner class Record {

        @Test
        fun `inneholder vurdering for ferdigbehandlet`() {
            val ferdigbehandlet = kandidat()
                .registrerSvarMottatt(OffsetDateTime.now())
                .ferdigbehandleVurdering(
                    veilederident = "Z999999",
                    vurderingAlternativ = VurderingAlternativ.RISIKO_FOR_LANGTIDSFRAVAR,
                    vurderingFritekst = "Fritekst",
                )

            val record = KartleggingssporsmalKandidatStatusV2Record.from(ferdigbehandlet)

            assertEquals(ferdigbehandlet.status.uuid, record.statusUuid)
            assertEquals("FERDIGBEHANDLET", record.status)
            assertEquals("RISIKO_FOR_LANGTIDSFRAVAR", record.vurdering?.alternativ)
            assertEquals("Fritekst", record.vurdering?.fritekst)
            assertEquals("Z999999", record.vurdering?.veilederident)
        }

        @Test
        fun `har ingen vurdering for andre statuser`() {
            val record = KartleggingssporsmalKandidatStatusV2Record.from(kandidat())

            assertEquals("KANDIDAT", record.status)
            assertNull(record.vurdering)
        }
    }
}
