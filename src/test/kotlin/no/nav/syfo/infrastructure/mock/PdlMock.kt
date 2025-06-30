package no.nav.syfo.infrastructure.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.UserConstants
import no.nav.syfo.generators.generatePdlError
import no.nav.syfo.generators.generatePdlHentPersonResponse
import no.nav.syfo.infrastructure.clients.pdl.model.PdlHentPersonRequest

suspend fun MockRequestHandleScope.pdlMockResponse(request: HttpRequestData): HttpResponseData {
    val pdlRequest = request.receiveBody<PdlHentPersonRequest>()
    return when (val personident = pdlRequest.variables.ident) {
        UserConstants.ARBEIDSTAKER_PERSONIDENT_INACTIVE.value ->
            respond(
                body = generatePdlHentPersonResponse(personident).copy(
                    errors = generatePdlError(code = "not_found")
                )
            )
        else -> respond(generatePdlHentPersonResponse(personident))
    }
}
