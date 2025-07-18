package no.nav.syfo.jobbforventning.infrastructure.kafka

import java.time.LocalDate
import java.time.OffsetDateTime

data class KafkaOppfolgingstilfellePersonDTO(
    val uuid: String,
    val createdAt: OffsetDateTime,
    val personIdentNumber: String,
    val oppfolgingstilfelleList: List<KafkaOppfolgingstilfelle>,
    val referanseTilfelleBitUuid: String,
    val referanseTilfelleBitInntruffet: OffsetDateTime,
    val dodsdato: LocalDate?,
)

data class KafkaOppfolgingstilfelle(
    val gradertAtTilfelleEnd: Boolean,
    val arbeidstakerAtTilfelleEnd: Boolean,
    val start: LocalDate,
    val end: LocalDate,
    val antallSykedager: Int?,
    val virksomhetsnummerList: List<String>,
)
