package no.nav.syfo.senoppfolging.infrastructure.database.repository

import no.nav.syfo.senoppfolging.domain.SenOppfolgingVurdering
import no.nav.syfo.senoppfolging.domain.VurderingType
import java.time.OffsetDateTime
import java.util.*

data class PSenOppfolgingVurdering(
    val id: Int,
    val uuid: UUID,
    val kandidatId: Int,
    val createdAt: OffsetDateTime,
    val veilederident: String,
    val begrunnelse: String,
    val type: VurderingType,
    val publishedAt: OffsetDateTime?,
) {
    fun toSenOppfolgingVurdering() = SenOppfolgingVurdering.createFromDatabase(
        uuid = uuid,
        createdAt = createdAt,
        veilederident = veilederident,
        begrunnelse = begrunnelse,
        type = type,
        publishedAt = publishedAt,
    )
}
