package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.util.nowUTC
import java.time.OffsetDateTime
import java.util.*

/**
 * KartleggingssporsmalKandidat representerer en kandidat for kartleggingsspørsmål.
 *
 * Se også [KartleggingssporsmalKandidatStatusendring] for å se statusendringer over tid knyttet til kandidaten.
 *
 * @property status forteller om nåværende status for kandidaten
 * @property varsletAt tidspunktet kandidaten fikk tilsendt kartleggingsspørsmål
 * @property journalpostId er id'en fra journalposten som ble opprettet når kartleggingsspørsmål ble sendt
 */
data class KartleggingssporsmalKandidat private constructor(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val personident: Personident,
    val status: KandidatStatus,
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
        varsletAt = null,
    )

    fun registrerStatusEndring(statusEndring: KartleggingssporsmalKandidatStatusendring) =
        this.copy(status = statusEndring.status)

    companion object {
        fun createFromDatabase(
            uuid: UUID,
            createdAt: OffsetDateTime,
            personident: Personident,
            status: String,
            varsletAt: OffsetDateTime?,
            journalpostId: JournalpostId?,
        ) = KartleggingssporsmalKandidat(
            uuid = uuid,
            createdAt = createdAt,
            personident = personident,
            status = KandidatStatus.valueOf(status),
            varsletAt = varsletAt,
            journalpostId = journalpostId,
        )
    }
}

enum class KandidatStatus {
    KANDIDAT, SVAR_MOTTATT, FERDIG_BEHANDLET
}
