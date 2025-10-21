package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.shared.util.nowUTC
import java.time.OffsetDateTime
import java.util.*

class KartleggingssporsmalKandidatStatusendring private constructor(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val status: KandidatStatus,
    val publishedAt: OffsetDateTime?,
    val svarAt: OffsetDateTime?,
    val veilederident: String?,
) {
    constructor(
        status: KandidatStatus,
        svarAt: OffsetDateTime? = null,
        veilederident: String? = null,
    ) : this(
        uuid = UUID.randomUUID(),
        createdAt = nowUTC(),
        status = status,
        publishedAt = null,
        svarAt = svarAt,
        veilederident = veilederident,
    )

    companion object {
        fun createFromDatabase(
            uuid: UUID,
            createdAt: OffsetDateTime,
            status: String,
            publishedAt: OffsetDateTime?,
            svarAt: OffsetDateTime?,
            veilederident: String?
        ) = KartleggingssporsmalKandidatStatusendring(
            uuid = uuid,
            createdAt = createdAt,
            status = KandidatStatus.valueOf(status),
            publishedAt = publishedAt,
            svarAt = svarAt,
            veilederident = veilederident,
        )
    }
}
