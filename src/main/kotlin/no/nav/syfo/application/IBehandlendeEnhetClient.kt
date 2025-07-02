package no.nav.syfo.application

import no.nav.syfo.domain.Personident
import no.nav.syfo.infrastructure.clients.behandlendeenhet.BehandlendeEnhetResponseDTO

interface IBehandlendeEnhetClient {
    suspend fun getEnhet(
        callId: String,
        personident: Personident,
    ): BehandlendeEnhetResponseDTO?
}
