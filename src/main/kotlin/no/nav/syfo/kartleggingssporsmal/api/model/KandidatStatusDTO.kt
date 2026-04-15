package no.nav.syfo.kartleggingssporsmal.api.model

import no.nav.syfo.kartleggingssporsmal.domain.KandidatStatus
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidatStatusendring
import no.nav.syfo.shared.domain.Personident
import java.time.OffsetDateTime
import java.util.*

data class KandidatStatusDTO(
    val kandidatUuid: UUID,
    val personident: Personident,
    val varsletAt: OffsetDateTime?,
    val svarAt: OffsetDateTime?,
    val status: KandidatStatus,
    val statusAt: OffsetDateTime,
    val vurdering: VurderingDTO?,
    val createdAt: OffsetDateTime,
)

data class VurderingDTO(
    val vurdertAt: OffsetDateTime,
    val vurdertBy: String,
    val vurderingAlternativ: KartleggingssporsmalKandidatStatusendring.Ferdigbehandlet.VurderingAlternativ?,
)

fun KartleggingssporsmalKandidat.toKandidatStatusDTO(
    kandidatStatusListe: List<KartleggingssporsmalKandidatStatusendring>,
): KandidatStatusDTO = KandidatStatusDTO(
    kandidatUuid = this.uuid,
    personident = this.personident,
    varsletAt = this.varsletAt,
    svarAt = kandidatStatusListe.firstOrNull { it is KartleggingssporsmalKandidatStatusendring.SvarMottatt }
        ?.let { (it as KartleggingssporsmalKandidatStatusendring.SvarMottatt).svarAt },
    status = this.status.kandidatStatus,
    statusAt = kandidatStatusListe.maxBy { it.createdAt }.createdAt,
    vurdering = kandidatStatusListe.firstOrNull { it is KartleggingssporsmalKandidatStatusendring.Ferdigbehandlet }
        ?.let {
            VurderingDTO(
                vurdertAt = it.createdAt,
                vurdertBy = (it as KartleggingssporsmalKandidatStatusendring.Ferdigbehandlet).veilederident,
                vurderingAlternativ = it.vurderingAlternativ,
            )
        },
    createdAt = this.createdAt
)
