package no.nav.syfo.infrastructure.kafka.senoppfolging

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.application.SenOppfolgingService
import no.nav.syfo.generators.generateSenOppfolgingSvarRecord
import no.nav.syfo.infrastructure.database.dropData
import no.nav.syfo.mocks.mockPollConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*

class SenOppfolgingSvarConsumerSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val kafkaConsumer = mockk<KafkaConsumer<String, SenOppfolgingSvarRecord>>()

    val senOppfolgingSvarConsumer = SenOppfolgingSvarConsumer(senOppfolgingService = SenOppfolgingService())

    beforeEachTest {
        every { kafkaConsumer.commitSync() } returns Unit
    }

    afterEachTest {
        database.dropData()
        clearAllMocks()
    }

    describe("pollAndProcessRecords") {
        it("receives record and commits") {
            val recordKey = UUID.randomUUID().toString()
            val senOppfolgingSvarRecord = generateSenOppfolgingSvarRecord()
            kafkaConsumer.mockPollConsumerRecords(
                records = listOf(recordKey to senOppfolgingSvarRecord),
                topic = SENOPPFOLGING_SVAR_TOPIC,
            )

            senOppfolgingSvarConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)

            verify(exactly = 1) {
                kafkaConsumer.commitSync()
            }
        }
    }
})
