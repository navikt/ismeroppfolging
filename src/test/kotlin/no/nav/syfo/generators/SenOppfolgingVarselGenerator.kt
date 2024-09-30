package no.nav.syfo.generators

import no.nav.syfo.UserConstants
import no.nav.syfo.infrastructure.kafka.senoppfolging.KSenOppfolgingVarselDTO
import java.time.LocalDateTime
import java.util.*

fun generateSenOppfolgingVarselRecord(
    personIdent: String = UserConstants.ARBEIDSTAKER_PERSONIDENT.value,
    varselId: UUID = UUID.randomUUID(),
): KSenOppfolgingVarselDTO = KSenOppfolgingVarselDTO(
    uuid = varselId,
    personident = personIdent,
    createdAt = LocalDateTime.now(),
)
