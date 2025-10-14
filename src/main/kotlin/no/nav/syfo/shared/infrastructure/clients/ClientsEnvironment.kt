package no.nav.syfo.shared.infrastructure.clients

data class ClientsEnvironment(
    val istilgangskontroll: ClientEnvironment,
    val syfobehandlendeenhet: ClientEnvironment,
    val pdl: ClientEnvironment,
    val dokarkiv: ClientEnvironment,
    val veilarbvedtaksstotte: ClientEnvironment,
    val isoppfolgingstilfelle: ClientEnvironment,
    val esyfopdfgen: ClientEnvironment,
)

data class ClientEnvironment(
    val baseUrl: String,
    val clientId: String,
)

data class OpenClientEnvironment(
    val baseUrl: String,
)
