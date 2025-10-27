package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.util.nowUTC
import java.time.OffsetDateTime
import java.util.*

// Refaktorering av domenet
//
// status: KartleggingssporsmalKandidatStatusendring
// - Får ikke en komplett representasjon av domenet uten.
//
// Når denne kobles på må også dette representeres i databasen.
//

/**
 * KartleggingssporsmalKandidat representerer en kandidat for kartleggingsspørsmål.
 *
 * Se også [KartleggingssporsmalKandidatStatusendring] for å se statusendringer over tid knyttet til kandidaten.
 *
 * @property status forteller om nåværende status for kandidaten
 * @property varsletAt tidspunktet kandidaten fikk tilsendt kartleggingsspørsmål
 * @property journalpostId er id'en fra journalposten som ble opprettet når kartleggingsspørsmål ble sendt
 */
data class KartleggingssporsmalKandidat(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val personident: Personident,
    val status: KartleggingssporsmalKandidatStatusendring,
    val varsletAt: OffsetDateTime?,
    val journalpostId: JournalpostId? = null,
) {

    fun registrerSvarMottatt(svarAt: OffsetDateTime) =
        this.copy(
            status = KartleggingssporsmalKandidatStatusendring.SvarMottatt(svarAt = svarAt),
        )

    fun ferdigbehandleVurdering(veilederident: String): KartleggingssporsmalKandidat {
        if (this.status !is KartleggingssporsmalKandidatStatusendring.SvarMottatt) {
            throw IllegalArgumentException("Kandidat er allerede ferdig behandlet, eller har ikke mottatt svar enda")
        }
        return this.copy(status = KartleggingssporsmalKandidatStatusendring.Ferdigbehandlet(veilederident = veilederident))
    }

    companion object {

        fun create(
            personident: Personident,
        ) = KartleggingssporsmalKandidat(
            uuid = UUID.randomUUID(),
            createdAt = nowUTC(),
            personident = personident,
            status = KartleggingssporsmalKandidatStatusendring.Kandidat(),
            varsletAt = null,
            journalpostId = null,
        )

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
            status =
                when (KandidatStatus.valueOf(status)) {
                    KandidatStatus.KANDIDAT -> KartleggingssporsmalKandidatStatusendring.Kandidat()
                    KandidatStatus.SVAR_MOTTATT -> KartleggingssporsmalKandidatStatusendring.SvarMottatt(
                        svarAt = varsletAt
                            ?: throw IllegalArgumentException("varsletAt kan ikke være null når status er SVAR_MOTTATT")
                    )
                    KandidatStatus.FERDIGBEHANDLET -> KartleggingssporsmalKandidatStatusendring.Ferdigbehandlet(
                        veilederident = "UNKNOWN",
                    )
                },
            varsletAt = varsletAt,
            journalpostId = journalpostId,
        )
    }
}

enum class KandidatStatus {
    KANDIDAT, SVAR_MOTTATT, FERDIGBEHANDLET,
}
