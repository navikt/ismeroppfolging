package no.nav.syfo.kartleggingssporsmal.infrastructure.clients.pdl.model

import java.time.LocalDate
import java.time.Period
import java.util.Locale

data class PdlHentPersonRequest(
    val query: String,
    val variables: PdlHentPersonRequestVariables,
)

data class PdlHentPersonRequestVariables(
    val ident: String,
    val navnHistorikk: Boolean = false,
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
    val navn: List<PdlPersonNavn>,
) {
    val fullName: String = navn.firstOrNull()?.fullName()
        ?: throw RuntimeException("PDL returned empty navn for given fnr")
}

data class Foedselsdato(
    val foedselsdato: LocalDate?,
    val foedselsaar: Int?,
)

fun PdlPerson.getAlder(): Int? {
    val fodselsdato = this.foedselsdato.firstOrNull()?.foedselsdato
    val fodselsaar = this.foedselsdato.firstOrNull()?.foedselsaar
    val today = LocalDate.now()
    return if (fodselsdato != null) {
        Period.between(fodselsdato, today).years
    } else if (fodselsaar != null) {
        today.year - fodselsaar
    } else {
        null
    }
}

data class PdlPersonNavn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
) {
    fun fullName(): String {
        val fornavn = fornavn.lowerCapitalize()
        val etternavn = etternavn.lowerCapitalize()

        return if (mellomnavn.isNullOrBlank()) {
            "$fornavn $etternavn"
        } else {
            "$fornavn ${mellomnavn.lowerCapitalize()} $etternavn"
        }
    }
}

fun String.lowerCapitalize() =
    this.split(" ").joinToString(" ") { name ->
        val nameWithDash = name.split("-")
        if (nameWithDash.size > 1) {
            nameWithDash.joinToString("-") { it.capitalizeName() }
        } else {
            name.capitalizeName()
        }
    }

private fun String.capitalizeName() =
    this.lowercase(Locale.getDefault()).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
