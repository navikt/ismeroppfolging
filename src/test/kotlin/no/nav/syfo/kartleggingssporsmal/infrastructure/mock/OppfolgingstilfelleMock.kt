package no.nav.syfo.kartleggingssporsmal.infrastructure.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_2
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_3
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_ERROR
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_INACTIVE
import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfellePersonDTO
import no.nav.syfo.shared.infrastructure.mock.respond
import no.nav.syfo.shared.util.NAV_PERSONIDENT_HEADER
import java.time.LocalDate

fun MockRequestHandleScope.oppfolgingstilfelleResponse(request: HttpRequestData): HttpResponseData {
    val personident = request.headers[NAV_PERSONIDENT_HEADER]
    return when (personident) {
        ARBEIDSTAKER_PERSONIDENT_2.value -> respond(
            createOppfolgingstilfellePersonDTO(
                personident = ARBEIDSTAKER_PERSONIDENT_2.value,
                tilfelleStart = LocalDate.now().minusWeeks(3),
                antallSykedager = 10,
            )
        )
        ARBEIDSTAKER_PERSONIDENT_3.value -> respond(
            createOppfolgingstilfellePersonDTO(
                personident = ARBEIDSTAKER_PERSONIDENT_ERROR.value,
                hasTilfelle = false,
            )
        )
        ARBEIDSTAKER_PERSONIDENT_INACTIVE.value -> respond(
            createOppfolgingstilfellePersonDTO(
                personident = ARBEIDSTAKER_PERSONIDENT_INACTIVE.value,
                dodsdato = LocalDate.now().minusWeeks(1),
                tilfelleStart = LocalDate.now().minusWeeks(3),
                antallSykedager = 43,
            )
        )
        ARBEIDSTAKER_PERSONIDENT_ERROR.value -> respondError(HttpStatusCode.InternalServerError)
        else -> respond(
            createOppfolgingstilfellePersonDTO(
                personident = personident!!,
                tilfelleStart = LocalDate.now().minusWeeks(3),
                antallSykedager = 42,
            )
        )
    }
}
