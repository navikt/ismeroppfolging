package no.nav.syfo.kartleggingssporsmal.application

import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.pdl.model.PdlPerson

interface IPdlClient {
    suspend fun getPerson(personident: Personident): Result<PdlPerson>
}
