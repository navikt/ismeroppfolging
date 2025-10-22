package no.nav.syfo.kartleggingssporsmal.api.endpoints.dto

import no.nav.syfo.kartleggingssporsmal.domain.KandidatStatus
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidatStatusendring
import no.nav.syfo.shared.domain.Personident
import java.time.OffsetDateTime
import java.util.UUID

data class KandidatStatusDTO(
    val kandidatUuid: UUID,
    val personident: Personident,
    val varsletAt: OffsetDateTime?,
    val svarAt: OffsetDateTime?,
    val status: KandidatStatus,
    val statusAt: OffsetDateTime,
    val statusVeilederident: String?,
)

fun KartleggingssporsmalKandidat.toKandidatStatusDTO(
    kandidatStatusListe: List<KartleggingssporsmalKandidatStatusendring>
): KandidatStatusDTO = KandidatStatusDTO(
    kandidatUuid = this.uuid,
    personident = this.personident,
    varsletAt = this.varsletAt,
    svarAt = kandidatStatusListe.firstOrNull { it.status == KandidatStatus.SVAR_MOTTATT }?.createdAt,
    status = this.status,
    statusAt = kandidatStatusListe.firstOrNull { it.status == this.status }?.createdAt ?: this.createdAt,
    statusVeilederident = if (this.status == KandidatStatus.FERDIG_BEHANDLET) {
        kandidatStatusListe.firstOrNull { it.status == KandidatStatus.FERDIG_BEHANDLET }?.veilederident
    } else {
        null
    },
)
