package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.shared.util.nowUTC
import java.time.OffsetDateTime
import java.util.*

sealed class KartleggingssporsmalKandidatStatusendring(
    open val uuid: UUID,
    open val createdAt: OffsetDateTime,
    open val publishedAt: OffsetDateTime?,
) {
    abstract val status: KandidatStatus

    data class Kandidat internal constructor(
        override val uuid: UUID,
        override val createdAt: OffsetDateTime,
        override val publishedAt: OffsetDateTime?,
    ) : KartleggingssporsmalKandidatStatusendring(
        uuid,
        createdAt,
        publishedAt,
    ) {
        override val status: KandidatStatus = KandidatStatus.KANDIDAT

        constructor() : this(
            uuid = UUID.randomUUID(),
            createdAt = nowUTC(),
            publishedAt = null,
        )
    }

    data class SvarMottatt internal constructor(
        override val uuid: UUID,
        override val createdAt: OffsetDateTime,
        override val publishedAt: OffsetDateTime?,
        val svarAt: OffsetDateTime,
    ) : KartleggingssporsmalKandidatStatusendring(
        uuid,
        createdAt,
        publishedAt,
    ) {
        override val status: KandidatStatus = KandidatStatus.SVAR_MOTTATT

        constructor(
            svarAt: OffsetDateTime,
        ) : this(
            uuid = UUID.randomUUID(),
            createdAt = nowUTC(),
            publishedAt = null,
            svarAt = svarAt,
        )

    }

    data class Ferdigbehandlet internal constructor(
        override val uuid: UUID,
        override val createdAt: OffsetDateTime,
        override val publishedAt: OffsetDateTime?,
        val veilederident: String,
    ) : KartleggingssporsmalKandidatStatusendring(
        uuid = UUID.randomUUID(),
        createdAt = nowUTC(),
        publishedAt = null,
    ) {
        override val status: KandidatStatus = KandidatStatus.FERDIGBEHANDLET

        constructor(veilederident: String) : this(
            uuid = UUID.randomUUID(),
            createdAt = nowUTC(),
            publishedAt = null,
            veilederident = veilederident,
        )
    }

    companion object {
        fun createFromDatabase(
            uuid: UUID,
            createdAt: OffsetDateTime,
            status: String,
            publishedAt: OffsetDateTime?,
            svarAt: OffsetDateTime?,
            veilederident: String?,
        ) =
            when (status) {
                KandidatStatus.KANDIDAT.name ->
                    Kandidat(
                        uuid = uuid,
                        createdAt = createdAt,
                        publishedAt = publishedAt,
                    )
                KandidatStatus.SVAR_MOTTATT.name ->
                    SvarMottatt(
                        uuid = uuid,
                        createdAt = createdAt,
                        publishedAt = publishedAt,
                        svarAt = svarAt!!,
                    )
                KandidatStatus.FERDIGBEHANDLET.name ->
                    Ferdigbehandlet(
                        uuid = uuid,
                        createdAt = createdAt,
                        publishedAt = publishedAt,
                        veilederident = veilederident!!,
                    )
                else -> throw IllegalArgumentException("Ukjent statusendring: $status")
            }
    }
}
