package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.shared.domain.Personident
import java.time.OffsetDateTime
import java.util.UUID

data class KartleggingssporsmalKandidat private constructor(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val personident: Personident,
    val status: KandidatStatus,
    val varsletAt: OffsetDateTime?,
) {
    constructor(
        personident: Personident,
        status: KandidatStatus,
    ) : this(
        uuid = UUID.randomUUID(),
        createdAt = OffsetDateTime.now(),
        personident = personident,
        status = status,
        varsletAt = null,
    )

    companion object {
        fun createFromDatabase(
            uuid: UUID,
            createdAt: OffsetDateTime,
            personident: Personident,
            status: String,
            varsletAt: OffsetDateTime?,
        ) = KartleggingssporsmalKandidat(
            uuid = uuid,
            createdAt = createdAt,
            personident = personident,
            status = KandidatStatus.valueOf(status),
            varsletAt = varsletAt,
        )
    }
}

enum class KandidatStatus {
    KANDIDAT, IKKE_KANDIDAT,
}
