package no.nav.syfo

import no.nav.syfo.shared.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.jobbforventning.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.jobbforventning.infrastructure.clients.vedtak14a.Vedtak14aClient
import no.nav.syfo.shared.infrastructure.clients.wellknown.WellKnown
import no.nav.syfo.shared.infrastructure.database.TestDatabase
import no.nav.syfo.shared.infrastructure.mock.mockHttpClient
import java.nio.file.Paths

fun wellKnownInternalAzureAD(): WellKnown {
    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    return WellKnown(
        issuer = "https://sts.issuer.net/veileder/v2",
        jwksUri = uri.toString()
    )
}

class ExternalMockEnvironment private constructor() {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()
    val environment = testEnvironment()
    val mockHttpClient = mockHttpClient(environment = environment)
    val wellKnownInternalAzureAD = wellKnownInternalAzureAD()
    val azureAdClient = AzureAdClient(
        azureEnvironment = environment.azure,
        httpClient = mockHttpClient,
    )
    val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.pdl,
        httpClient = mockHttpClient,
    )
    val vedtak14aClient = Vedtak14aClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.veilarbvedtaksstotte,
        httpClient = mockHttpClient,
    )

    companion object {
        val instance: ExternalMockEnvironment = ExternalMockEnvironment()
    }
}
