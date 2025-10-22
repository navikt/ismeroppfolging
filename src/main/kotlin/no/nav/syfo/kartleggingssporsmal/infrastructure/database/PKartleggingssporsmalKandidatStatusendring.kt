package no.nav.syfo.kartleggingssporsmal.infrastructure.database

import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidatStatusendring
import java.time.OffsetDateTime
import java.util.*

data class PKartleggingssporsmalKandidatStatusendring(
    val id: Int,
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val kandidatId: Int,
    val status: String,
    val publishedAt: OffsetDateTime?,
    val svarAt: OffsetDateTime?,
    val veilederident: String?,
) {
    fun toKartleggingssporsmalKandidatStatusendring() = KartleggingssporsmalKandidatStatusendring.createFromDatabase(
        uuid = uuid,
        createdAt = createdAt,
        status = status,
        publishedAt = publishedAt,
        svarAt = svarAt,
        veilederident = veilederident,
    )
}
