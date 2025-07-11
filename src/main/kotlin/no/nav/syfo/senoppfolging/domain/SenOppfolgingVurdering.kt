package no.nav.syfo.senoppfolging.domain

import no.nav.syfo.shared.util.nowUTC
import java.time.OffsetDateTime
import java.util.*

data class SenOppfolgingVurdering private constructor(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val veilederident: String,
    val begrunnelse: String?,
    val type: VurderingType,
    val publishedAt: OffsetDateTime?,
) {
    constructor(
        veilederident: String,
        begrunnelse: String?,
        type: VurderingType,
    ) : this(
        uuid = UUID.randomUUID(),
        createdAt = nowUTC(),
        veilederident = veilederident,
        begrunnelse = begrunnelse,
        type = type,
        publishedAt = null,
    )

    companion object {
        fun createFromDatabase(
            uuid: UUID,
            createdAt: OffsetDateTime,
            veilederident: String,
            begrunnelse: String,
            type: VurderingType,
            publishedAt: OffsetDateTime?,
        ) = SenOppfolgingVurdering(
            uuid = uuid,
            createdAt = createdAt,
            veilederident = veilederident,
            begrunnelse = begrunnelse,
            type = type,
            publishedAt = publishedAt,
        )
    }
}

enum class VurderingType {
    FERDIGBEHANDLET,
}
