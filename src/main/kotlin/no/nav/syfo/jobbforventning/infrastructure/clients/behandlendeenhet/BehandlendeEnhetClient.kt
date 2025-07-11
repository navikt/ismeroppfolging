package no.nav.syfo.jobbforventning.infrastructure.clients.behandlendeenhet

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.syfo.senoppfolging.application.IBehandlendeEnhetClient
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.util.NAV_CALL_ID_HEADER
import no.nav.syfo.shared.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.shared.util.bearerHeader
import no.nav.syfo.shared.infrastructure.clients.ClientEnvironment
import no.nav.syfo.shared.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.shared.infrastructure.clients.httpClientDefault
import org.slf4j.LoggerFactory

class BehandlendeEnhetClient(
    private val azureAdClient: AzureAdClient,
    private val clientEnvironment: ClientEnvironment,
    private val httpClient: HttpClient = httpClientDefault(),
) : IBehandlendeEnhetClient {
    private val behandlendeEnhetUrl = "${clientEnvironment.baseUrl}$BEHANDLENDEENHET_SYSTEM_PATH"

    override suspend fun getEnhet(
        callId: String,
        personident: Personident,
    ): BehandlendeEnhetResponseDTO? {
        val systemToken = azureAdClient.getSystemToken(
            scopeClientId = clientEnvironment.clientId,
        )
            ?.accessToken
            ?: throw RuntimeException("Failed to request access to Syfobehandlendeenhet: Failed to get token")

        return try {
            val response: HttpResponse = httpClient.get(behandlendeEnhetUrl) {
                header(HttpHeaders.Authorization, bearerHeader(systemToken))
                header(NAV_CALL_ID_HEADER, callId)
                header(NAV_PERSONIDENT_HEADER, personident.value)
                accept(ContentType.Application.Json)
            }
            if (response.status == HttpStatusCode.NoContent) {
                null
            } else {
                response.body<BehandlendeEnhetResponseDTO>()
            }
        } catch (e: Exception) {
            log.error(
                """
                    Error while requesting BehandlendeEnhet of person from Syfobehandlendeenhet
                    CallId: $callId
                    Error: ${e.message}
                """.trimIndent(),
            )
            throw e
        }
    }

    companion object {
        const val BEHANDLENDEENHET_SYSTEM_PATH = "/api/system/v2/personident"
        private val log = LoggerFactory.getLogger(BehandlendeEnhetClient::class.java)
    }
}
