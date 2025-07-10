package no.nav.syfo.infrastructure.kafka.jobbforventning

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.generators.createKafkaOppfolgingstilfellePerson
import no.nav.syfo.mocks.mockPollConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class OppfolgingstilfelleConsumerTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val kafkaConsumer = mockk<KafkaConsumer<String, KafkaOppfolgingstilfellePersonDTO>>()

    private val oppfolgingstilfelleConsumer = OppfolgingstilfelleConsumer()

    @BeforeEach
    fun setUp() {
        every { kafkaConsumer.commitSync() } returns Unit
    }

    @AfterEach
    fun tearDown() {
        database.resetDatabase()
        clearAllMocks()
    }

    @Test
    fun `pollAndProcessRecords should process records and commit offsets`() {
        val recordKey = UUID.randomUUID().toString()
        val oppfolgingstilfelleRecord = createKafkaOppfolgingstilfellePerson()
        kafkaConsumer.mockPollConsumerRecords(
            records = listOf(recordKey to oppfolgingstilfelleRecord),
            topic = OPPFOLGINGSTILFELLE_PERSON_TOPIC,
        )

        oppfolgingstilfelleConsumer.pollAndProcessRecords(kafkaConsumer)

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }
    }

    @Test
    fun `pollAndProcessRecords should not commit offsets if no records are processed`() {
        kafkaConsumer.mockPollConsumerRecords(
            records = emptyList(),
            topic = OPPFOLGINGSTILFELLE_PERSON_TOPIC,
        )

        oppfolgingstilfelleConsumer.pollAndProcessRecords(kafkaConsumer)

        verify(exactly = 0) {
            kafkaConsumer.commitSync()
        }
    }
}
