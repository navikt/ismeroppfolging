package no.nav.syfo.infrastructure.database.repository

import java.time.OffsetDateTime
import java.util.*

data class PSenOppfolgingVurdering(
    val id: Int,
    val uuid: UUID,
    val kandidatId: Int,
    val createdAt: OffsetDateTime,
    val createdBy: String,
    val status: String,
    val publishedAt: OffsetDateTime?,
)
