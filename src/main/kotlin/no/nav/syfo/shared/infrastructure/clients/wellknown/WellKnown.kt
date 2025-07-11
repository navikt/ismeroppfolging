package no.nav.syfo.shared.infrastructure.clients.wellknown

data class WellKnown(
    val issuer: String,
    val jwksUri: String,
)
