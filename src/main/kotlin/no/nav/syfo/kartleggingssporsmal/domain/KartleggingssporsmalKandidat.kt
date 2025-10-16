package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.util.nowUTC
import java.time.OffsetDateTime
import java.util.*

data class KartleggingssporsmalKandidat private constructor(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val personident: Personident,
    val status: KandidatStatus,
    val publishedAt: OffsetDateTime?,
    val varsletAt: OffsetDateTime?,
    val journalpostId: JournalpostId? = null,
) {
    constructor(
        personident: Personident,
        status: KandidatStatus,
    ) : this(
        uuid = UUID.randomUUID(),
        createdAt = nowUTC(),
        personident = personident,
        status = status,
        publishedAt = null,
        varsletAt = null,
    )

    companion object {
        fun createFromDatabase(
            uuid: UUID,
            createdAt: OffsetDateTime,
            personident: Personident,
            status: String,
            publishedAt: OffsetDateTime?,
            varsletAt: OffsetDateTime?,
            journalpostId: JournalpostId?
        ) = KartleggingssporsmalKandidat(
            uuid = uuid,
            createdAt = createdAt,
            personident = personident,
            status = KandidatStatus.valueOf(status),
            publishedAt = publishedAt,
            varsletAt = varsletAt,
            journalpostId = journalpostId,
        )
    }
}

enum class KandidatStatus {
    KANDIDAT, IKKE_KANDIDAT,
}
