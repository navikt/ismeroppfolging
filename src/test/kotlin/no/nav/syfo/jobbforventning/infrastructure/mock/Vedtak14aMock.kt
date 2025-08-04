package no.nav.syfo.jobbforventning.infrastructure.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode
import no.nav.syfo.UserConstants
import no.nav.syfo.jobbforventning.generators.generateVedtak14aResponse
import no.nav.syfo.jobbforventning.infrastructure.clients.vedtak14a.Vedtak14aRequestDTO
import no.nav.syfo.shared.infrastructure.mock.receiveBody
import no.nav.syfo.shared.infrastructure.mock.respond

suspend fun MockRequestHandleScope.vedtak14aMockResponse(request: HttpRequestData): HttpResponseData {
    val request = request.receiveBody<Vedtak14aRequestDTO>()
    return when (request.fnr) {
        UserConstants.ARBEIDSTAKER_PERSONIDENT_INACTIVE.value ->
            respond(null)
        UserConstants.ARBEIDSTAKER_PERSONIDENT_ERROR.value ->
            respondError(status = HttpStatusCode.InternalServerError)
        else -> respond(generateVedtak14aResponse())
    }
}
