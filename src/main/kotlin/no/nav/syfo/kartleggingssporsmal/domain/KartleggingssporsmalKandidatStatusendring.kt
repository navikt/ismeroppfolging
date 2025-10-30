package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.shared.util.nowUTC
import java.time.OffsetDateTime
import java.util.*

/**
 * KartleggingssporsmalKandidatStatusendring representerer en status for en kandidat til kartleggingsspørsmål [KartleggingssporsmalKandidat].
 *
 * Det finnes tre typer statusendringer:
 * - [Kandidat]: Når en person blir opprettet som kandidat til å motta kartleggingsspørsmål.
 * - [SvarMottatt]: Når kandidaten har sendt inn svar på kartleggingsspørsmålene.
 * - [Ferdigbehandlet]: Når en veileder har ferdigbehandlet kandidatens svar.
 *
 * Felles felt:
 * @property uuid Unik identifikator for statusendringen.
 * @property createdAt Tidspunktet statusendringen ble opprettet.
 * @property publishedAt Tidspunktet statusendringen ble publisert på event systemet.
 */
sealed class KartleggingssporsmalKandidatStatusendring(
    open val uuid: UUID,
    open val createdAt: OffsetDateTime,
    open val publishedAt: OffsetDateTime?,
) {
    abstract val kandidatStatus: KandidatStatus

    data class Kandidat internal constructor(
        override val uuid: UUID,
        override val createdAt: OffsetDateTime,
        override val publishedAt: OffsetDateTime?,
    ) : KartleggingssporsmalKandidatStatusendring(
        uuid,
        createdAt,
        publishedAt,
    ) {
        override val kandidatStatus: KandidatStatus = KandidatStatus.KANDIDAT

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
        override val kandidatStatus: KandidatStatus = KandidatStatus.SVAR_MOTTATT

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
        uuid,
        createdAt,
        publishedAt,
    ) {
        override val kandidatStatus: KandidatStatus = KandidatStatus.FERDIGBEHANDLET

        constructor(veilederident: String) : this(
            uuid = UUID.randomUUID(),
            createdAt = nowUTC(),
            publishedAt = null,
            veilederident = veilederident,
        )
    }
}
