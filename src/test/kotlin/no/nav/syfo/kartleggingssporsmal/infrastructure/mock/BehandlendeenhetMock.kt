package no.nav.syfo.kartleggingssporsmal.infrastructure.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_INACTIVE
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_PILOT_UTEN_UTSENDING
import no.nav.syfo.UserConstants.VALID_PILOTKONTOR_UTEN_VARSEL
import no.nav.syfo.kartleggingssporsmal.generators.getBehandlendeEnhetDTO
import no.nav.syfo.shared.infrastructure.mock.respond
import no.nav.syfo.shared.util.NAV_PERSONIDENT_HEADER

fun MockRequestHandleScope.behandlendeenhetResponse(request: HttpRequestData): HttpResponseData {
    return when (request.headers[NAV_PERSONIDENT_HEADER]) {
        ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET.value -> respond(
            getBehandlendeEnhetDTO(geografiskEnhetId = "4444")
        )
        ARBEIDSTAKER_PERSONIDENT_INACTIVE.value -> respond(
            body = emptyList<String>(),
            statusCode = HttpStatusCode.NoContent,
        )
        ARBEIDSTAKER_PERSONIDENT_PILOT_UTEN_UTSENDING.value -> respond(
            getBehandlendeEnhetDTO(geografiskEnhetId = VALID_PILOTKONTOR_UTEN_VARSEL)
        )
        else -> respond(getBehandlendeEnhetDTO())
    }
}
