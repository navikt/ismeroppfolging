package no.nav.syfo.generators

import no.nav.syfo.UserConstants.VEILEDER_IDENT
import no.nav.syfo.infrastructure.clients.behandlendeenhet.BehandlendeEnhetResponseDTO
import no.nav.syfo.infrastructure.clients.behandlendeenhet.Enhet
import no.nav.syfo.infrastructure.clients.behandlendeenhet.OppfolgingsenhetDTO
import java.time.LocalDateTime

fun getBehandlendeEnhetDTO(
    geografiskEnhetId: String = "1234",
    oppfolgingsenhetId: String? = null,
) = BehandlendeEnhetResponseDTO(
    geografiskEnhet = Enhet(
        enhetId = geografiskEnhetId,
        navn = "Geografisk enhet",
    ),
    oppfolgingsenhetDTO = oppfolgingsenhetId?.let {
        OppfolgingsenhetDTO(
            enhet = Enhet(
                enhetId = oppfolgingsenhetId,
                navn = "Oppfolgingsenhet",
            ),
            createdAt = LocalDateTime.now(),
            veilederident = VEILEDER_IDENT,
        )
    }
)
