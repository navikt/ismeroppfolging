package no.nav.syfo.infrastructure.kafka.senoppfolging

import no.nav.syfo.application.SenOppfolgingService
import no.nav.syfo.domain.Personident
import no.nav.syfo.infrastructure.kafka.KafkaConsumerService
import no.nav.syfo.util.toOffsetDateTimeUTC
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.OffsetDateTime

class SenOppfolgingSvarConsumer(private val senOppfolgingService: SenOppfolgingService) : KafkaConsumerService<SenOppfolgingSvarRecord> {

    override val pollDurationInMillis: Long = 1000

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, SenOppfolgingSvarRecord>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            records.requireNoNulls().forEach { record ->
                val senOppfolgingSvarRecord = record.value()
                log.info("Received record: $senOppfolgingSvarRecord")
                processRecord(senOppfolgingSvarRecord = senOppfolgingSvarRecord)
            }
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecord(senOppfolgingSvarRecord: SenOppfolgingSvarRecord) {
        // TODO: createKandidat should be moved to varsel consumer
        val kandidat = senOppfolgingService.createKandidat(
            personident = Personident(senOppfolgingSvarRecord.personIdent),
            varselAt = OffsetDateTime.now()
        )

        // TODO: Should get kandidat based on varsel associated with svar, then add svar to that kandidat
        val kandidatMedSvar = senOppfolgingService.addSvar(
            kandidat = kandidat,
            svarAt = senOppfolgingSvarRecord.createdAt.toOffsetDateTimeUTC(),
            onskerOppfolging = senOppfolgingSvarRecord.response.toOnskerOppfolging()
        )

        log.info("Created kandidat med svar: $kandidatMedSvar")
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
