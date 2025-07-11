package no.nav.syfo.kartleggingssporsmal.application

import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.behandlendeenhet.BehandlendeEnhetResponseDTO

interface IBehandlendeEnhetClient {
    suspend fun getEnhet(
        callId: String,
        personident: Personident,
    ): BehandlendeEnhetResponseDTO?
}
