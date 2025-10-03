package no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.kartleggingssporsmalsvar

import io.micrometer.core.instrument.Counter
import no.nav.syfo.shared.infrastructure.kafka.KafkaConsumerService
import no.nav.syfo.shared.infrastructure.metric.METRICS_NS
import no.nav.syfo.shared.infrastructure.metric.METRICS_REGISTRY
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class KartleggingssporsmalSvarConsumer() : KafkaConsumerService<KafkaKartleggingssporsmalSvarDTO> {

    override val pollDurationInMillis: Long = 1000

    override suspend fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KafkaKartleggingssporsmalSvarDTO>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            records.requireNoNulls().forEach { record ->
                val svarRecord = record.value()
                log.info(
                    """
                    Received kartleggingssporsmal svar:
                    key: ${record.key()}
                    svarId: ${svarRecord.svarId}
                    """.trimIndent()
                )
                Metrics.COUNT_KAFKA_CONSUMER_KARTLEGGINGSSPORSMAL_SVAR_READ.increment()
                processRecord(svarRecord)
            }
            kafkaConsumer.commitSync()
        }
    }

    private suspend fun processRecord(svarRecordDTO: KafkaKartleggingssporsmalSvarDTO) {
        // TODO
        return
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}

private object Metrics {
    private const val KAFKA_CONSUMER_KARTLEGGINGSSPORSMAL_SVAR_BASE = "${METRICS_NS}_kafka_consumer_kartleggingssporsmal_svar"

    val COUNT_KAFKA_CONSUMER_KARTLEGGINGSSPORSMAL_SVAR_READ: Counter = Counter
        .builder("${KAFKA_CONSUMER_KARTLEGGINGSSPORSMAL_SVAR_BASE}_read_count")
        .description("Counts the number of reads from topic $KARTLEGGINGSSPORSMAL_SVAR_TOPIC")
        .register(METRICS_REGISTRY)
}
