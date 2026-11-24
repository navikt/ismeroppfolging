package no.nav.syfo.kartleggingssporsmal.infrastructure.kafka

import no.nav.syfo.kartleggingssporsmal.application.IKartleggingssporsmalKandidatV2Producer
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidatStatusendring
import no.nav.syfo.shared.util.configuredJacksonMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.*

class KartleggingssporsmalKandidatV2Producer(
    private val producer: KafkaProducer<String, KartleggingssporsmalKandidatStatusV2Record>,
) : IKartleggingssporsmalKandidatV2Producer {

    override fun send(kandidat: KartleggingssporsmalKandidat): Result<KartleggingssporsmalKandidat> =
        try {
            val personidentKey = UUID.nameUUIDFromBytes(kandidat.personident.toString().toByteArray()).toString()
            val record = ProducerRecord(
                TOPIC,
                personidentKey,
                KartleggingssporsmalKandidatStatusV2Record.from(kandidat = kandidat),
            )
            producer.send(record).get()
            Result.success(kandidat)
        } catch (e: Exception) {
            log.error("Exception was thrown when attempting to send kartleggingssporsmal kandidat status v2 (statusUuid: ${kandidat.status.uuid})", e)
            Result.failure(e)
        }

    companion object {
        private const val TOPIC = "teamsykefravr.ismeroppfolging-kartleggingssporsmal-kandidat-v2"
        private val log = LoggerFactory.getLogger(KartleggingssporsmalKandidatV2Producer::class.java)
    }
}

data class KartleggingssporsmalKandidatStatusV2Record(
    val statusUuid: UUID,
    val kandidatUuid: UUID,
    val personident: String,
    val createdAt: OffsetDateTime,
    val status: String,
    val skjemavariant: String,
    val vurdering: VurderingRecord?,
) {
    companion object {
        fun from(kandidat: KartleggingssporsmalKandidat): KartleggingssporsmalKandidatStatusV2Record {
            val status = kandidat.status
            return KartleggingssporsmalKandidatStatusV2Record(
                statusUuid = status.uuid,
                kandidatUuid = kandidat.uuid,
                personident = kandidat.personident.value,
                createdAt = status.createdAt,
                status = status.kandidatStatus.name,
                skjemavariant = kandidat.skjemavariant.name,
                vurdering = (status as? KartleggingssporsmalKandidatStatusendring.Ferdigbehandlet)?.let {
                    VurderingRecord(
                        alternativ = it.vurderingAlternativ?.name,
                        fritekst = it.vurderingFritekst,
                        veilederident = it.veilederident,
                    )
                },
            )
        }
    }
}

data class VurderingRecord(
    val alternativ: String?,
    val fritekst: String?,
    val veilederident: String,
)

class KartleggingssporsmalKandidatV2RecordSerializer : Serializer<KartleggingssporsmalKandidatStatusV2Record> {
    private val mapper = configuredJacksonMapper()
    override fun serialize(topic: String?, data: KartleggingssporsmalKandidatStatusV2Record?): ByteArray =
        mapper.writeValueAsBytes(data)
}
