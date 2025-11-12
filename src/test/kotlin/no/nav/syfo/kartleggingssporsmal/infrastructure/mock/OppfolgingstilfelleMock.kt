package no.nav.syfo.kartleggingssporsmal.infrastructure.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_NO_TILFELLE
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_ERROR
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_NO_ARBEIDSGIVER
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_TILFELLE_DOD
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT_DURATION_LEFT
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT_DURATION_LEFT_BUT_LONG
import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfellePersonDTO
import no.nav.syfo.shared.infrastructure.mock.respond
import no.nav.syfo.shared.util.NAV_PERSONIDENT_HEADER
import java.time.LocalDate

fun MockRequestHandleScope.oppfolgingstilfelleResponse(request: HttpRequestData): HttpResponseData {
    val personident = request.headers[NAV_PERSONIDENT_HEADER]
    return when (personident) {
        ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT.value -> respond(
            createOppfolgingstilfellePersonDTO(
                personident = ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT.value,
                tilfelleStart = LocalDate.now().minusWeeks(3),
                antallSykedager = 10,
            )
        )
        ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT_DURATION_LEFT.value -> respond(
            createOppfolgingstilfellePersonDTO(
                personident = ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT_DURATION_LEFT.value,
                tilfelleStart = LocalDate.now().minusDays(6 * 7 + 1),
                antallSykedager = 6 * 7 + 3,
            )
        )
        ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT_DURATION_LEFT_BUT_LONG.value -> respond(
            createOppfolgingstilfellePersonDTO(
                personident = ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT_DURATION_LEFT_BUT_LONG.value,
                tilfelleStart = LocalDate.now().minusDays(8 * 7 + 1),
                antallSykedager = 8 * 7 + 3,
            )
        )
        ARBEIDSTAKER_PERSONIDENT_NO_TILFELLE.value -> respond(
            createOppfolgingstilfellePersonDTO(
                personident = ARBEIDSTAKER_PERSONIDENT_NO_TILFELLE.value,
                hasTilfelle = false,
            )
        )
        ARBEIDSTAKER_PERSONIDENT_NO_ARBEIDSGIVER.value -> respond(
            createOppfolgingstilfellePersonDTO(
                personident = ARBEIDSTAKER_PERSONIDENT_NO_ARBEIDSGIVER.value,
                tilfelleStart = LocalDate.now().minusWeeks(3),
                antallSykedager = 42,
                isArbeidstakerAtTilfelleEnd = false,
                virksomhetsnummerList = emptyList(),
            )
        )
        ARBEIDSTAKER_PERSONIDENT_TILFELLE_DOD.value -> respond(
            createOppfolgingstilfellePersonDTO(
                personident = ARBEIDSTAKER_PERSONIDENT_TILFELLE_DOD.value,
                dodsdato = LocalDate.now().minusWeeks(1),
                tilfelleStart = LocalDate.now().minusWeeks(3),
                antallSykedager = 43,
            )
        )
        ARBEIDSTAKER_PERSONIDENT_ERROR.value -> respondError(HttpStatusCode.InternalServerError)
        else -> respond(
            createOppfolgingstilfellePersonDTO(
                personident = personident!!,
                tilfelleStart = LocalDate.now().minusDays(6 * 7 + 1),
                antallSykedager = 6 * 7 + 14,
            )
        )
    }
}
