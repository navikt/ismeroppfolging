package no.nav.syfo.shared.infrastructure.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.shared.infrastructure.clients.azuread.AzureAdTokenResponse

fun MockRequestHandleScope.azureAdMockResponse(): HttpResponseData = respond(
    AzureAdTokenResponse(
        access_token = "token",
        expires_in = 3600,
        token_type = "type",
    )
)
