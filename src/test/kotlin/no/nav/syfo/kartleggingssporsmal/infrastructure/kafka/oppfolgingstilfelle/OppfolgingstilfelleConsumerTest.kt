package no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.oppfolgingstilfelle

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.kartleggingssporsmal.application.KartleggingssporsmalService
import no.nav.syfo.kartleggingssporsmal.generators.createKafkaOppfolgingstilfellePersonDTO
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.EsyfovarselHendelse
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.EsyfovarselProducer
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.KartleggingssporsmalKandidatProducer
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.KartleggingssporsmalKandidatStatusRecord
import no.nav.syfo.shared.infrastructure.kafka.mockPollConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class OppfolgingstilfelleConsumerTest {

    private val externalMockEnvironment = ExternalMockEnvironment.Companion.instance
    private val database = externalMockEnvironment.database
    private val kafkaConsumer = mockk<KafkaConsumer<String, KafkaOppfolgingstilfellePersonDTO>>()

    private val mockEsyfoVarselProducer = mockk<KafkaProducer<String, EsyfovarselHendelse>>()
    private val esyfovarselProducer = EsyfovarselProducer(mockEsyfoVarselProducer)

    private val mockKandidatProducer = mockk<KafkaProducer<String, KartleggingssporsmalKandidatStatusRecord>>()
    private val kartleggingssporsmalKandidatProducer = KartleggingssporsmalKandidatProducer(mockKandidatProducer)

    private val kartleggingssporsmalService = KartleggingssporsmalService(
        behandlendeEnhetClient = externalMockEnvironment.behandlendeEnhetClient,
        kartleggingssporsmalRepository = externalMockEnvironment.kartleggingssporsmalRepository,
        oppfolgingstilfelleClient = externalMockEnvironment.oppfolgingstilfelleClient,
        esyfoVarselProducer = esyfovarselProducer,
        kartleggingssporsmalKandidatProducer = kartleggingssporsmalKandidatProducer,
        pdlClient = externalMockEnvironment.pdlClient,
        vedtak14aClient = externalMockEnvironment.vedtak14aClient,
        senOppfolgingService = externalMockEnvironment.senOppfolgingService,
        isKandidatPublishingEnabled = true,
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
        val oppfolgingstilfelleRecord = createKafkaOppfolgingstilfellePersonDTO()
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
