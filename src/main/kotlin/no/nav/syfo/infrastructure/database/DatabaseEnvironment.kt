package no.nav.syfo.infrastructure.database

data class DatabaseEnvironment(
    val host: String,
    val port: String,
    val name: String,
    val username: String,
    val password: String,
    val url: String,
)
