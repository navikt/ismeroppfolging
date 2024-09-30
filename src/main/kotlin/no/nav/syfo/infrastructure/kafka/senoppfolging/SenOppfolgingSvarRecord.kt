package no.nav.syfo.infrastructure.kafka.senoppfolging

import no.nav.syfo.domain.OnskerOppfolging
import java.time.LocalDateTime
import java.util.*

data class SenOppfolgingSvarRecord(
    val id: UUID,
    val varselId: UUID? = null,
    val personIdent: String,
    val createdAt: LocalDateTime,
    val response: List<SenOppfolgingQuestion>,
)

data class SenOppfolgingQuestion(
    val questionType: String,
    val answerType: String,
)

enum class SenOppfolgingQuestionType {
    BEHOV_FOR_OPPFOLGING
}

enum class BehovForOppfolgingSvar {
    JA,
    NEI
}

fun List<SenOppfolgingQuestion>.toOnskerOppfolging(): OnskerOppfolging {
    val behovForOppfolgingQuestion = this.find { it.questionType == SenOppfolgingQuestionType.BEHOV_FOR_OPPFOLGING.name }
    return if (behovForOppfolgingQuestion?.answerType == BehovForOppfolgingSvar.JA.name) OnskerOppfolging.JA else OnskerOppfolging.NEI
}
