package no.nav.syfo.kartleggingssporsmal.api.model

import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidatStatusendring.Ferdigbehandlet.VurderingAlternativ

data class KartleggingssporsmalRequestDTO(
    val vurderingAlternativ: VurderingAlternativ? = null,
)
