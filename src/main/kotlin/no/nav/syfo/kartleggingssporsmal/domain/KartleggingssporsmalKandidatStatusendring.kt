package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.shared.util.nowUTC
import java.time.OffsetDateTime
import java.util.*

data class KartleggingssporsmalKandidatStatusendring private constructor(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val status: KandidatStatus,
    val publishedAt: OffsetDateTime?,
    val svarAt: OffsetDateTime?,
) {
    constructor(
        status: KandidatStatus,
    ) : this(
        uuid = UUID.randomUUID(),
        createdAt = nowUTC(),
        status = status,
        publishedAt = null,
        svarAt = null,
    )

    companion object {
        fun createFromDatabase(
            uuid: UUID,
            createdAt: OffsetDateTime,
            status: String,
            publishedAt: OffsetDateTime?,
            svarAt: OffsetDateTime?,
        ) = KartleggingssporsmalKandidatStatusendring(
            uuid = uuid,
            createdAt = createdAt,
            status = KandidatStatus.valueOf(status),
            publishedAt = publishedAt,
            svarAt = svarAt,
        )
    }
}
