package no.nav.syfo.domain

import no.nav.syfo.util.nowUTC
import java.time.OffsetDateTime
import java.util.UUID

data class SenOppfolgingVurdering(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val veilederident: String,
    val type: VurderingType,
    val publishedAt: OffsetDateTime?,
) {
    constructor(
        veilederident: String,
        type: VurderingType,
    ) : this(
        uuid = UUID.randomUUID(),
        createdAt = nowUTC(),
        veilederident = veilederident,
        type = type,
        publishedAt = null,
    )

    companion object {
        fun createFromDatabase(
            uuid: UUID,
            createdAt: OffsetDateTime,
            veilederident: String,
            type: VurderingType,
            publishedAt: OffsetDateTime?,
        ) = SenOppfolgingVurdering(
            uuid = uuid,
            createdAt = createdAt,
            veilederident = veilederident,
            type = type,
            publishedAt = publishedAt,
        )
    }
}

enum class VurderingType {
    FERDIGBEHANDLET,
}
