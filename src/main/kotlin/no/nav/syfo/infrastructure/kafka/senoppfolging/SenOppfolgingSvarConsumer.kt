package no.nav.syfo.infrastructure.kafka.senoppfolging

import no.nav.syfo.infrastructure.kafka.KafkaConsumerService
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class SenOppfolgingSvarConsumer : KafkaConsumerService<SenOppfolgingSvarRecord> {

    override val pollDurationInMillis: Long = 1000

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, SenOppfolgingSvarRecord>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            records.forEach { record ->
                log.info("Received record: ${record.value()}") // TODO: Lagre i database
            }
            kafkaConsumer.commitSync()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
