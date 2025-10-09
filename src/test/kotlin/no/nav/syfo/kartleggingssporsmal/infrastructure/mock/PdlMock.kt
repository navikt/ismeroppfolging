package no.nav.syfo.kartleggingssporsmal.infrastructure.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.UserConstants
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_INACTIVE
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_ONLY_FODSELSAAR
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_TOO_OLD
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_PDL_FAILS
import no.nav.syfo.kartleggingssporsmal.generators.generatePdlError
import no.nav.syfo.kartleggingssporsmal.generators.generatePdlHentPersonResponse
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.pdl.model.Foedselsdato
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.pdl.model.PdlHentPerson
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.pdl.model.PdlHentPersonRequest
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.pdl.model.PdlPerson
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.pdl.model.PdlPersonNavn
import no.nav.syfo.shared.infrastructure.mock.receiveBody
import no.nav.syfo.shared.infrastructure.mock.respond
import java.time.LocalDate

suspend fun MockRequestHandleScope.pdlMockResponse(request: HttpRequestData): HttpResponseData {
    val pdlRequest = request.receiveBody<PdlHentPersonRequest>()
    return when (val personident = pdlRequest.variables.ident) {
        ARBEIDSTAKER_PERSONIDENT_INACTIVE.value ->
            respond(
                body = generatePdlHentPersonResponse(personident).copy(
                    errors = generatePdlError(code = "not_found")
                )
            )
        ARBEIDSTAKER_PERSONIDENT_TOO_OLD.value ->
            respond(
                body = generatePdlHentPersonResponse(
                    ident = personident,
                    fodseldato = LocalDate.now().minusYears(67),
                )
            )
        ARBEIDSTAKER_PERSONIDENT_ONLY_FODSELSAAR.value ->
            respond(
                body = generatePdlHentPersonResponse(
                    ident = personident,
                ).copy(
                    data = PdlHentPerson(
                        hentPerson = PdlPerson(
                            foedselsdato = listOf(
                                Foedselsdato(
                                    foedselsdato = null,
                                    foedselsaar = LocalDate.now().minusYears(40).year
                                )
                            ),
                            navn = listOf(
                                PdlPersonNavn(
                                    fornavn = UserConstants.PERSON_FORNAVN,
                                    mellomnavn = UserConstants.PERSON_MELLOMNAVN,
                                    etternavn = UserConstants.PERSON_ETTERNAVN,
                                )
                            ),
                        ),
                    )
                )
            )
        ARBEIDSTAKER_PERSONIDENT_PDL_FAILS.value ->
            respond(
                body = generatePdlHentPersonResponse(errors = generatePdlError())
            )
        else -> respond(generatePdlHentPersonResponse(personident))
    }
}
