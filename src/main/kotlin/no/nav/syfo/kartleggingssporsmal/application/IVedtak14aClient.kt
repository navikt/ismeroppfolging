package no.nav.syfo.kartleggingssporsmal.application

import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.vedtak14a.Vedtak14aResponseDTO
import no.nav.syfo.shared.domain.Personident

interface IVedtak14aClient {
    suspend fun hentGjeldende14aVedtak(personident: Personident): Result<Vedtak14aResponseDTO?>
}
