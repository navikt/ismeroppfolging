package no.nav.syfo.kartleggingssporsmal.infrastructure.clients.oppfolgingstilfelle

import no.nav.syfo.kartleggingssporsmal.domain.OppfolgingstilfelleDTO
import java.time.LocalDate

data class OppfolgingstilfellePersonDTO(
    val oppfolgingstilfelleList: List<OppfolgingstilfelleDTO>,
    val personIdent: String,
    val dodsdato: LocalDate?,
)
