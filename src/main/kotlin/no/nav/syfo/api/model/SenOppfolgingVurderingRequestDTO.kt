package no.nav.syfo.api.model

import no.nav.syfo.domain.VurderingType

data class SenOppfolgingVurderingRequestDTO(
    val begrunnelse: String?,
    val type: VurderingType,
)
