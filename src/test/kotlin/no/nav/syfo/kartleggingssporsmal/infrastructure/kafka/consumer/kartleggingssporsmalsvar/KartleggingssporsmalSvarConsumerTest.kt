package no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.consumer.kartleggingssporsmalsvar

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.kartleggingssporsmal.application.KartleggingssporsmalService
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.kartleggingssporsmalsvar.KARTLEGGINGSSPORSMAL_SVAR_TOPIC
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.kartleggingssporsmalsvar.KafkaKartleggingssporsmalSvarDTO
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.kartleggingssporsmalsvar.KartleggingssporsmalSvarConsumer
import no.nav.syfo.shared.infrastructure.kafka.mockPollConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.*

class KartleggingssporsmalSvarConsumerTest {
    private val kartleggingssporsmalService = mockk<KartleggingssporsmalService>()
    private val kafkaConsumer = mockk<KafkaConsumer<String, KafkaKartleggingssporsmalSvarDTO>>()
    private val kartleggingssporsmalSvarConsumer = KartleggingssporsmalSvarConsumer(kartleggingssporsmalService)

    @BeforeEach
    fun setUp() {
        every { kafkaConsumer.commitSync() } returns Unit
        coEvery { kartleggingssporsmalService.registrerSvar(any(), any(), any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `pollAndProcessRecords should process records and commit offsets`() {
        val recordKey = UUID.randomUUID().toString()
        val svarRecord = KafkaKartleggingssporsmalSvarDTO(
            personident = ARBEIDSTAKER_PERSONIDENT.value,
            kandidatId = UUID.randomUUID(),
            svarId = UUID.randomUUID(),
            createdAt = OffsetDateTime.now(),
        )
        kafkaConsumer.mockPollConsumerRecords(
            records = listOf(recordKey to svarRecord),
            topic = KARTLEGGINGSSPORSMAL_SVAR_TOPIC,
        )

        runBlocking {
            kartleggingssporsmalSvarConsumer.pollAndProcessRecords(kafkaConsumer)
        }

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }
    }

    @Test
    fun `pollAndProcessRecords should not commit offsets if no records are processed`() {
        kafkaConsumer.mockPollConsumerRecords(
            records = emptyList(),
            topic = KARTLEGGINGSSPORSMAL_SVAR_TOPIC,
        )

        runBlocking {
            kartleggingssporsmalSvarConsumer.pollAndProcessRecords(kafkaConsumer)
        }

        verify(exactly = 0) {
            kafkaConsumer.commitSync()
        }
    }
}
