package no.nav.syfo.kartleggingssporsmal.infrastructure.clients.oppfolgingstilfelle

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.syfo.kartleggingssporsmal.application.IOppfolgingstilfelleClient
import no.nav.syfo.kartleggingssporsmal.domain.Oppfolgingstilfelle
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.infrastructure.clients.ClientEnvironment
import no.nav.syfo.shared.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.shared.infrastructure.clients.httpClientDefault
import no.nav.syfo.shared.util.NAV_CALL_ID_HEADER
import no.nav.syfo.shared.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.shared.util.bearerHeader
import org.slf4j.LoggerFactory

class OppfolgingstilfelleClient(
    private val azureAdClient: AzureAdClient,
    private val clientEnvironment: ClientEnvironment,
    private val httpClient: HttpClient = httpClientDefault(),
) : IOppfolgingstilfelleClient {
    private val personOppfolgingstilfelleSystemUrl: String =
        "${clientEnvironment.baseUrl}$ISOPPFOLGINGSTILFELLE_OPPFOLGINGSTILFELLE_SYSTEM_PERSON_PATH"

    override suspend fun getOppfolgingstilfelle(
        personident: Personident,
        callId: String,
    ): Result<Oppfolgingstilfelle.OppfolgingstilfelleFromApi?> {
        val azureToken = azureAdClient.getSystemToken(clientEnvironment.clientId)
            ?.accessToken
            ?: throw RuntimeException("Failed to get azure system token")

        return try {
            val response: HttpResponse = httpClient.get(personOppfolgingstilfelleSystemUrl) {
                header(HttpHeaders.Authorization, bearerHeader(azureToken))
                header(NAV_CALL_ID_HEADER, callId)
                header(NAV_PERSONIDENT_HEADER, personident.value)
                accept(ContentType.Application.Json)
            }
            val body = response.body<OppfolgingstilfellePersonDTO>()
            Result.success(
                Oppfolgingstilfelle.createFromApi(
                    personident = body.personIdent,
                    dodsdato = body.dodsdato,
                    oppfolgingstilfelleList = body.oppfolgingstilfelleList,
                )
            ).also {
                COUNT_CALL_OPPFOLGINGSTILFELLE_PERSON_SUCCESS.increment()
            }
        } catch (responseException: ResponseException) {
            log.error("Error while requesting OppfolgingstilfellePerson from isoppfolgingstilfelle with status: ${responseException.response.status.value}, callId $callId")
            COUNT_CALL_OPPFOLGINGSTILFELLE_PERSON_FAIL.increment()
            Result.failure(responseException)
        }
    }

    companion object {
        private const val ISOPPFOLGINGSTILFELLE_OPPFOLGINGSTILFELLE_SYSTEM_PERSON_PATH =
            "/api/system/v1/oppfolgingstilfelle/personident"

        private val log = LoggerFactory.getLogger(OppfolgingstilfelleClient::class.java)
    }
}
