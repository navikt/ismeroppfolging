package no.nav.syfo.senoppfolging.infrastructure.kafka.producer

import no.nav.syfo.senoppfolging.application.IKandidatStatusProducer
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.senoppfolging.domain.SenOppfolgingKandidat
import no.nav.syfo.senoppfolging.domain.SenOppfolgingStatus
import no.nav.syfo.senoppfolging.domain.VurderingType
import no.nav.syfo.shared.util.configuredJacksonMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.*

class KandidatStatusProducer(private val producer: KafkaProducer<String, KandidatStatusRecord>) :
    IKandidatStatusProducer {

    override fun send(kandidat: SenOppfolgingKandidat): Result<SenOppfolgingKandidat> =
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
    val status: StatusDTO,
    val sisteVurdering: VurderingDTO?,
) {
    companion object {
        fun fromKandidat(kandidat: SenOppfolgingKandidat): KandidatStatusRecord =
            KandidatStatusRecord(
                uuid = kandidat.uuid,
                createdAt = kandidat.createdAt,
                personident = kandidat.personident.value,
                status = StatusDTO(
                    value = kandidat.status,
                    isActive = kandidat.status.isActive,
                ),
                sisteVurdering = kandidat.vurdering?.let {
                    VurderingDTO(
                        uuid = it.uuid,
                        type = it.type,
                        createdAt = it.createdAt,
                        veilederident = it.veilederident,
                    )
                },
            )
    }
}

data class StatusDTO(
    val value: SenOppfolgingStatus,
    val isActive: Boolean,
)

data class VurderingDTO(
    val uuid: UUID,
    val type: VurderingType,
    val createdAt: OffsetDateTime,
    val veilederident: String,
)

class KandidatStatusRecordSerializer : Serializer<KandidatStatusRecord> {
    private val mapper = configuredJacksonMapper()
    override fun serialize(topic: String?, data: KandidatStatusRecord?): ByteArray =
        mapper.writeValueAsBytes(data)
}
