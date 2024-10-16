package no.nav.syfo.application

import io.mockk.*
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.domain.OnskerOppfolging
import no.nav.syfo.domain.SenOppfolgingStatus
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.infrastructure.database.dropData
import no.nav.syfo.infrastructure.database.getSenOppfolgingKandidater
import no.nav.syfo.infrastructure.database.getSenOppfolgingVurderinger
import no.nav.syfo.infrastructure.database.repository.SenOppfolgingRepository
import no.nav.syfo.infrastructure.kafka.KandidatStatusProducer
import no.nav.syfo.infrastructure.kafka.KandidatStatusRecord
import no.nav.syfo.util.millisekundOpplosning
import no.nav.syfo.util.nowUTC
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.Future

class SenOppfolgingServiceSpek : Spek({
    describe(SenOppfolgingService::class.java.simpleName) {

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        val senOppfolgingRepository = SenOppfolgingRepository(database = database)

        val mockKandidatStatusProducer = mockk<KafkaProducer<String, KandidatStatusRecord>>(relaxed = true)
        val kandidatStatusProducer = KandidatStatusProducer(
            producer = mockKandidatStatusProducer,
        )

        val senOppfolgingService = SenOppfolgingService(
            senOppfolgingRepository = senOppfolgingRepository,
            kandidatStatusProducer = kandidatStatusProducer,
        )

        beforeEachTest {
            clearAllMocks()
            coEvery { mockKandidatStatusProducer.send(any()) } returns mockk<Future<RecordMetadata>>(relaxed = true)
        }

        afterEachTest {
            database.dropData()
        }

        describe("publishUnpublishedKandidatStatus") {

            it("publishes unpublished kandidat with svar") {
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
                failed.size shouldBeEqualTo 0
                success.size shouldBeEqualTo 1

                val producerRecordSlot = slot<ProducerRecord<String, KandidatStatusRecord>>()
                verify(exactly = 1) { mockKandidatStatusProducer.send(capture(producerRecordSlot)) }

                val kandidatStatusRecord = producerRecordSlot.captured.value()
                kandidatStatusRecord.uuid shouldBeEqualTo kandidat.uuid
                kandidatStatusRecord.personident shouldBeEqualTo kandidat.personident.value
                kandidatStatusRecord.createdAt shouldBeEqualTo kandidat.createdAt
                kandidatStatusRecord.status.value shouldBeEqualTo kandidat.status
                kandidatStatusRecord.status.isActive shouldBeEqualTo kandidat.status.isActive
                kandidatStatusRecord.sisteVurdering.shouldBeNull()

                val pKandidat = database.getSenOppfolgingKandidater().first()
                pKandidat.publishedAt.shouldNotBeNull()
            }

            it("publishes unpublished kandidat once") {
                val kandidat = senOppfolgingService.createKandidat(
                    personident = ARBEIDSTAKER_PERSONIDENT,
                )
                senOppfolgingService.addSvar(
                    kandidat = kandidat,
                    svarAt = OffsetDateTime.now(),
                    onskerOppfolging = OnskerOppfolging.JA,
                )
                var results = senOppfolgingService.publishUnpublishedKandidatStatus().partition { it.isSuccess }
                results.first.size shouldBeEqualTo 1
                results.second.size shouldBeEqualTo 0

                results = senOppfolgingService.publishUnpublishedKandidatStatus().partition { it.isSuccess }
                results.first.size shouldBeEqualTo 0
                results.second.size shouldBeEqualTo 0

                verify(exactly = 1) { mockKandidatStatusProducer.send(any()) }
            }

            it("does not publish unpublished kandidat with no svar and less than 10 days passed since varsel") {
                senOppfolgingService.createKandidat(
                    personident = ARBEIDSTAKER_PERSONIDENT,
                    varselAt = nowUTC(),
                )
                val (success, failed) = senOppfolgingService.publishUnpublishedKandidatStatus()
                    .partition { it.isSuccess }
                failed.size shouldBeEqualTo 0
                success.size shouldBeEqualTo 0

                val pKandidat = database.getSenOppfolgingKandidater().first()
                pKandidat.publishedAt.shouldBeNull()
            }

            it("publishes unpublished kandidat with no svar and 10 days since varsel") {
                val kandidatIkkeSvart = senOppfolgingService.createKandidat(
                    personident = ARBEIDSTAKER_PERSONIDENT,
                    varselAt = OffsetDateTime.now().minusDays(10),
                )
                val (success, failed) = senOppfolgingService.publishUnpublishedKandidatStatus()
                    .partition { it.isSuccess }
                failed.size shouldBeEqualTo 0
                success.size shouldBeEqualTo 1

                val producerRecordSlot = slot<ProducerRecord<String, KandidatStatusRecord>>()
                verify(exactly = 1) { mockKandidatStatusProducer.send(capture(producerRecordSlot)) }

                val kandidatStatusRecord = producerRecordSlot.captured.value()
                kandidatStatusRecord.uuid shouldBeEqualTo kandidatIkkeSvart.uuid
                kandidatStatusRecord.personident shouldBeEqualTo kandidatIkkeSvart.personident.value
                kandidatStatusRecord.createdAt shouldBeEqualTo kandidatIkkeSvart.createdAt
                kandidatStatusRecord.status.value shouldBeEqualTo kandidatIkkeSvart.status
                kandidatStatusRecord.status.isActive shouldBeEqualTo kandidatIkkeSvart.status.isActive
                kandidatStatusRecord.sisteVurdering.shouldBeNull()

                val pKandidat = database.getSenOppfolgingKandidater().first()
                pKandidat.publishedAt.shouldNotBeNull()
            }

            it("publishes unpublished vurdering") {
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
                failed.size shouldBeEqualTo 0
                success.size shouldBeEqualTo 1

                val producerRecordSlot = slot<ProducerRecord<String, KandidatStatusRecord>>()
                verify(exactly = 1) { mockKandidatStatusProducer.send(capture(producerRecordSlot)) }

                val kandidatStatusRecord = producerRecordSlot.captured.value()
                kandidatStatusRecord.uuid shouldBeEqualTo kandidat.uuid
                kandidatStatusRecord.personident shouldBeEqualTo kandidat.personident.value
                kandidatStatusRecord.createdAt shouldBeEqualTo kandidat.createdAt
                kandidatStatusRecord.status.value shouldBeEqualTo vurdertKandidat.status
                kandidatStatusRecord.status.isActive shouldBeEqualTo vurdertKandidat.status.isActive
                kandidatStatusRecord.sisteVurdering.shouldNotBeNull()
                kandidatStatusRecord.sisteVurdering?.createdAt?.millisekundOpplosning() shouldBeEqualTo vurdertKandidat.vurdering?.createdAt?.millisekundOpplosning()
                kandidatStatusRecord.sisteVurdering?.veilederident shouldBeEqualTo UserConstants.VEILEDER_IDENT
                kandidatStatusRecord.sisteVurdering?.type shouldBeEqualTo VurderingType.FERDIGBEHANDLET

                val pVurdering = database.getSenOppfolgingVurderinger().first()
                pVurdering.publishedAt.shouldNotBeNull()
            }

            it("publishes unpublished vurdering once") {
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
                results.first.size shouldBeEqualTo 1
                results.second.size shouldBeEqualTo 0

                results = senOppfolgingService.publishUnpublishedKandidatStatus().partition { it.isSuccess }
                results.first.size shouldBeEqualTo 0
                results.second.size shouldBeEqualTo 0

                verify(exactly = 1) { mockKandidatStatusProducer.send(any()) }
            }

            it("publishes nothing when no unpublished kandidat") {
                val (success, failed) = senOppfolgingService.publishUnpublishedKandidatStatus()
                    .partition { it.isSuccess }
                failed.size shouldBeEqualTo 0
                success.size shouldBeEqualTo 0
                verify(exactly = 0) { mockKandidatStatusProducer.send(any()) }
            }

            it("fails publishing when kafka-producer fails") {
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
                failed.size shouldBeEqualTo 1
                success.size shouldBeEqualTo 0
                verify(exactly = 1) { mockKandidatStatusProducer.send(any()) }

                val kandidatList = senOppfolgingRepository.getUnpublishedKandidatStatuser()
                kandidatList.size shouldBeEqualTo 1
                kandidatList.first().uuid.shouldBeEqualTo(kandidat.uuid)
            }
        }

        describe("ferdigbehandleKandidat") {
            it("ferdigbehandler kandidat") {
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

                ferdigbehandletKandidat.status shouldBeEqualTo SenOppfolgingStatus.FERDIGBEHANDLET
                ferdigbehandletKandidat.vurdering shouldNotBeEqualTo null
                val vurdering = ferdigbehandletKandidat.vurdering!!
                vurdering.type shouldBeEqualTo VurderingType.FERDIGBEHANDLET
                vurdering.veilederident shouldBeEqualTo UserConstants.VEILEDER_IDENT

                val pKandidat = database.getSenOppfolgingKandidater().first()
                pKandidat.uuid shouldBeEqualTo kandidat.uuid
                pKandidat.status shouldBeEqualTo SenOppfolgingStatus.FERDIGBEHANDLET
                val pVurdering = database.getSenOppfolgingVurderinger().first()
                pVurdering.kandidatId shouldBeEqualTo pKandidat.id
                pVurdering.type shouldBeEqualTo VurderingType.FERDIGBEHANDLET
                pVurdering.veilederident shouldBeEqualTo UserConstants.VEILEDER_IDENT
                pVurdering.publishedAt.shouldBeNull()
                pVurdering.begrunnelse shouldBeEqualTo vurdering.begrunnelse
            }
        }
    }
})
