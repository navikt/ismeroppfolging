package no.nav.syfo.jobbforventning.generators

import no.nav.syfo.jobbforventning.infrastructure.clients.vedtak14a.Hovedmal
import no.nav.syfo.jobbforventning.infrastructure.clients.vedtak14a.Innsatsgruppe
import no.nav.syfo.jobbforventning.infrastructure.clients.vedtak14a.Vedtak14aResponseDTO
import java.time.LocalDate

fun generateVedtak14aResponse(
    fattetDato: LocalDate = LocalDate.now().minusDays(10),
    innsatsgruppe: Innsatsgruppe = Innsatsgruppe.GODE_MULIGHETER,
    hovedmal: Hovedmal? = null,
) = Vedtak14aResponseDTO(
    fattetDato = fattetDato,
    innsatsgruppe = innsatsgruppe,
    hovedmal = hovedmal,
)
