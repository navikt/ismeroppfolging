package no.nav.syfo.kartleggingssporsmal.infrastructure.clients.pdl.model

import java.time.LocalDate
import java.time.Period

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

fun PdlPerson.getAlder(): Int? {
    val fodselsdato = this.foedselsdato.firstOrNull()?.foedselsdato ?: return null
    val today = LocalDate.now()
    return Period.between(fodselsdato, today).years
}
