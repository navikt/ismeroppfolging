package no.nav.syfo.infrastructure.clients.pdl.model

import java.time.LocalDate

data class PdlHentPersonRequest(
    val query: String,
    val variables: PdlHentPersonRequestVariables,
)

data class PdlHentPersonRequestVariables(
    val ident: String,
)

data class PdlHentPersonResponse(
    val errors: List<PdlError>?,
    val data: PdlHentPerson?,
)

data class PdlHentPerson(
    val hentPerson: PdlPerson?,
)

data class PdlPerson(
    val foedselsdato: List<Foedselsdato>,
)

data class Foedselsdato(
    val foedselsdato: LocalDate?,
    val foedselsaar: Int?,
)
