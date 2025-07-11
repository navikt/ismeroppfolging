package no.nav.syfo.senoppfolging.infrastructure.kafka.consumer

import java.time.LocalDateTime
import java.util.*

data class KSenOppfolgingVarselDTO(
    val uuid: UUID,
    val personident: String,
    val createdAt: LocalDateTime,
)
