package no.nav.syfo.senoppfolging.generators

import no.nav.syfo.UserConstants
import no.nav.syfo.senoppfolging.infrastructure.kafka.consumer.KSenOppfolgingVarselDTO
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
