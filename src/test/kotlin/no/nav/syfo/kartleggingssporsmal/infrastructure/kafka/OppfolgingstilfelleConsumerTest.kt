package no.nav.syfo.kartleggingssporsmal.infrastructure.kafka

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.kartleggingssporsmal.application.KartleggingssporsmalService
import no.nav.syfo.kartleggingssporsmal.generators.createKafkaOppfolgingstilfellePerson
import no.nav.syfo.shared.infrastructure.kafka.mockPollConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class OppfolgingstilfelleConsumerTest {

    private val externalMockEnvironment = ExternalMockEnvironment.Companion.instance
    private val database = externalMockEnvironment.database
    private val kafkaConsumer = mockk<KafkaConsumer<String, KafkaOppfolgingstilfellePersonDTO>>()

    private val kartleggingssporsmalService = KartleggingssporsmalService(
        behandlendeEnhetClient = externalMockEnvironment.behandlendeEnhetClient,
    )
    private val oppfolgingstilfelleConsumer = OppfolgingstilfelleConsumer(kartleggingssporsmalService)

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

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(kafkaConsumer)
        }

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

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(kafkaConsumer)
        }

        verify(exactly = 0) {
            kafkaConsumer.commitSync()
        }
    }
}
