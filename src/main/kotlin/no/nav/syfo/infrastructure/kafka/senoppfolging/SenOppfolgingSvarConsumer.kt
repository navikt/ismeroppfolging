package no.nav.syfo.infrastructure.kafka.senoppfolging

import io.micrometer.core.instrument.Counter
import no.nav.syfo.application.SenOppfolgingService
import no.nav.syfo.domain.OnskerOppfolging
import no.nav.syfo.domain.Personident
import no.nav.syfo.infrastructure.kafka.KafkaConsumerService
import no.nav.syfo.infrastructure.metric.METRICS_NS
import no.nav.syfo.infrastructure.metric.METRICS_REGISTRY
import no.nav.syfo.util.toOffsetDateTimeUTC
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class SenOppfolgingSvarConsumer(private val senOppfolgingService: SenOppfolgingService) : KafkaConsumerService<SenOppfolgingSvarRecord> {

    override val pollDurationInMillis: Long = 1000

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, SenOppfolgingSvarRecord>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            records.requireNoNulls().forEach { record ->
                val senOppfolgingSvarRecord = record.value()
                log.info("Received sen oppfolging svar record with id: ${senOppfolgingSvarRecord.id}")
                Metrics.COUNT_KAFKA_CONSUMER_SEN_OPPFOLGING_SVAR_READ.increment()
                processRecord(senOppfolgingSvarRecord = senOppfolgingSvarRecord)
            }
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecord(senOppfolgingSvarRecord: SenOppfolgingSvarRecord) {
        val kandidatForVarsel = senOppfolgingSvarRecord.varselId?.let {
            senOppfolgingService.findKandidatFromVarselId(it)
        }
        val recentKandidat = senOppfolgingService.findRecentKandidatFromPersonIdent(
            personident = Personident(senOppfolgingSvarRecord.personIdent),
        )
        val kandidat = if (kandidatForVarsel != null) {
            kandidatForVarsel
        } else if (recentKandidat != null) {
            recentKandidat
        } else {
            senOppfolgingService.createKandidat(
                personident = Personident(senOppfolgingSvarRecord.personIdent),
            ).also {
                Metrics.COUNT_KAFKA_CONSUMER_SEN_OPPFOLGING_SVAR_KANDIDAT_CREATED.increment()
            }
        }

        val onskerOppfolging = senOppfolgingSvarRecord.response.toOnskerOppfolging()
        senOppfolgingService.addSvar(
            kandidat = kandidat,
            svarAt = senOppfolgingSvarRecord.createdAt.toOffsetDateTimeUTC(),
            onskerOppfolging = onskerOppfolging
        )
        when (onskerOppfolging) {
            OnskerOppfolging.JA -> Metrics.COUNT_KAFKA_CONSUMER_SEN_OPPFOLGING_SVAR_KANDIDAT_ONSKER_OPPFOLGING.increment()
            OnskerOppfolging.NEI -> Metrics.COUNT_KAFKA_CONSUMER_SEN_OPPFOLGING_SVAR_KANDIDAT_ONSKER_IKKE_OPPFOLGING.increment()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}

private object Metrics {
    private const val KAFKA_CONSUMER_SEN_OPPFOLGING_SVAR_BASE = "${METRICS_NS}_kafka_consumer_sen_oppfolging_svar"

    val COUNT_KAFKA_CONSUMER_SEN_OPPFOLGING_SVAR_READ: Counter = Counter
        .builder("${KAFKA_CONSUMER_SEN_OPPFOLGING_SVAR_BASE}_read_count")
        .description("Counts the number of reads from topic $SENOPPFOLGING_SVAR_TOPIC")
        .register(METRICS_REGISTRY)
    val COUNT_KAFKA_CONSUMER_SEN_OPPFOLGING_SVAR_KANDIDAT_CREATED: Counter = Counter
        .builder("${KAFKA_CONSUMER_SEN_OPPFOLGING_SVAR_BASE}_kandidat_created_count")
        .description("Counts the number of kandidater created from topic $SENOPPFOLGING_SVAR_TOPIC")
        .register(METRICS_REGISTRY)
    val COUNT_KAFKA_CONSUMER_SEN_OPPFOLGING_SVAR_KANDIDAT_ONSKER_OPPFOLGING: Counter = Counter
        .builder("${KAFKA_CONSUMER_SEN_OPPFOLGING_SVAR_BASE}_kandidat_onsker_oppfolging_count")
        .description("Counts the number of kandidater onsker oppfolging from topic $SENOPPFOLGING_SVAR_TOPIC")
        .register(METRICS_REGISTRY)
    val COUNT_KAFKA_CONSUMER_SEN_OPPFOLGING_SVAR_KANDIDAT_ONSKER_IKKE_OPPFOLGING: Counter = Counter
        .builder("${KAFKA_CONSUMER_SEN_OPPFOLGING_SVAR_BASE}_kandidat_onsker_ikke_oppfolging_count")
        .description("Counts the number of kandidater onsker ikke oppfolging from topic $SENOPPFOLGING_SVAR_TOPIC")
        .register(METRICS_REGISTRY)
}
