package no.nav.syfo.senoppfolging.generators

import no.nav.syfo.UserConstants
import no.nav.syfo.senoppfolging.infrastructure.kafka.consumer.SenOppfolgingQuestion
import no.nav.syfo.senoppfolging.infrastructure.kafka.consumer.SenOppfolgingSvarRecord
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
