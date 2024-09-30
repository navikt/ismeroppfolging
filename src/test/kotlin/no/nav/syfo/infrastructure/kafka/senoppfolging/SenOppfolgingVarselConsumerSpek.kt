package no.nav.syfo.infrastructure.kafka.senoppfolging

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.application.SenOppfolgingService
import no.nav.syfo.generators.generateSenOppfolgingVarselRecord
import no.nav.syfo.infrastructure.database.dropData
import no.nav.syfo.infrastructure.database.getSenOppfolgingKandidater
import no.nav.syfo.infrastructure.database.repository.SenOppfolgingRepository
import no.nav.syfo.infrastructure.kafka.KandidatStatusProducer
import no.nav.syfo.infrastructure.kafka.KandidatStatusRecord
import no.nav.syfo.mocks.mockPollConsumerRecords
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*

class SenOppfolgingVarselConsumerSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val kafkaConsumer = mockk<KafkaConsumer<String, KSenOppfolgingVarselDTO>>()
    val kafkaProducer = mockk<KafkaProducer<String, KandidatStatusRecord>>()

    val senOppfolgingRepository = SenOppfolgingRepository(database = database)
    val senOppfolgingService = SenOppfolgingService(
        senOppfolgingRepository = senOppfolgingRepository,
        kandidatStatusProducer = KandidatStatusProducer(kafkaProducer),
    )
    val senOppfolgingVarselConsumer = SenOppfolgingVarselConsumer(senOppfolgingService = senOppfolgingService)

    beforeEachTest {
        every { kafkaConsumer.commitSync() } returns Unit
    }

    afterEachTest {
        database.dropData()
        clearAllMocks()
    }

    describe("pollAndProcessRecords") {
        it("creates kandidat with reference to varsel") {
            val recordKey = UUID.randomUUID().toString()
            val senOppfolgingVarselRecord = generateSenOppfolgingVarselRecord(
                personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT.value,
            )

            kafkaConsumer.mockPollConsumerRecords(
                records = listOf(recordKey to senOppfolgingVarselRecord),
                topic = SENOPPFOLGING_VARSEL_TOPIC,
            )

            senOppfolgingVarselConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)

            verify(exactly = 1) {
                kafkaConsumer.commitSync()
            }

            val kandidater = database.getSenOppfolgingKandidater()
            kandidater.size shouldBeEqualTo 1
            val kandidat = kandidater.first()
            kandidat.varselAt.shouldNotBeNull()
            kandidat.varselId shouldBeEqualTo senOppfolgingVarselRecord.uuid
            kandidat.svarAt.shouldBeNull()
        }
        it("does not create duplicate kandidat with reference to same varselId") {
            val recordKey = UUID.randomUUID().toString()
            val senOppfolgingVarselRecord = generateSenOppfolgingVarselRecord(
                personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT.value,
            )

            kafkaConsumer.mockPollConsumerRecords(
                records = listOf(recordKey to senOppfolgingVarselRecord),
                topic = SENOPPFOLGING_VARSEL_TOPIC,
            )

            senOppfolgingVarselConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)
            verify(exactly = 1) {
                kafkaConsumer.commitSync()
            }

            database.getSenOppfolgingKandidater().size shouldBeEqualTo 1

            senOppfolgingVarselConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)
            verify(exactly = 2) {
                kafkaConsumer.commitSync()
            }

            database.getSenOppfolgingKandidater().size shouldBeEqualTo 1
        }
        it("does nothing when no kandidat") {
            kafkaConsumer.mockPollConsumerRecords(
                records = emptyList(),
                topic = SENOPPFOLGING_VARSEL_TOPIC,
            )

            senOppfolgingVarselConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)
            verify(exactly = 0) {
                kafkaConsumer.commitSync()
            }

            database.getSenOppfolgingKandidater().size shouldBeEqualTo 0
        }
    }
})
