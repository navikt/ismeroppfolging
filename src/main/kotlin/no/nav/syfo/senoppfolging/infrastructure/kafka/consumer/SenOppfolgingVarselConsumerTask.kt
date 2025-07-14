package no.nav.syfo.senoppfolging.infrastructure.kafka.consumer

import no.nav.syfo.ApplicationState
import no.nav.syfo.senoppfolging.application.SenOppfolgingService
import no.nav.syfo.shared.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.shared.infrastructure.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.shared.infrastructure.kafka.launchKafkaConsumer
import no.nav.syfo.shared.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.serialization.Deserializer

const val SENOPPFOLGING_VARSEL_TOPIC = "team-esyfo.sen-oppfolging-varsel"

fun launchSenOppfolgingVarselConsumer(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
    senOppfolgingService: SenOppfolgingService
) {
    val senOppfolgingVarselConsumer = SenOppfolgingVarselConsumer(senOppfolgingService = senOppfolgingService)
    launchKafkaConsumer(
        applicationState = applicationState,
        topic = SENOPPFOLGING_VARSEL_TOPIC,
        consumerProperties = kafkaAivenConsumerConfig<SenOppfolgingVarselDeserializer>(
            kafkaEnvironment = kafkaEnvironment,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
        ),
        kafkaConsumerService = senOppfolgingVarselConsumer
    )
}

class SenOppfolgingVarselDeserializer : Deserializer<KSenOppfolgingVarselDTO> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): KSenOppfolgingVarselDTO =
        mapper.readValue(data, KSenOppfolgingVarselDTO::class.java)
}
