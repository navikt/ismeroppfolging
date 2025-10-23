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
    val vurdering: KartleggingVurderingDTO?,
)

data class KartleggingVurderingDTO(
    val vurdertAt: OffsetDateTime,
    val vurdertBy: String,
)

fun KartleggingssporsmalKandidat.toKandidatStatusDTO(
    kandidatStatusListe: List<KartleggingssporsmalKandidatStatusendring>
): KandidatStatusDTO = KandidatStatusDTO(
    kandidatUuid = this.uuid,
    personident = this.personident,
    varsletAt = this.varsletAt,
    svarAt = kandidatStatusListe.firstOrNull { it.status == KandidatStatus.SVAR_MOTTATT }?.svarAt,
    status = this.status,
    statusAt = kandidatStatusListe.firstOrNull { it.status == this.status }?.createdAt ?: this.createdAt,
    vurdering = if (this.status == KandidatStatus.FERDIG_BEHANDLET) {
        val ferdigBehandletStatus = kandidatStatusListe.first { it.status == KandidatStatus.FERDIG_BEHANDLET }
        KartleggingVurderingDTO(
            vurdertAt = ferdigBehandletStatus.createdAt,
            vurdertBy = ferdigBehandletStatus.veilederident!!,
        )
    } else null,
)
