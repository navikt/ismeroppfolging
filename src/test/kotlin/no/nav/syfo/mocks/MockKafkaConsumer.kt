package no.nav.syfo.mocks

import io.mockk.every
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import java.time.Duration

fun <ConsumerRecordValue> KafkaConsumer<String, ConsumerRecordValue>.mockPollConsumerRecords(
    records: List<Pair<String, ConsumerRecordValue>>,
    topic: String = "topic",
) {
    val topicPartition = TopicPartition(
        topic,
        0
    )
    val consumerRecordList = records.mapIndexed { index, pair ->
        ConsumerRecord(
            topic,
            0,
            index.toLong(),
            pair.first,
            pair.second,
        )
    }
    every { this@mockPollConsumerRecords.poll(any<Duration>()) } returns ConsumerRecords(mapOf(topicPartition to consumerRecordList))
}
