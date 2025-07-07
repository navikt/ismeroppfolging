package no.nav.syfo.application

import no.nav.syfo.domain.Personident
import no.nav.syfo.infrastructure.clients.pdl.model.PdlPerson

interface IPdlClient {
    suspend fun getPerson(personident: Personident): Result<PdlPerson>
}
