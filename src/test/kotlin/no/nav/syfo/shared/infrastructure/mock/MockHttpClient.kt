package no.nav.syfo.shared.infrastructure.mock

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import no.nav.syfo.Environment
import no.nav.syfo.jobbforventning.infrastructure.mock.behandlendeenhetResponse
import no.nav.syfo.jobbforventning.infrastructure.mock.pdlMockResponse
import no.nav.syfo.shared.infrastructure.clients.commonConfig

fun mockHttpClient(environment: Environment) = HttpClient(MockEngine) {
    commonConfig()
    engine {
        addHandler { request ->
            val requestUrl = request.url.encodedPath
            when {
                requestUrl == "/${environment.azure.openidConfigTokenEndpoint}" -> azureAdMockResponse()
                requestUrl.startsWith("/${environment.clients.istilgangskontroll.baseUrl}") -> tilgangskontrollResponse(
                    request
                )
                requestUrl.startsWith("/${environment.clients.syfobehandlendeenhet.baseUrl}") -> behandlendeenhetResponse(
                    request
                )
                requestUrl.startsWith("/${environment.clients.pdl.baseUrl}") -> pdlMockResponse(
                    request
                )
                else -> error("Unhandled ${request.url.encodedPath}")
            }
        }
    }
}
