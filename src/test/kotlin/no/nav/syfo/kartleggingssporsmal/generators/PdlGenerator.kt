package no.nav.syfo.kartleggingssporsmal.generators

import no.nav.syfo.UserConstants
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_NO_FODSELSDATO
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_INACTIVE
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.pdl.model.*
import java.time.LocalDate

fun generatePdlPerson(
    fodselsdato: LocalDate?,
) = PdlPerson(
    foedselsdato = fodselsdato?.let {
        listOf(
            Foedselsdato(
                foedselsdato = it,
                foedselsaar = it.year
            )
        )
    } ?: emptyList(),
    navn = listOf(
        PdlPersonNavn(
            fornavn = UserConstants.PERSON_FORNAVN,
            mellomnavn = UserConstants.PERSON_MELLOMNAVN,
            etternavn = UserConstants.PERSON_ETTERNAVN,
        )
    )
)

fun generatePdlHentPerson(
    fodseldato: LocalDate?,
) = PdlHentPerson(
    hentPerson = generatePdlPerson(fodseldato),
)

fun generatePdlHentPersonResponse(
    ident: String = ARBEIDSTAKER_PERSONIDENT.value,
    fodseldato: LocalDate? = LocalDate.now().minusYears(30),
    errors: List<PdlError> = emptyList()
) = PdlHentPersonResponse(
    errors = errors,
    data = if (ident == ARBEIDSTAKER_PERSONIDENT_INACTIVE.value) null else generatePdlHentPerson(
        fodseldato = if (ident == ARBEIDSTAKER_PERSONIDENT_NO_FODSELSDATO.value) null else fodseldato,
    ),
)

fun generatePdlError(code: String? = null) = listOf(
    PdlError(
        message = "Error",
        locations = emptyList(),
        path = emptyList(),
        extensions = PdlErrorExtension(
            code = code,
            classification = "Classification",
        )
    )
)
