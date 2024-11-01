package no.nav.syfo.generators

import no.nav.syfo.UserConstants
import no.nav.syfo.infrastructure.kafka.senoppfolging.SenOppfolgingQuestion
import no.nav.syfo.infrastructure.kafka.senoppfolging.SenOppfolgingSvarRecord
import java.time.LocalDateTime
import java.util.*

fun generateSenOppfolgingSvarRecord(
    personIdent: String = UserConstants.ARBEIDSTAKER_PERSONIDENT.value,
    question: SenOppfolgingQuestion,
    varselId: UUID,
): SenOppfolgingSvarRecord = SenOppfolgingSvarRecord(
    id = UUID.randomUUID(),
    personIdent = personIdent,
    createdAt = LocalDateTime.now(),
    response = listOf(question),
    varselId = varselId,
)
