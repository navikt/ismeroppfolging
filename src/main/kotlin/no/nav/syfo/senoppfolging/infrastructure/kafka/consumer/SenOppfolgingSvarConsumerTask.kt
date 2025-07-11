package no.nav.syfo.senoppfolging.infrastructure.kafka.consumer

import no.nav.syfo.ApplicationState
import no.nav.syfo.senoppfolging.application.SenOppfolgingService
import no.nav.syfo.shared.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.shared.infrastructure.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.shared.infrastructure.kafka.launchKafkaConsumer
import no.nav.syfo.shared.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.serialization.Deserializer

const val SENOPPFOLGING_SVAR_TOPIC =
    "team-esyfo.sen-oppfolging-svar"

fun launchSenOppfolgingSvarConsumer(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
    senOppfolgingService: SenOppfolgingService
) {
    val senOppfolgingSvarConsumer = SenOppfolgingSvarConsumer(senOppfolgingService = senOppfolgingService)
    launchKafkaConsumer(
        applicationState = applicationState,
        topic = SENOPPFOLGING_SVAR_TOPIC,
        consumerProperties = kafkaAivenConsumerConfig<SenOppfolgingSvarDeserializer>(
            kafkaEnvironment = kafkaEnvironment,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
        ),
        kafkaConsumerService = senOppfolgingSvarConsumer
    )
}

class SenOppfolgingSvarDeserializer : Deserializer<SenOppfolgingSvarRecord> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): SenOppfolgingSvarRecord =
        mapper.readValue(data, SenOppfolgingSvarRecord::class.java)
}
