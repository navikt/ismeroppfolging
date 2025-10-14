package no.nav.syfo.kartleggingssporsmal.infrastructure.database

import no.nav.syfo.kartleggingssporsmal.domain.JournalpostId
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.shared.domain.Personident
import java.time.OffsetDateTime
import java.util.*

data class PKartleggingssporsmalKandidat(
    val id: Int,
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val personident: Personident,
    val generatedByStoppunktId: Int,
    val status: String,
    val publishedAt: OffsetDateTime?,
    val varsletAt: OffsetDateTime?,
    val journalpostId: JournalpostId?,
) {
    fun toKartleggingssporsmalKandidat() = KartleggingssporsmalKandidat.createFromDatabase(
        uuid = uuid,
        createdAt = createdAt,
        personident = personident,
        status = status,
        publishedAt = publishedAt,
        varsletAt = varsletAt,
        journalpostId = journalpostId
    )
}
