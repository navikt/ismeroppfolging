package no.nav.syfo.infrastructure.kafka

import no.nav.syfo.application.IKandidatStatusProducer
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.SenOppfolgingKandidat
import no.nav.syfo.domain.Status
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.*

class KandidatStatusProducer(private val producer: KafkaProducer<String, KandidatStatusRecord>) :
    IKandidatStatusProducer {

    override fun send(kandidatStatus: SenOppfolgingKandidat): Result<SenOppfolgingKandidat> =
        try {
            val record = ProducerRecord(
                TOPIC,
                kandidatStatus.personident.asProducerRecordKey(),
                KandidatStatusRecord.fromSenOppfolgingKandidat(kandidatStatus),
            )
            producer.send(record).get()
            Result.success(kandidatStatus)
        } catch (e: Exception) {
            log.error("Exception was thrown when attempting to send vurdering: ${e.message}")
            Result.failure(e)
        }

    companion object {
        private const val TOPIC = "teamsykefravr.ismeroppfolging-senoppfolging-kandidat-status"
        private val log = LoggerFactory.getLogger(KandidatStatusProducer::class.java)
    }
}

private fun Personident.asProducerRecordKey(): String = UUID.nameUUIDFromBytes(value.toByteArray()).toString()

data class KandidatStatusRecord(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val personident: String,
    val status: Status,
) {
    companion object {
        fun fromSenOppfolgingKandidat(kandidat: SenOppfolgingKandidat): KandidatStatusRecord =
            KandidatStatusRecord(
                uuid = kandidat.uuid,
                createdAt = kandidat.createdAt,
                personident = kandidat.personident.value,
                status = kandidat.status,
            )
    }
}

class KandidatStatusRecordSerializer : Serializer<KandidatStatusRecord> {
    private val mapper = configuredJacksonMapper()
    override fun serialize(topic: String?, data: KandidatStatusRecord?): ByteArray =
        mapper.writeValueAsBytes(data)
}
