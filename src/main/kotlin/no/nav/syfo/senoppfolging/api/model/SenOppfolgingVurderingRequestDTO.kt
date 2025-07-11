package no.nav.syfo.senoppfolging.api.model

import no.nav.syfo.senoppfolging.domain.VurderingType

data class SenOppfolgingVurderingRequestDTO(
    val begrunnelse: String?,
    val type: VurderingType,
)
