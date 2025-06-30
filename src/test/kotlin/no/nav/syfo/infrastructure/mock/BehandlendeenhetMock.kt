package no.nav.syfo.infrastructure.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET
import no.nav.syfo.generators.getBehandlendeEnhetDTO
import no.nav.syfo.infrastructure.NAV_PERSONIDENT_HEADER

fun MockRequestHandleScope.behandlendeenhetResponse(request: HttpRequestData): HttpResponseData {
    return when (request.headers[NAV_PERSONIDENT_HEADER]) {
        ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET.value -> respond(
            getBehandlendeEnhetDTO(geografiskEnhetId = "4444")
        )
        else -> respond(getBehandlendeEnhetDTO())
    }
}
