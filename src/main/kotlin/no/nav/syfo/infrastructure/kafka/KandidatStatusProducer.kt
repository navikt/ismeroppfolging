package no.nav.syfo.infrastructure.kafka

import no.nav.syfo.application.IKandidatStatusProducer
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.SenOppfolgingKandidat
import no.nav.syfo.domain.SenOppfolgingStatus
import no.nav.syfo.domain.SenOppfolgingVurdering
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.*

class KandidatStatusProducer(private val producer: KafkaProducer<String, KandidatStatusRecord>) :
    IKandidatStatusProducer {

    override fun sendKandidat(kandidat: SenOppfolgingKandidat): Result<SenOppfolgingKandidat> =
        try {
            val record = ProducerRecord(
                TOPIC,
                kandidat.personident.asProducerRecordKey(),
                KandidatStatusRecord.fromKandidat(kandidat = kandidat),
            )
            producer.send(record).get()
            Result.success(kandidat)
        } catch (e: Exception) {
            log.error("Exception was thrown when attempting to send kandidat status: ${e.message}")
            Result.failure(e)
        }

    override fun sendVurdering(
        vurdering: SenOppfolgingVurdering,
        kandidat: SenOppfolgingKandidat
    ): Result<SenOppfolgingKandidat> =
        try {
            val record = ProducerRecord(
                TOPIC,
                kandidat.personident.asProducerRecordKey(),
                KandidatStatusRecord.fromVurdering(vurdering = vurdering, kandidat = kandidat),
            )
            producer.send(record).get()
            Result.success(kandidat)
        } catch (e: Exception) {
            log.error("Exception was thrown when attempting to send kandidat vurdering status: ${e.message}")
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
    val veilederident: String?,
    val status: StatusDTO,
) {
    companion object {
        fun fromKandidat(kandidat: SenOppfolgingKandidat): KandidatStatusRecord =
            KandidatStatusRecord(
                uuid = kandidat.uuid,
                createdAt = kandidat.createdAt,
                personident = kandidat.personident.value,
                veilederident = null,
                status = StatusDTO(
                    value = kandidat.status,
                    isActive = kandidat.status.isActive,
                    createdAt = kandidat.createdAt,
                ),
            )

        fun fromVurdering(vurdering: SenOppfolgingVurdering, kandidat: SenOppfolgingKandidat): KandidatStatusRecord =
            KandidatStatusRecord(
                uuid = kandidat.uuid,
                createdAt = kandidat.createdAt,
                personident = kandidat.personident.value,
                veilederident = vurdering.veilederident,
                status = StatusDTO(
                    value = kandidat.status,
                    isActive = kandidat.status.isActive,
                    createdAt = vurdering.createdAt,
                ),
            )
    }
}

data class StatusDTO(
    val value: SenOppfolgingStatus,
    val isActive: Boolean,
    val createdAt: OffsetDateTime,
)

class KandidatStatusRecordSerializer : Serializer<KandidatStatusRecord> {
    private val mapper = configuredJacksonMapper()
    override fun serialize(topic: String?, data: KandidatStatusRecord?): ByteArray =
        mapper.writeValueAsBytes(data)
}
