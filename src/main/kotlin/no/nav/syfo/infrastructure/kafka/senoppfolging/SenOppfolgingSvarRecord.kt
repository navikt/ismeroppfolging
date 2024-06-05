package no.nav.syfo.infrastructure.kafka.senoppfolging

import java.time.LocalDateTime
import java.util.*

data class SenOppfolgingSvarRecord(
    val id: UUID,
    val personIdent: String,
    val createdAt: LocalDateTime,
    val response: List<SenOppfolgingQuestion>,
)

data class SenOppfolgingQuestion(
    val questionType: String,
    val answerType: String,
)
