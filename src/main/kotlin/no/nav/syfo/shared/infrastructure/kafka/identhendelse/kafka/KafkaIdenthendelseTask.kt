package no.nav.syfo.shared.infrastructure.kafka.identhendelse.kafka

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.syfo.ApplicationState
import no.nav.syfo.senoppfolging.application.ISenOppfolgingRepository
import no.nav.syfo.shared.infrastructure.kafka.identhendelse.IdenthendelseService
import no.nav.syfo.shared.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.shared.infrastructure.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.shared.infrastructure.kafka.launchKafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import java.util.Properties

const val PDL_AKTOR_TOPIC = "pdl.aktor-v2"

fun launchKafkaTaskIdenthendelse(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
    senOppfolgingRepository: ISenOppfolgingRepository,
) {
    val identhendelseService = IdenthendelseService(
        senOppfolgingRepository = senOppfolgingRepository,
    )

    val kafkaIdenthendelseConsumerService = IdenthendelseConsumerService(
        identhendelseService = identhendelseService,
    )

    val consumerProperties = Properties().apply {
        putAll(kafkaAivenConsumerConfig<KafkaAvroDeserializer>(kafkaEnvironment, OffsetResetStrategy.EARLIEST))
        this[MAX_POLL_RECORDS_CONFIG] = "1000"
        this[KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = kafkaEnvironment.aivenSchemaRegistryUrl
        this[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = false
        this[KafkaAvroDeserializerConfig.USER_INFO_CONFIG] =
            "${kafkaEnvironment.aivenRegistryUser}:${kafkaEnvironment.aivenRegistryPassword}"
        this[KafkaAvroDeserializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE] = "USER_INFO"
    }

    launchKafkaConsumer(
        applicationState = applicationState,
        topic = PDL_AKTOR_TOPIC,
        consumerProperties = consumerProperties,
        kafkaConsumerService = kafkaIdenthendelseConsumerService,
    )
}
