package no.nav.syfo.infrastructure.clients.pdfgen

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.shared.infrastructure.clients.httpClientDefault
import no.nav.syfo.shared.util.NAV_CALL_ID_HEADER
import org.slf4j.LoggerFactory

class PdfGenClient(
    private val httpClient: HttpClient = httpClientDefault(),
    private val pdfGenBaseUrl: String
) {

    suspend fun createKartleggingPdf(
        payload: PdfModel.KartleggingPdfModel,
        callId: String,
    ): ByteArray =
        getPdf(
            payload = payload,
            pdfUrl = "$pdfGenBaseUrl$API_BASE_PATH$KARTLEGGING_PATH",
            callId = callId,
        ) ?: throw RuntimeException("Failed to request pdf, callId: $callId")

    private suspend inline fun <reified Payload> getPdf(
        payload: Payload,
        pdfUrl: String,
        callId: String,
    ): ByteArray? =
        try {
            val response: HttpResponse = httpClient.post(pdfUrl) {
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            response.body()
        } catch (e: ResponseException) {
            handleUnexpectedResponseException(pdfUrl, e.response, callId)
        }

    private fun handleUnexpectedResponseException(
        url: String,
        response: HttpResponse,
        callId: String,
    ): ByteArray? {
        log.error(
            "Error while requesting PDF from syfooppdfgen with {}, {}, {}",
            StructuredArguments.keyValue("statusCode", response.status.value.toString()),
            StructuredArguments.keyValue("url", url),
            StructuredArguments.keyValue("callId", callId),
        )
        return null
    }

    companion object {
        private const val API_BASE_PATH = "/api/v1/genpdf"
        const val KARTLEGGING_PATH = "/kartlegging/utsending"

        private val log = LoggerFactory.getLogger(PdfGenClient::class.java)
    }
}
