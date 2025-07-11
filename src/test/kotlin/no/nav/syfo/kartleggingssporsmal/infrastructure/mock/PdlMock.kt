package no.nav.syfo.kartleggingssporsmal.infrastructure.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.UserConstants
import no.nav.syfo.kartleggingssporsmal.generators.generatePdlError
import no.nav.syfo.kartleggingssporsmal.generators.generatePdlHentPersonResponse
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.pdl.model.PdlHentPersonRequest
import no.nav.syfo.shared.infrastructure.mock.receiveBody
import no.nav.syfo.shared.infrastructure.mock.respond

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
