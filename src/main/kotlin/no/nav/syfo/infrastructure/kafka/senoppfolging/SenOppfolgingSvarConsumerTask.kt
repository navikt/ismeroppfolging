package no.nav.syfo.infrastructure.kafka.senoppfolging

import no.nav.syfo.ApplicationState
import no.nav.syfo.application.SenOppfolgingService
import no.nav.syfo.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.infrastructure.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.infrastructure.kafka.launchKafkaConsumer
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.serialization.Deserializer

const val SENOPPFOLGING_SVAR_TOPIC =
    "team-esyfo.sen-oppfolging-svar"

fun launchSenOppfolgingSvarConsumer(applicationState: ApplicationState, kafkaEnvironment: KafkaEnvironment) {
    launchKafkaConsumer(
        applicationState = applicationState,
        topic = SENOPPFOLGING_SVAR_TOPIC,
        consumerProperties = kafkaAivenConsumerConfig<SenOppfolgingSvarDeserializer>(
            kafkaEnvironment = kafkaEnvironment,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
        ),
        kafkaConsumerService = SenOppfolgingSvarConsumer(senOppfolgingService = SenOppfolgingService())
    )
}

class SenOppfolgingSvarDeserializer : Deserializer<SenOppfolgingSvarRecord> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): SenOppfolgingSvarRecord =
        mapper.readValue(data, SenOppfolgingSvarRecord::class.java)
}
