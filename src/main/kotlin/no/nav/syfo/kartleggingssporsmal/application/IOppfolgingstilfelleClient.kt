package no.nav.syfo.kartleggingssporsmal.application

import no.nav.syfo.kartleggingssporsmal.domain.Oppfolgingstilfelle
import no.nav.syfo.shared.domain.Personident
import java.util.UUID

interface IOppfolgingstilfelleClient {
    suspend fun getOppfolgingstilfelle(
        personident: Personident,
        callId: String = UUID.randomUUID().toString(),
    ): Result<Oppfolgingstilfelle.OppfolgingstilfelleFromApi?>
}
