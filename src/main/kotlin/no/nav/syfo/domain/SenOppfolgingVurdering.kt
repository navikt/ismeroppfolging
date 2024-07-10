package no.nav.syfo.domain

import no.nav.syfo.util.nowUTC
import java.time.OffsetDateTime
import java.util.UUID

data class SenOppfolgingVurdering(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val veilederident: String,
    val type: VurderingType,
) {
    constructor(
        veilederident: String,
        type: VurderingType,
    ) : this(
        uuid = UUID.randomUUID(),
        createdAt = nowUTC(),
        veilederident = veilederident,
        type = type,
    )

    companion object {
        fun createFromDatabase(
            uuid: UUID,
            createdAt: OffsetDateTime,
            veilederident: String,
            type: VurderingType,
        ) = SenOppfolgingVurdering(
            uuid = uuid,
            createdAt = createdAt,
            veilederident = veilederident,
            type = type,
        )
    }
}

enum class VurderingType {
    FERDIGBEHANDLET,
}
