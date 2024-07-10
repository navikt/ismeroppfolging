package no.nav.syfo.infrastructure.database.repository

import no.nav.syfo.domain.SenOppfolgingStatus
import no.nav.syfo.domain.SenOppfolgingVurdering
import java.time.OffsetDateTime
import java.util.*

data class PSenOppfolgingVurdering(
    val id: Int,
    val uuid: UUID,
    val kandidatId: Int,
    val createdAt: OffsetDateTime,
    val veilederident: String,
    val status: String,
    val publishedAt: OffsetDateTime?,
) {
    fun toSenOppfolgingVurdering() = SenOppfolgingVurdering.createFromDatabase(
        uuid = uuid,
        createdAt = createdAt,
        veilederident = veilederident,
        status = SenOppfolgingStatus.valueOf(status),
    )
}
