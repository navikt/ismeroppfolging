package no.nav.syfo.infrastructure.kafka.senoppfolging

import java.time.LocalDateTime
import java.util.*

data class KSenOppfolgingVarselDTO(
    val uuid: UUID,
    val fnr: String,
    val createdAt: LocalDateTime,
)
