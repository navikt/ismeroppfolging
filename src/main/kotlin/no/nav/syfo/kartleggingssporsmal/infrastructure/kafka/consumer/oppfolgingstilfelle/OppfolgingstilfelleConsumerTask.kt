package no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.consumer.oppfolgingstilfelle

import no.nav.syfo.ApplicationState
import no.nav.syfo.kartleggingssporsmal.application.KartleggingssporsmalService
import no.nav.syfo.shared.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.shared.infrastructure.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.shared.infrastructure.kafka.launchKafkaConsumer
import no.nav.syfo.shared.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.serialization.Deserializer

const val OPPFOLGINGSTILFELLE_PERSON_TOPIC =
    "teamsykefravr.isoppfolgingstilfelle-oppfolgingstilfelle-person"

fun launchOppfolgingstilfelleConsumer(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
    kartleggingssporsmalService: KartleggingssporsmalService,
) {
    val consumerProperties = kafkaAivenConsumerConfig<KafkaOppfolgingstilfellePersonDeserializer>(
        kafkaEnvironment = kafkaEnvironment,
        offsetResetStrategy = OffsetResetStrategy.LATEST,
    )
    consumerProperties.apply {
        this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "100"
    }

    val oppfolgingstilfelleConsumer = OppfolgingstilfelleConsumer(kartleggingssporsmalService)

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
