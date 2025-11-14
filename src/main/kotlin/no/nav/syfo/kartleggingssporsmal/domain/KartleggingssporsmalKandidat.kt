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
data class KartleggingssporsmalKandidat(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val personident: Personident,
    val status: KartleggingssporsmalKandidatStatusendring,
    val varsletAt: OffsetDateTime?,
    val journalpostId: JournalpostId? = null,
) {

    fun registrerSvarMottatt(svarAt: OffsetDateTime): KartleggingssporsmalKandidat {
        if (this.status is KartleggingssporsmalKandidatStatusendring.Kandidat ||
            this.status is KartleggingssporsmalKandidatStatusendring.SvarMottatt
        ) {
            return this.copy(
                status = KartleggingssporsmalKandidatStatusendring.SvarMottatt(svarAt = svarAt),
            )
        } else {
            throw IllegalArgumentException("Kandidat må være Kandidat eller SvarMottatt for å registrere SvarMottatt")
        }
    }

    fun ferdigbehandleVurdering(veilederident: String): KartleggingssporsmalKandidat {
        if (this.status !is KartleggingssporsmalKandidatStatusendring.SvarMottatt) {
            throw IllegalArgumentException("Ferdigbehandling feilet: Kandidaten må ha status ${KartleggingssporsmalKandidatStatusendring.SvarMottatt::class.simpleName} for å ferdigbehandles, men var ${status.kandidatStatus} ")
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
    }
}

enum class KandidatStatus {
    KANDIDAT, SVAR_MOTTATT, FERDIGBEHANDLET,
}
