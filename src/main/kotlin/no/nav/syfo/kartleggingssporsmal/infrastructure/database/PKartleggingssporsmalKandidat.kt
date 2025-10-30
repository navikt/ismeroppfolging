package no.nav.syfo.kartleggingssporsmal.infrastructure.database

import no.nav.syfo.kartleggingssporsmal.domain.JournalpostId
import no.nav.syfo.kartleggingssporsmal.domain.KandidatStatus
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidatStatusendring
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidatStatusendring.*
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
    val varsletAt: OffsetDateTime?,
    val journalpostId: JournalpostId?,
) {
    fun toKartleggingssporsmalKandidat(
        pKartleggingssporsmalKandidatStatusendringer: PKartleggingssporsmalKandidatStatusendring,
    ) =
        KartleggingssporsmalKandidat(
            uuid = this.uuid,
            createdAt = this.createdAt,
            personident = this.personident,
            status = pKartleggingssporsmalKandidatStatusendringer.toKartleggingssporsmalKandidatStatusendring(),
            varsletAt = this.varsletAt,
            journalpostId = this.journalpostId,
        )
}

data class PKartleggingssporsmalKandidatStatusendring(
    val id: Int,
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val kandidatId: Int,
    val status: String,
    val publishedAt: OffsetDateTime?,
    val svarAt: OffsetDateTime?,
    val veilederident: String?,
) {
    fun toKartleggingssporsmalKandidatStatusendring(): KartleggingssporsmalKandidatStatusendring =
        when (this.status) {
            KandidatStatus.KANDIDAT.name ->
                Kandidat(
                    uuid = this.uuid,
                    createdAt = this.createdAt,
                    publishedAt = this.publishedAt,
                )
            KandidatStatus.SVAR_MOTTATT.name ->
                SvarMottatt(
                    uuid = this.uuid,
                    createdAt = this.createdAt,
                    publishedAt = this.publishedAt,
                    svarAt = this.svarAt!!,
                )
            KandidatStatus.FERDIGBEHANDLET.name ->
                Ferdigbehandlet(
                    uuid = this.uuid,
                    createdAt = this.createdAt,
                    publishedAt = this.publishedAt,
                    veilederident = this.veilederident!!,
                )
            else -> throw IllegalArgumentException("Ukjent statusendring: ${this.status}")
        }
}
