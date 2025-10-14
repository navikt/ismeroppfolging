package no.nav.syfo.kartleggingssporsmal.infrastructure.kafka

import no.nav.syfo.kartleggingssporsmal.application.IKartleggingssporsmalKandidatProducer
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.shared.util.configuredJacksonMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import java.util.*

class KartleggingssporsmalKandidatProducer(private val producer: KafkaProducer<String, KartleggingssporsmalKandidatRecord>) :
    IKartleggingssporsmalKandidatProducer {

    override fun send(kandidat: KartleggingssporsmalKandidat): Result<KartleggingssporsmalKandidat> =
        try {
            val personidentKey = UUID.nameUUIDFromBytes(kandidat.personident.toString().toByteArray()).toString()
            val record = ProducerRecord(
                TOPIC,
                personidentKey,
                KartleggingssporsmalKandidatRecord.from(kandidat = kandidat),
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

data class KartleggingssporsmalKandidatRecord(
    val uuid: UUID,
    val createdAt: String,
    val personident: String,
    val status: String,
    val varsletAt: String?,
) {
    companion object {
        fun from(kandidat: KartleggingssporsmalKandidat): KartleggingssporsmalKandidatRecord =
            KartleggingssporsmalKandidatRecord(
                uuid = kandidat.uuid,
                createdAt = kandidat.createdAt.toString(),
                personident = kandidat.personident.toString(),
                status = kandidat.status.name,
                varsletAt = kandidat.varsletAt?.toString(),
            )
    }
}

class KartleggingssporsmalKandidatRecordSerializer : Serializer<KartleggingssporsmalKandidatRecord> {
    private val mapper = configuredJacksonMapper()
    override fun serialize(topic: String?, data: KartleggingssporsmalKandidatRecord?): ByteArray =
        mapper.writeValueAsBytes(data)
}
