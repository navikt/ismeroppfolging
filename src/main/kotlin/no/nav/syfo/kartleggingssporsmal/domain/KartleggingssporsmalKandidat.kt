package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.util.nowUTC
import java.time.OffsetDateTime
import java.util.*

data class KartleggingssporsmalKandidat private constructor(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val personident: Personident,
    val varsletAt: OffsetDateTime?,
    val svarAt: OffsetDateTime?,
    val journalpostId: JournalpostId? = null,
) {
    constructor(
        personident: Personident,
    ) : this(
        uuid = UUID.randomUUID(),
        createdAt = nowUTC(),
        personident = personident,
        varsletAt = null,
        svarAt = null,
    )

    fun addSvarAt(svarAt: OffsetDateTime): KartleggingssporsmalKandidat = this.copy(
        svarAt = svarAt,
    )

    companion object {
        fun createFromDatabase(
            uuid: UUID,
            createdAt: OffsetDateTime,
            personident: Personident,
            varsletAt: OffsetDateTime?,
            svarAt: OffsetDateTime?,
            journalpostId: JournalpostId?
        ) = KartleggingssporsmalKandidat(
            uuid = uuid,
            createdAt = createdAt,
            personident = personident,
            varsletAt = varsletAt,
            svarAt = svarAt,
            journalpostId = journalpostId,
        )
    }
}

enum class KandidatStatus {
    KANDIDAT, IKKE_KANDIDAT,
}
