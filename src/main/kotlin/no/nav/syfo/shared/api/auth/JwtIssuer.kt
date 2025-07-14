package no.nav.syfo.shared.api.auth

import no.nav.syfo.shared.infrastructure.clients.wellknown.WellKnown

data class JwtIssuer(
    val acceptedAudienceList: List<String>,
    val jwtIssuerType: JwtIssuerType,
    val wellKnown: WellKnown
)

enum class JwtIssuerType {
    INTERNAL_AZUREAD
}
