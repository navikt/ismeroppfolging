package no.nav.syfo.api

import io.ktor.server.application.*
import no.nav.syfo.ExternalMockEnvironment

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
) {
    val database = externalMockEnvironment.database

    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        environment = externalMockEnvironment.environment,
        wellKnownInternalAzureAD = externalMockEnvironment.wellKnownInternalAzureAD,
        database = database,
    )
}
