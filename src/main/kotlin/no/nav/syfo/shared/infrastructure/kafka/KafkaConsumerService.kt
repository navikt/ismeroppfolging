package no.nav.syfo.shared.infrastructure.kafka

import org.apache.kafka.clients.consumer.KafkaConsumer

interface KafkaConsumerService<ConsumerRecordValue> {
    val pollDurationInMillis: Long
    suspend fun pollAndProcessRecords(
        kafkaConsumer: KafkaConsumer<String, ConsumerRecordValue>,
    )
}
