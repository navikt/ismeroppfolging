package no.nav.syfo.kartleggingssporsmal.infrastructure.kafka

import io.mockk.*
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.kartleggingssporsmal.domain.KandidatStatus
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.util.concurrent.Future

class EsyfovarselProducerTest {

    val kafkaProducer = mockk<KafkaProducer<String, EsyfovarselHendelse>>()
    val esyfovarselProducer = EsyfovarselProducer(kafkaProducer)

    @BeforeEach
    fun setUp() {
        clearMocks(kafkaProducer)
        coEvery {
            kafkaProducer.send(any())
        } returns mockk<Future<RecordMetadata>>(relaxed = true)
    }

    @Nested
    @DisplayName("Send varsel")
    inner class SendVarsel {

        @Test
        fun `sendKartleggingssporsmal should send varsel`() {
            val kandidat = KartleggingssporsmalKandidat(
                personident = ARBEIDSTAKER_PERSONIDENT,
                status = KandidatStatus.KANDIDAT,
            )

            val result = esyfovarselProducer.sendKartleggingssporsmal(kandidat)

            assertTrue(result.isSuccess)

            val producerRecordSlot = slot<ProducerRecord<String, EsyfovarselHendelse>>()
            verify(exactly = 1) {
                kafkaProducer.send(capture(producerRecordSlot))
            }

            val esyfovarselHendelse = producerRecordSlot.captured.value()
            assertEquals(esyfovarselHendelse.type, EsyfovarselHendelse.HendelseType.SM_KARTLEGGINGSSPORSMAL)
            assertNull(esyfovarselHendelse.ferdigstill)
        }

        @Test
        fun `sendKartleggingssporsmal should handle failure`() {
            coEvery {
                kafkaProducer.send(any())
            } throws RuntimeException("Kafka error")

            val kandidat = KartleggingssporsmalKandidat(
                personident = ARBEIDSTAKER_PERSONIDENT,
                status = KandidatStatus.KANDIDAT,
            )

            val result = esyfovarselProducer.sendKartleggingssporsmal(kandidat)

            assertTrue(result.isFailure)

            val producerRecordSlot = slot<ProducerRecord<String, EsyfovarselHendelse>>()
            verify(exactly = 1) {
                kafkaProducer.send(capture(producerRecordSlot))
            }
        }
    }

    @Nested
    @DisplayName("Ferdigstill varsel")
    inner class FerdigstillVarsel {

        @Test
        fun `ferdigstillKartleggingssporsmalVarsel should send varsel with ferdigstill flag true`() {
            val kandidat = KartleggingssporsmalKandidat(
                personident = ARBEIDSTAKER_PERSONIDENT,
                status = KandidatStatus.KANDIDAT,
            )

            val result = esyfovarselProducer.ferdigstillKartleggingssporsmalVarsel(kandidat)

            assertTrue(result.isSuccess)

            val producerRecordSlot = slot<ProducerRecord<String, EsyfovarselHendelse>>()
            verify(exactly = 1) {
                kafkaProducer.send(capture(producerRecordSlot))
            }

            val esyfovarselHendelse = producerRecordSlot.captured.value()
            assertEquals(esyfovarselHendelse.type, EsyfovarselHendelse.HendelseType.SM_KARTLEGGINGSSPORSMAL)
            assertTrue(esyfovarselHendelse.ferdigstill!!)
        }

        @Test
        fun `ferdigstillKartleggingssporsmalVarsel should handle failure`() {
            coEvery {
                kafkaProducer.send(any())
            } throws RuntimeException("Kafka error")

            val kandidat = KartleggingssporsmalKandidat(
                personident = ARBEIDSTAKER_PERSONIDENT,
                status = KandidatStatus.KANDIDAT,
            )

            val result = esyfovarselProducer.ferdigstillKartleggingssporsmalVarsel(kandidat)

            assertTrue(result.isFailure)

            val producerRecordSlot = slot<ProducerRecord<String, EsyfovarselHendelse>>()
            verify(exactly = 1) {
                kafkaProducer.send(capture(producerRecordSlot))
            }
        }
    }
}
