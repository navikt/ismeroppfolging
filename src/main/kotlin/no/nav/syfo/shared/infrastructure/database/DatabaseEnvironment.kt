package no.nav.syfo.shared.infrastructure.database

data class DatabaseEnvironment(
    val host: String,
    val port: String,
    val name: String,
    val username: String,
    val password: String,
    val url: String,
)
