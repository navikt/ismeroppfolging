package no.nav.syfo.senoppfolging.application

import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.jobbforventning.infrastructure.clients.behandlendeenhet.BehandlendeEnhetResponseDTO

interface IBehandlendeEnhetClient {
    suspend fun getEnhet(
        callId: String,
        personident: Personident,
    ): BehandlendeEnhetResponseDTO?
}
