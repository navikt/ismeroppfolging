package no.nav.syfo.infrastructure.clients.pdl

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.syfo.application.IPdlClient
import no.nav.syfo.domain.Personident
import no.nav.syfo.infrastructure.bearerHeader
import no.nav.syfo.infrastructure.clients.ClientEnvironment
import no.nav.syfo.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.infrastructure.clients.httpClientDefault
import no.nav.syfo.infrastructure.clients.pdl.model.*
import org.slf4j.LoggerFactory

class PdlClient(
    private val azureAdClient: AzureAdClient,
    private val clientEnvironment: ClientEnvironment,
    private val httpClient: HttpClient = httpClientDefault(),
) : IPdlClient {

    override suspend fun getPerson(personident: Personident): Result<PdlPerson> =
        try {
            val token = azureAdClient.getSystemToken(clientEnvironment.clientId)
                ?: throw RuntimeException("Failed to send request to PDL: No token was found")
            val query = getPdlQuery("/pdl/hentPerson.graphql")
            val request = PdlHentPersonRequest(
                query = query,
                variables = PdlHentPersonRequestVariables(ident = personident.value),
            )

            val response: HttpResponse = httpClient.post(clientEnvironment.baseUrl) {
                setBody(request)
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Authorization, bearerHeader(token.accessToken))
                header(BEHANDLINGSNUMMER_HEADER_KEY, BEHANDLINGSNUMMER_HEADER_VALUE)
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val pdlPersonReponse = response.body<PdlHentPersonResponse>()
                    val isErrors = !pdlPersonReponse.errors.isNullOrEmpty()
                    if (isErrors) {
                        pdlPersonReponse.errors.forEach {
                            logger.error("Error while requesting person from PersonDataLosningen: ${it.errorMessage()}")
                        }
                    }
                    pdlPersonReponse.data
                        ?.hentPerson
                        ?.let { Result.success(it) }
                        ?: Result.failure(RuntimeException("No person found in PDL response"))
                }
                else -> {
                    logger.error("Request with url: ${clientEnvironment.baseUrl} failed with reponse code ${response.status.value}")
                    Result.failure(RuntimeException("Failed to get person from PDL"))
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to get person from PDL", e)
            Result.failure(e)
        }

    private fun getPdlQuery(queryFilePath: String): String {
        return this::class.java.getResource(queryFilePath)!!
            .readText()
            .replace("[\n\r]", "")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PdlClient::class.java)

        // Se behandlingskatalog https://behandlingskatalog.intern.nav.no/
        // Team isyfo: https://behandlingskatalog.intern.nav.no/process/team/02e803bf-0114-4ad2-8871-685b5c3035ba
        // Behandling: Sykefraværsoppfølging: Modia sykefraværsoppfolging
        private const val BEHANDLINGSNUMMER_HEADER_KEY = "behandlingsnummer"
        private const val BEHANDLINGSNUMMER_HEADER_VALUE = "B426"
    }
}
