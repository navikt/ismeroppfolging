package no.nav.syfo.kartleggingssporsmal.infrastructure.kafka

import no.nav.syfo.kartleggingssporsmal.application.IKartleggingssporsmalKandidatProducer
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidatStatusendring
import no.nav.syfo.shared.util.configuredJacksonMapper
import no.nav.syfo.shared.util.toLocalDateTimeOslo
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*

class KartleggingssporsmalKandidatProducer(private val producer: KafkaProducer<String, KartleggingssporsmalKandidatStatusRecord>) :
    IKartleggingssporsmalKandidatProducer {

    override fun send(
        kandidat: KartleggingssporsmalKandidat,
        statusEndring: KartleggingssporsmalKandidatStatusendring,
    ): Result<KartleggingssporsmalKandidat> =
        try {
            val personidentKey = UUID.nameUUIDFromBytes(kandidat.personident.toString().toByteArray()).toString()
            val record = ProducerRecord(
                TOPIC,
                personidentKey,
                KartleggingssporsmalKandidatStatusRecord.from(
                    kandidat = kandidat,
                    statusEndring = statusEndring,
                ),
            )
            producer.send(record).get()
            Result.success(kandidat)
        } catch (e: Exception) {
            log.error("Exception was thrown when attempting to send kartleggingssporsmal kandidat status: ${e.message}")
            Result.failure(e)
        }

    companion object {
        private const val TOPIC = "teamsykefravr.ismeroppfolging-kartleggingssporsmal-kandidat"
        private val log = LoggerFactory.getLogger(KartleggingssporsmalKandidatProducer::class.java)
    }
}

data class KartleggingssporsmalKandidatStatusRecord(
    val kandidatUuid: UUID,
    val personident: String,
    val createdAt: OffsetDateTime,
    val status: String,
) {
    companion object {
        fun from(
            kandidat: KartleggingssporsmalKandidat,
            statusEndring: KartleggingssporsmalKandidatStatusendring,
        ): KartleggingssporsmalKandidatStatusRecord =
            KartleggingssporsmalKandidatStatusRecord(
                kandidatUuid = kandidat.uuid,
                personident = kandidat.personident.value,
                createdAt = statusEndring.createdAt,
                status = statusEndring.status.name,
            )
    }
}

class KartleggingssporsmalKandidatRecordSerializer : Serializer<KartleggingssporsmalKandidatStatusRecord> {
    private val mapper = configuredJacksonMapper()
    override fun serialize(topic: String?, data: KartleggingssporsmalKandidatStatusRecord?): ByteArray =
        mapper.writeValueAsBytes(data)
}
