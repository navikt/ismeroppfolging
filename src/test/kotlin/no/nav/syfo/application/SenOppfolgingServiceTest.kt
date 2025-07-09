package no.nav.syfo.application

import io.mockk.*
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.domain.OnskerOppfolging
import no.nav.syfo.domain.SenOppfolgingStatus
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.infrastructure.database.getSenOppfolgingKandidater
import no.nav.syfo.infrastructure.database.getSenOppfolgingVurderinger
import no.nav.syfo.infrastructure.database.repository.SenOppfolgingRepository
import no.nav.syfo.infrastructure.kafka.KandidatStatusProducer
import no.nav.syfo.infrastructure.kafka.KandidatStatusRecord
import no.nav.syfo.util.millisekundOpplosning
import no.nav.syfo.util.nowUTC
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.Future

class SenOppfolgingServiceTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database

    private val senOppfolgingRepository = SenOppfolgingRepository(database = database)

    private val mockKandidatStatusProducer = mockk<KafkaProducer<String, KandidatStatusRecord>>(relaxed = true)
    private val kandidatStatusProducer = KandidatStatusProducer(
        producer = mockKandidatStatusProducer,
    )

    private val senOppfolgingService = SenOppfolgingService(
        senOppfolgingRepository = senOppfolgingRepository,
        kandidatStatusProducer = kandidatStatusProducer,
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        coEvery { mockKandidatStatusProducer.send(any()) } returns mockk<Future<RecordMetadata>>(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        database.resetDatabase()
    }

    @Nested
    @DisplayName("Publish unpublished kandidat status")
    inner class PublishUnpublishedKandidatStatus {

        @Test
        fun `publishes unpublished kandidat with svar`() {
            val kandidat = senOppfolgingService.createKandidat(
                personident = ARBEIDSTAKER_PERSONIDENT,
            )
            senOppfolgingService.addSvar(
                kandidat = kandidat,
                svarAt = OffsetDateTime.now(),
                onskerOppfolging = OnskerOppfolging.JA,
            )
            val (success, failed) = senOppfolgingService.publishUnpublishedKandidatStatus()
                .partition { it.isSuccess }

            assertEquals(0, failed.size)
            assertEquals(1, success.size)

            val producerRecordSlot = slot<ProducerRecord<String, KandidatStatusRecord>>()
            verify(exactly = 1) { mockKandidatStatusProducer.send(capture(producerRecordSlot)) }

            val kandidatStatusRecord = producerRecordSlot.captured.value()
            assertEquals(kandidat.uuid, kandidatStatusRecord.uuid)
            assertEquals(kandidat.personident.value, kandidatStatusRecord.personident)
            assertEquals(kandidat.createdAt, kandidatStatusRecord.createdAt)
            assertEquals(kandidat.status, kandidatStatusRecord.status.value)
            assertEquals(kandidat.status.isActive, kandidatStatusRecord.status.isActive)
            assertNull(kandidatStatusRecord.sisteVurdering)

            val pKandidat = database.getSenOppfolgingKandidater().first()
            assertNotNull(pKandidat.publishedAt)
        }

        @Test
        fun `publishes unpublished kandidat once`() {
            val kandidat = senOppfolgingService.createKandidat(
                personident = ARBEIDSTAKER_PERSONIDENT,
            )
            senOppfolgingService.addSvar(
                kandidat = kandidat,
                svarAt = OffsetDateTime.now(),
                onskerOppfolging = OnskerOppfolging.JA,
            )
            var results = senOppfolgingService.publishUnpublishedKandidatStatus().partition { it.isSuccess }
            assertEquals(1, results.first.size)
            assertEquals(0, results.second.size)

            results = senOppfolgingService.publishUnpublishedKandidatStatus().partition { it.isSuccess }
            assertEquals(0, results.first.size)
            assertEquals(0, results.second.size)

            verify(exactly = 1) { mockKandidatStatusProducer.send(any()) }
        }

        @Test
        fun `does not publish unpublished kandidat with no svar and less than 10 days passed since varsel`() {
            senOppfolgingService.createKandidat(
                personident = ARBEIDSTAKER_PERSONIDENT,
                varselAt = nowUTC(),
            )
            val (success, failed) = senOppfolgingService.publishUnpublishedKandidatStatus()
                .partition { it.isSuccess }
            assertEquals(0, failed.size)
            assertEquals(0, success.size)

            val pKandidat = database.getSenOppfolgingKandidater().first()
            assertNull(pKandidat.publishedAt)
        }

        @Test
        fun `publishes unpublished kandidat with no svar and 10 days since varsel`() {
            val kandidatIkkeSvart = senOppfolgingService.createKandidat(
                personident = ARBEIDSTAKER_PERSONIDENT,
                varselAt = OffsetDateTime.now().minusDays(10),
            )
            val (success, failed) = senOppfolgingService.publishUnpublishedKandidatStatus()
                .partition { it.isSuccess }
            assertEquals(0, failed.size)
            assertEquals(1, success.size)

            val producerRecordSlot = slot<ProducerRecord<String, KandidatStatusRecord>>()
            verify(exactly = 1) { mockKandidatStatusProducer.send(capture(producerRecordSlot)) }

            val kandidatStatusRecord = producerRecordSlot.captured.value()
            assertEquals(kandidatIkkeSvart.uuid, kandidatStatusRecord.uuid)
            assertEquals(kandidatIkkeSvart.personident.value, kandidatStatusRecord.personident)
            assertEquals(kandidatIkkeSvart.createdAt, kandidatStatusRecord.createdAt)
            assertEquals(kandidatIkkeSvart.status, kandidatStatusRecord.status.value)
            assertEquals(kandidatIkkeSvart.status.isActive, kandidatStatusRecord.status.isActive)
            assertNull(kandidatStatusRecord.sisteVurdering)

            val pKandidat = database.getSenOppfolgingKandidater().first()
            assertNotNull(pKandidat.publishedAt)
        }

        @Test
        fun `publishes unpublished vurdering`() {
            val kandidat = senOppfolgingService.createKandidat(
                personident = ARBEIDSTAKER_PERSONIDENT,
                varselAt = nowUTC(),
                varselId = UUID.randomUUID(),
            )
            senOppfolgingService.addSvar(
                kandidat = kandidat,
                svarAt = OffsetDateTime.now(),
                onskerOppfolging = OnskerOppfolging.JA,
            )
            senOppfolgingRepository.setKandidatPublished(kandidatUuid = kandidat.uuid)
            val vurdertKandidat = senOppfolgingService.vurderKandidat(
                kandidat = kandidat,
                veilederident = UserConstants.VEILEDER_IDENT,
                begrunnelse = "Begrunnelse",
                type = VurderingType.FERDIGBEHANDLET,
            )

            val (success, failed) = senOppfolgingService.publishUnpublishedKandidatStatus()
                .partition { it.isSuccess }
            assertEquals(0, failed.size)
            assertEquals(1, success.size)

            val producerRecordSlot = slot<ProducerRecord<String, KandidatStatusRecord>>()
            verify(exactly = 1) { mockKandidatStatusProducer.send(capture(producerRecordSlot)) }

            val kandidatStatusRecord = producerRecordSlot.captured.value()
            assertEquals(kandidat.uuid, kandidatStatusRecord.uuid)
            assertEquals(kandidat.personident.value, kandidatStatusRecord.personident)
            assertEquals(kandidat.createdAt, kandidatStatusRecord.createdAt)
            assertEquals(vurdertKandidat.status, kandidatStatusRecord.status.value)
            assertEquals(vurdertKandidat.status.isActive, kandidatStatusRecord.status.isActive)
            assertNotNull(kandidatStatusRecord.sisteVurdering)
            assertEquals(vurdertKandidat.vurdering?.createdAt?.millisekundOpplosning(), kandidatStatusRecord.sisteVurdering?.createdAt?.millisekundOpplosning())
            assertEquals(UserConstants.VEILEDER_IDENT, kandidatStatusRecord.sisteVurdering?.veilederident)
            assertEquals(VurderingType.FERDIGBEHANDLET, kandidatStatusRecord.sisteVurdering?.type)

            val pVurdering = database.getSenOppfolgingVurderinger().first()
            assertNotNull(pVurdering.publishedAt)
        }

        @Test
        fun `publishes unpublished vurdering once`() {
            val kandidat = senOppfolgingService.createKandidat(
                personident = ARBEIDSTAKER_PERSONIDENT,
                varselAt = nowUTC(),
                varselId = UUID.randomUUID(),
            )
            senOppfolgingService.addSvar(
                kandidat = kandidat,
                svarAt = OffsetDateTime.now(),
                onskerOppfolging = OnskerOppfolging.JA,
            )
            senOppfolgingRepository.setKandidatPublished(kandidatUuid = kandidat.uuid)
            senOppfolgingService.vurderKandidat(
                kandidat = kandidat,
                veilederident = UserConstants.VEILEDER_IDENT,
                begrunnelse = "Begrunnelse",
                type = VurderingType.FERDIGBEHANDLET
            )

            var results = senOppfolgingService.publishUnpublishedKandidatStatus().partition { it.isSuccess }
            assertEquals(1, results.first.size)
            assertEquals(0, results.second.size)

            results = senOppfolgingService.publishUnpublishedKandidatStatus().partition { it.isSuccess }
            assertEquals(0, results.first.size)
            assertEquals(0, results.second.size)

            verify(exactly = 1) { mockKandidatStatusProducer.send(any()) }
        }

        @Test
        fun `publishes nothing when no unpublished kandidat`() {
            val (success, failed) = senOppfolgingService.publishUnpublishedKandidatStatus()
                .partition { it.isSuccess }
            assertEquals(0, failed.size)
            assertEquals(0, success.size)
            verify(exactly = 0) { mockKandidatStatusProducer.send(any()) }
        }

        @Test
        fun `fails publishing when kafka-producer fails`() {
            val kandidat = senOppfolgingService.createKandidat(
                personident = ARBEIDSTAKER_PERSONIDENT,
                varselAt = nowUTC(),
                varselId = UUID.randomUUID(),
            )
            senOppfolgingService.addSvar(
                kandidat = kandidat,
                svarAt = OffsetDateTime.now(),
                onskerOppfolging = OnskerOppfolging.JA,
            )

            every { mockKandidatStatusProducer.send(any()) } throws Exception("Error producing to kafka")

            val (success, failed) = senOppfolgingService.publishUnpublishedKandidatStatus()
                .partition { it.isSuccess }
            assertEquals(1, failed.size)
            assertEquals(0, success.size)
            verify(exactly = 1) { mockKandidatStatusProducer.send(any()) }

            val kandidatList = senOppfolgingRepository.getUnpublishedKandidatStatuser()
            assertEquals(1, kandidatList.size)
            assertEquals(kandidat.uuid, kandidatList.first().uuid)
        }
    }

    @Nested
    @DisplayName("Ferdigbehandle kandidat")
    inner class FerdigbehandleKandidat {

        @Test
        fun `ferdigbehandler kandidat`() {
            val kandidat = senOppfolgingService.createKandidat(
                personident = ARBEIDSTAKER_PERSONIDENT,
                varselAt = nowUTC(),
            )
            val ferdigbehandletKandidat = senOppfolgingService.vurderKandidat(
                kandidat = kandidat,
                veilederident = UserConstants.VEILEDER_IDENT,
                begrunnelse = "Begrunnelse",
                type = VurderingType.FERDIGBEHANDLET,
            )

            assertEquals(SenOppfolgingStatus.FERDIGBEHANDLET, ferdigbehandletKandidat.status)
            assertNotNull(ferdigbehandletKandidat.vurdering)
            val vurdering = ferdigbehandletKandidat.vurdering!!
            assertEquals(VurderingType.FERDIGBEHANDLET, vurdering.type)
            assertEquals(UserConstants.VEILEDER_IDENT, vurdering.veilederident)

            val pKandidat = database.getSenOppfolgingKandidater().first()
            assertEquals(kandidat.uuid, pKandidat.uuid)
            assertEquals(SenOppfolgingStatus.FERDIGBEHANDLET, pKandidat.status)
            val pVurdering = database.getSenOppfolgingVurderinger().first()
            assertEquals(pKandidat.id, pVurdering.kandidatId)
            assertEquals(VurderingType.FERDIGBEHANDLET, pVurdering.type)
            assertEquals(UserConstants.VEILEDER_IDENT, pVurdering.veilederident)
            assertNull(pVurdering.publishedAt)
            assertEquals(vurdering.begrunnelse, pVurdering.begrunnelse)
        }
    }
}
