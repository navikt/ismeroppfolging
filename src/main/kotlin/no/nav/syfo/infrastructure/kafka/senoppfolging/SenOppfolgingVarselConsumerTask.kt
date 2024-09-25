package no.nav.syfo.infrastructure.kafka.senoppfolging

import no.nav.syfo.ApplicationState
import no.nav.syfo.application.SenOppfolgingService
import no.nav.syfo.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.infrastructure.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.infrastructure.kafka.launchKafkaConsumer
import no.nav.syfo.util.configuredJacksonMapper
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
