package no.nav.syfo.kartleggingssporsmal.infrastructure.database

import no.nav.syfo.shared.domain.Personident
import java.time.OffsetDateTime
import java.util.UUID

data class PKartleggingssporsmalKandidat(
    val id: Int,
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val personident: Personident,
    val generatedByStoppunktId: Int,
    val status: String,
    val varsletAt: OffsetDateTime?,
)
