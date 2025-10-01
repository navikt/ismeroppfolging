package no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.consumer.oppfolgingstilfelle

import no.nav.syfo.kartleggingssporsmal.domain.OppfolgingstilfelleDTO
import java.time.LocalDate
import java.time.OffsetDateTime

data class KafkaOppfolgingstilfellePersonDTO(
    val uuid: String,
    val createdAt: OffsetDateTime,
    val personIdentNumber: String,
    val oppfolgingstilfelleList: List<OppfolgingstilfelleDTO>,
    val referanseTilfelleBitUuid: String,
    val dodsdato: LocalDate?,
)
