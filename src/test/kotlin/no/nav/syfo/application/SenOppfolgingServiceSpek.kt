package no.nav.syfo.application

import io.mockk.*
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.infrastructure.database.dropData
import no.nav.syfo.infrastructure.database.repository.SenOppfolgingRepository
import no.nav.syfo.infrastructure.kafka.KandidatStatusProducer
import no.nav.syfo.infrastructure.kafka.KandidatStatusRecord
import no.nav.syfo.util.nowUTC
import org.amshove.kluent.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
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

        describe("publishUnpublishedVurderinger") {

            it("publishes unpublished vurdering") {
                val kandidat = senOppfolgingService.createKandidat(
                    personident = ARBEIDSTAKER_PERSONIDENT,
                    varselAt = nowUTC(),
                )
                val (success, failed) = senOppfolgingService.publishUnpublishedKandidatStatus().partition { it.isSuccess }
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
            }

            it("publishes nothing when no unpublished vurdering") {
                val (success, failed) = senOppfolgingService.publishUnpublishedKandidatStatus().partition { it.isSuccess }
                failed.size shouldBeEqualTo 0
                success.size shouldBeEqualTo 0
                verify(exactly = 0) { mockKandidatStatusProducer.send(any()) }
            }

            it("fails publishing when kafka-producer fails") {
                val kandidat = senOppfolgingService.createKandidat(
                    personident = ARBEIDSTAKER_PERSONIDENT,
                    varselAt = nowUTC(),
                )

                every { mockKandidatStatusProducer.send(any()) } throws Exception("Error producing to kafka")

                val (success, failed) = senOppfolgingService.publishUnpublishedKandidatStatus().partition { it.isSuccess }
                failed.size shouldBeEqualTo 1
                success.size shouldBeEqualTo 0
                verify(exactly = 1) { mockKandidatStatusProducer.send(any()) }

                val kandidatList = senOppfolgingRepository.getUnpublishedKandidater()
                kandidatList.size shouldBeEqualTo 1
                kandidatList.first().uuid.shouldBeEqualTo(kandidat.uuid)
            }
        }
    }
})
