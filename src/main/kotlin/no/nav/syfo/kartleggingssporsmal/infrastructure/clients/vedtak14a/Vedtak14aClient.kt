package no.nav.syfo.kartleggingssporsmal.infrastructure.clients.vedtak14a

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.micrometer.core.instrument.Counter
import no.nav.syfo.kartleggingssporsmal.application.IVedtak14aClient
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.vedtak14a.Vedtak14aClient.Companion.GJELDENDE_14A_VEDTAK_API_PATH
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.infrastructure.clients.ClientEnvironment
import no.nav.syfo.shared.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.shared.infrastructure.clients.httpClientDefault
import no.nav.syfo.shared.infrastructure.metric.METRICS_NS
import no.nav.syfo.shared.infrastructure.metric.METRICS_REGISTRY
import no.nav.syfo.shared.util.bearerHeader
import org.slf4j.LoggerFactory

/**
 * See API documentation:
 * https://veilarbvedtaksstotte.intern.dev.nav.no/veilarbvedtaksstotte/internal/swagger-ui/index.html?urls.primaryName=Eksterne+endepunkter+(for+konsumenter)#/
 */

class Vedtak14aClient(
    private val azureAdClient: AzureAdClient,
    private val clientEnvironment: ClientEnvironment,
    private val httpClient: HttpClient = httpClientDefault(),
) : IVedtak14aClient {

    private val vedtak14aUrl = "${clientEnvironment.baseUrl}$GJELDENDE_14A_VEDTAK_API_PATH"

    override suspend fun hentGjeldende14aVedtak(personident: Personident): Result<Vedtak14aResponseDTO?> {
        val systemToken = azureAdClient.getSystemToken(
            scopeClientId = clientEnvironment.clientId,
        )
            ?.accessToken
            ?: throw RuntimeException("Failed to request access to veilarbvedtaksstotte")

        val requestDTO = Vedtak14aRequestDTO(
            fnr = personident.value,
        )

        return try {
            val response: HttpResponse = httpClient.post(vedtak14aUrl) {
                header(HttpHeaders.Authorization, bearerHeader(systemToken))
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(requestDTO)
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val responseText = response.bodyAsText()
                    if (responseText.isBlank() || responseText == "null") {
                        Metrics.COUNT_HAS_NOT_VEDTAK_14A_API.increment()
                        Result.success(null)
                    } else {
                        val vedtak14aResponse = response.body<Vedtak14aResponseDTO>()
                        Metrics.COUNT_HAS_VEDTAK_14A_API.increment()
                        Result.success(vedtak14aResponse)
                    }
                }
                else -> {
                    log.error("Failed to get gjeldende 14a vedtak: statuscode ${response.status.value}")
                    Metrics.COUNT_VEDTAK_14A_API_ERROR.increment()
                    Result.failure(RuntimeException("Failed to get gjeldende 14a vedtak: ${response.status.value}"))
                }
            }
        } catch (e: Exception) {
            log.error("Failed to get gjeldende 14a vedtak", e)
            Metrics.COUNT_VEDTAK_14A_API_ERROR.increment()
            Result.failure(e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(Vedtak14aClient::class.java)
        const val GJELDENDE_14A_VEDTAK_API_PATH = "/veilarbvedtaksstotte/api/ekstern/hent-gjeldende-14a-vedtak"
    }
}

private object Metrics {
    private const val VEDTAK_14A_API_BASE = "${METRICS_NS}_vedtak_14a_api"

    val COUNT_HAS_VEDTAK_14A_API: Counter = Counter
        .builder("${VEDTAK_14A_API_BASE}_has_vedtak_count")
        .description("Counts the number of persons with 14a-vedtak from API $GJELDENDE_14A_VEDTAK_API_PATH")
        .register(METRICS_REGISTRY)
    val COUNT_HAS_NOT_VEDTAK_14A_API: Counter = Counter
        .builder("${VEDTAK_14A_API_BASE}_has_not_vedtak_count")
        .description("Counts the number of persons without 14a-vedtak from API $GJELDENDE_14A_VEDTAK_API_PATH")
        .register(METRICS_REGISTRY)
    val COUNT_VEDTAK_14A_API_ERROR: Counter = Counter
        .builder("${VEDTAK_14A_API_BASE}_error_count")
        .description("Counts the number of erroneous call to API $GJELDENDE_14A_VEDTAK_API_PATH")
        .register(METRICS_REGISTRY)
}
