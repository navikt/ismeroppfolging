package no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.kartleggingssporsmalsvar

import no.nav.syfo.ApplicationState
import no.nav.syfo.shared.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.shared.infrastructure.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.shared.infrastructure.kafka.launchKafkaConsumer
import no.nav.syfo.shared.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.serialization.Deserializer

const val KARTLEGGINGSSPORSMAL_SVAR_TOPIC =
    "team-esyfo.kartleggingssporsmal-svar"

fun launchKartleggingssporsmalSvarConsumer(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
) {
    val consumerProperties = kafkaAivenConsumerConfig<KafkaKartleggingssporsmalSvarDTODeserializer>(
        kafkaEnvironment = kafkaEnvironment,
        offsetResetStrategy = OffsetResetStrategy.EARLIEST,
    )
    consumerProperties.apply {
        this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "100"
    }

    val kartleggingssporsmalSvarConsumer = KartleggingssporsmalSvarConsumer()

    launchKafkaConsumer(
        applicationState = applicationState,
        topic = KARTLEGGINGSSPORSMAL_SVAR_TOPIC,
        consumerProperties = consumerProperties,
        kafkaConsumerService = kartleggingssporsmalSvarConsumer,
    )
}

class KafkaKartleggingssporsmalSvarDTODeserializer : Deserializer<KafkaKartleggingssporsmalSvarDTO> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): KafkaKartleggingssporsmalSvarDTO =
        mapper.readValue(data, KafkaKartleggingssporsmalSvarDTO::class.java)
}
