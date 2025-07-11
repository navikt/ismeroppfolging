package no.nav.syfo.senoppfolging.application

import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.jobbforventning.infrastructure.clients.pdl.model.PdlPerson

interface IPdlClient {
    suspend fun getPerson(personident: Personident): Result<PdlPerson>
}
