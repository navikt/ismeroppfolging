package no.nav.syfo.infrastructure.kafka.senoppfolging

import io.micrometer.core.instrument.Counter
import no.nav.syfo.application.SenOppfolgingService
import no.nav.syfo.domain.Personident
import no.nav.syfo.infrastructure.kafka.KafkaConsumerService
import no.nav.syfo.infrastructure.metric.METRICS_NS
import no.nav.syfo.infrastructure.metric.METRICS_REGISTRY
import no.nav.syfo.util.toOffsetDateTimeUTC
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class SenOppfolgingVarselConsumer(private val senOppfolgingService: SenOppfolgingService) :
    KafkaConsumerService<KSenOppfolgingVarselDTO> {

    override val pollDurationInMillis: Long = 1000

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KSenOppfolgingVarselDTO>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            records.requireNoNulls().forEach { record ->
                val senOppfolgingVarselRecord = record.value()
                log.info("Received sen oppfolging varsel record with id: ${senOppfolgingVarselRecord.uuid}")
                VarselMetrics.COUNT_KAFKA_CONSUMER_SEN_OPPFOLGING_VARSEL_READ.increment()
                processRecord(senOppfolgingVarselRecord = senOppfolgingVarselRecord)
            }
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecord(senOppfolgingVarselRecord: KSenOppfolgingVarselDTO) {
        val existing = senOppfolgingService.findKandidatFromVarselId(senOppfolgingVarselRecord.uuid)
        if (existing == null) {
            senOppfolgingService.createKandidat(
                personident = Personident(senOppfolgingVarselRecord.fnr),
                varselAt = senOppfolgingVarselRecord.createdAt.toOffsetDateTimeUTC(),
                varselId = senOppfolgingVarselRecord.uuid,
            )
            VarselMetrics.COUNT_KAFKA_CONSUMER_SEN_OPPFOLGING_VARSEL_KANDIDAT_CREATED.increment()
        } else {
            log.warn("Received duplicate oppfolgingvarsel: ${senOppfolgingVarselRecord.uuid}")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}

private object VarselMetrics {
    private const val KAFKA_CONSUMER_SEN_OPPFOLGING_VARSEL_BASE = "${METRICS_NS}_kafka_consumer_sen_oppfolging_varsel"

    val COUNT_KAFKA_CONSUMER_SEN_OPPFOLGING_VARSEL_READ: Counter = Counter
        .builder("${KAFKA_CONSUMER_SEN_OPPFOLGING_VARSEL_BASE}_read_count")
        .description("Counts the number of reads from topic $SENOPPFOLGING_VARSEL_TOPIC")
        .register(METRICS_REGISTRY)
    val COUNT_KAFKA_CONSUMER_SEN_OPPFOLGING_VARSEL_KANDIDAT_CREATED: Counter = Counter
        .builder("${KAFKA_CONSUMER_SEN_OPPFOLGING_VARSEL_BASE}_kandidat_created_count")
        .description("Counts the number of kandidater created from topic $SENOPPFOLGING_VARSEL_TOPIC")
        .register(METRICS_REGISTRY)
}
