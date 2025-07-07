package no.nav.syfo.infrastructure.kafka.senoppfolging

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.application.SenOppfolgingService
import no.nav.syfo.generators.generateSenOppfolgingVarselRecord
import no.nav.syfo.infrastructure.database.getSenOppfolgingKandidater
import no.nav.syfo.infrastructure.database.repository.SenOppfolgingRepository
import no.nav.syfo.infrastructure.kafka.KandidatStatusProducer
import no.nav.syfo.infrastructure.kafka.KandidatStatusRecord
import no.nav.syfo.mocks.mockPollConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class SenOppfolgingVarselConsumerTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val kafkaConsumer = mockk<KafkaConsumer<String, KSenOppfolgingVarselDTO>>()
    private val kafkaProducer = mockk<KafkaProducer<String, KandidatStatusRecord>>()

    private val senOppfolgingRepository = SenOppfolgingRepository(database = database)
    private val senOppfolgingService = SenOppfolgingService(
        senOppfolgingRepository = senOppfolgingRepository,
        kandidatStatusProducer = KandidatStatusProducer(kafkaProducer),
    )
    private val senOppfolgingVarselConsumer = SenOppfolgingVarselConsumer(senOppfolgingService = senOppfolgingService)

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
    fun `creates kandidat with reference to varsel`() {
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
        assertEquals(1, kandidater.size)
        val kandidat = kandidater.first()
        assertNotNull(kandidat.varselAt)
        assertEquals(senOppfolgingVarselRecord.uuid, kandidat.varselId)
        assertNull(kandidat.svarAt)
    }

    @Test
    fun `does not create duplicate kandidat with reference to same varselId`() {
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

        assertEquals(1, database.getSenOppfolgingKandidater().size)

        senOppfolgingVarselConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)
        verify(exactly = 2) {
            kafkaConsumer.commitSync()
        }

        assertEquals(1, database.getSenOppfolgingKandidater().size)
    }

    @Test
    fun `does nothing when no kandidat`() {
        kafkaConsumer.mockPollConsumerRecords(
            records = emptyList(),
            topic = SENOPPFOLGING_VARSEL_TOPIC,
        )

        senOppfolgingVarselConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)
        verify(exactly = 0) {
            kafkaConsumer.commitSync()
        }

        assertEquals(0, database.getSenOppfolgingKandidater().size)
    }
}
