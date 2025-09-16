package no.nav.syfo.kartleggingssporsmal.generators

import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.vedtak14a.Hovedmal
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.vedtak14a.Innsatsgruppe
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.vedtak14a.Vedtak14aResponseDTO
import java.time.ZonedDateTime

fun generateVedtak14aResponse(
    fattetDato: ZonedDateTime = ZonedDateTime.now().minusDays(10),
    innsatsgruppe: Innsatsgruppe = Innsatsgruppe.GODE_MULIGHETER,
    hovedmal: Hovedmal? = null,
) = Vedtak14aResponseDTO(
    fattetDato = fattetDato,
    innsatsgruppe = innsatsgruppe,
    hovedmal = hovedmal,
)
