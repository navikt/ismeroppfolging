package no.nav.syfo.generators

import no.nav.syfo.UserConstants
import no.nav.syfo.infrastructure.kafka.senoppfolging.SenOppfolgingSvarRecord
import java.time.LocalDateTime
import java.util.*

fun generateSenOppfolgingSvarRecord(): SenOppfolgingSvarRecord = SenOppfolgingSvarRecord(
    id = UUID.randomUUID(),
    personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT.value,
    createdAt = LocalDateTime.now(),
    response = emptyList(),
)
