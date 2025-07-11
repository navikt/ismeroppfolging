package no.nav.syfo.infrastructure.kafka.jobbforventning

import no.nav.syfo.ApplicationState
import no.nav.syfo.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.infrastructure.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.infrastructure.kafka.launchKafkaConsumer
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.serialization.Deserializer

const val OPPFOLGINGSTILFELLE_PERSON_TOPIC =
    "teamsykefravr.isoppfolgingstilfelle-oppfolgingstilfelle-person"

fun launchOppfolgingstilfelleConsumer(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
) {
    val consumerProperties = kafkaAivenConsumerConfig<KafkaOppfolgingstilfellePersonDeserializer>(
        kafkaEnvironment = kafkaEnvironment,
        offsetResetStrategy = OffsetResetStrategy.LATEST,
    )
    consumerProperties.apply {
        this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "100"
    }

    val oppfolgingstilfelleConsumer = OppfolgingstilfelleConsumer()

    launchKafkaConsumer(
        applicationState = applicationState,
        topic = OPPFOLGINGSTILFELLE_PERSON_TOPIC,
        consumerProperties = consumerProperties,
        kafkaConsumerService = oppfolgingstilfelleConsumer,
    )
}

class KafkaOppfolgingstilfellePersonDeserializer : Deserializer<KafkaOppfolgingstilfellePersonDTO> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): KafkaOppfolgingstilfellePersonDTO =
        mapper.readValue(data, KafkaOppfolgingstilfellePersonDTO::class.java)
}
