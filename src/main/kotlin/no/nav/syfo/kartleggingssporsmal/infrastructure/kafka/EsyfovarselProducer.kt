package no.nav.syfo.kartleggingssporsmal.infrastructure.kafka

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.util.*

class EsyfovarselProducer(
    private val kafkaProducer: KafkaProducer<String, EsyfovarselHendelse>,
) {

    /**
     * Sender kartleggingssporsmal varsel til esyfovarsel topic.
     *
     * NB: Sjekk at detaljer stemmer i `esyfovarsel` før producer taes i bruk.
     */
    fun sendKartleggingssporsmal(kartleggingssporsmalKandidat: KartleggingssporsmalKandidat): Result<KartleggingssporsmalKandidat> {
        val varselHendelse = ArbeidstakerHendelse(
            type = EsyfovarselHendelse.HendelseType.SM_KARTLEGGINGSSPORSMAL,
            arbeidstakerFnr = kartleggingssporsmalKandidat.personident.value,
        )
        val personidentUuid = UUID.nameUUIDFromBytes(kartleggingssporsmalKandidat.personident.value.toByteArray())

        return try {
            kafkaProducer.send(
                ProducerRecord(
                    ESYFOVARSEL_TOPIC,
                    personidentUuid.toString(),
                    varselHendelse,
                )
            ).get()
            Result.success(kartleggingssporsmalKandidat)
        } catch (e: Exception) {
            log.error("Exception was thrown when attempting to send kartleggingssporsmal varsel (uuid: ${kartleggingssporsmalKandidat.uuid}) to esyfovarsel: ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private const val ESYFOVARSEL_TOPIC = "team-esyfo.varselbus"
        private val log = LoggerFactory.getLogger(EsyfovarselProducer::class.java)
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
sealed interface EsyfovarselHendelse : Serializable {
    val type: HendelseType

    enum class HendelseType {
        SM_KARTLEGGINGSSPORSMAL,
    }
}

data class ArbeidstakerHendelse(
    override val type: EsyfovarselHendelse.HendelseType,
    val arbeidstakerFnr: String,
) : EsyfovarselHendelse
