package no.nav.syfo.jobbforventning.infrastructure.kafka

import io.micrometer.core.instrument.Counter
import no.nav.syfo.shared.infrastructure.kafka.KafkaConsumerService
import no.nav.syfo.shared.infrastructure.metric.METRICS_NS
import no.nav.syfo.shared.infrastructure.metric.METRICS_REGISTRY
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class OppfolgingstilfelleConsumer() : KafkaConsumerService<KafkaOppfolgingstilfellePersonDTO> {

    override val pollDurationInMillis: Long = 1000

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KafkaOppfolgingstilfellePersonDTO>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            records.requireNoNulls().forEach { record ->
                val oppfolgingstilfelleRecord = record.value()
                log.info(
                    """
                    Received oppfolgingstilfelle:
                    key: ${record.key()}
                    tilfelle_uuid: ${oppfolgingstilfelleRecord.uuid}
                    """.trimIndent()
                )
                Metrics.COUNT_KAFKA_CONSUMER_OPPFOLGINGSTILFELLE_READ.increment()
                processRecord(oppfolgingstilfelleRecord)
            }
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecord(oppfolgingstilfellePersonDTO: KafkaOppfolgingstilfellePersonDTO) {
        // TODO: Implement processing
        return
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}

private object Metrics {
    private const val KAFKA_CONSUMER_OPPFOLGINGSTILFELLE_BASE = "${METRICS_NS}_kafka_consumer_oppfolgingstilfelle"

    val COUNT_KAFKA_CONSUMER_OPPFOLGINGSTILFELLE_READ: Counter = Counter
        .builder("${KAFKA_CONSUMER_OPPFOLGINGSTILFELLE_BASE}_read_count")
        .description("Counts the number of reads from topic $OPPFOLGINGSTILFELLE_PERSON_TOPIC")
        .register(METRICS_REGISTRY)
}
