package no.nav.syfo.infrastructure.kafka.senoppfolging

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.application.SenOppfolgingService
import no.nav.syfo.domain.OnskerOppfolging
import no.nav.syfo.generators.generateSenOppfolgingSvarRecord
import no.nav.syfo.infrastructure.database.dropData
import no.nav.syfo.infrastructure.database.getSenOppfolgingKandidater
import no.nav.syfo.infrastructure.database.repository.SenOppfolgingRepository
import no.nav.syfo.infrastructure.kafka.KandidatStatusProducer
import no.nav.syfo.infrastructure.kafka.KandidatStatusRecord
import no.nav.syfo.mocks.mockPollConsumerRecords
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*

class SenOppfolgingSvarConsumerSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val kafkaConsumer = mockk<KafkaConsumer<String, SenOppfolgingSvarRecord>>()
    val kafkaProducer = mockk<KafkaProducer<String, KandidatStatusRecord>>()

    val senOppfolgingRepository = SenOppfolgingRepository(database = database)
    val senOppfolgingService = SenOppfolgingService(
        senOppfolgingRepository = senOppfolgingRepository,
        kandidatStatusProducer = KandidatStatusProducer(kafkaProducer),
    )
    val senOppfolgingSvarConsumer = SenOppfolgingSvarConsumer(senOppfolgingService = senOppfolgingService)

    beforeEachTest {
        every { kafkaConsumer.commitSync() } returns Unit
    }

    afterEachTest {
        database.dropData()
        clearAllMocks()
    }

    describe("pollAndProcessRecords") {
        it("creates kandidat with svar onsker oppfolging JA when answer is JA on BEHOV_FOR_OPPFOLGING") {
            val recordKey = UUID.randomUUID().toString()
            val question = SenOppfolgingQuestion(
                questionType = SenOppfolgingQuestionType.BEHOV_FOR_OPPFOLGING.name,
                answerType = BehovForOppfolgingSvar.JA.name,
            )
            val senOppfolgingSvarRecord = generateSenOppfolgingSvarRecord(
                question = question
            )

            kafkaConsumer.mockPollConsumerRecords(
                records = listOf(recordKey to senOppfolgingSvarRecord),
                topic = SENOPPFOLGING_SVAR_TOPIC,
            )

            senOppfolgingSvarConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)

            verify(exactly = 1) {
                kafkaConsumer.commitSync()
            }

            val kandidater = database.getSenOppfolgingKandidater()
            kandidater.size shouldBeEqualTo 1
            val kandidat = kandidater.first()
            kandidat.svarAt.shouldNotBeNull()
            kandidat.onskerOppfolging shouldBeEqualTo OnskerOppfolging.JA.name
        }

        it("creates kandidat with svar onsker oppfolging NEI when answer is NEI on BEHOV_FOR_OPPFOLGING") {
            val recordKey = UUID.randomUUID().toString()
            val question = SenOppfolgingQuestion(
                questionType = SenOppfolgingQuestionType.BEHOV_FOR_OPPFOLGING.name,
                answerType = BehovForOppfolgingSvar.NEI.name,
            )
            val senOppfolgingSvarRecord = generateSenOppfolgingSvarRecord(question = question)

            kafkaConsumer.mockPollConsumerRecords(
                records = listOf(recordKey to senOppfolgingSvarRecord),
                topic = SENOPPFOLGING_SVAR_TOPIC,
            )

            senOppfolgingSvarConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)

            verify(exactly = 1) {
                kafkaConsumer.commitSync()
            }

            val kandidater = database.getSenOppfolgingKandidater()
            kandidater.size shouldBeEqualTo 1
            val kandidat = kandidater.first()
            kandidat.svarAt.shouldNotBeNull()
            kandidat.onskerOppfolging shouldBeEqualTo OnskerOppfolging.NEI.name
        }
    }
})
