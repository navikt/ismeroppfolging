package no.nav.syfo.kartleggingssporsmal.application

import no.nav.syfo.kartleggingssporsmal.domain.JournalpostId
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat

interface IJournalforingService {
    suspend fun journalfor(
        kandidatVarslet: KartleggingssporsmalKandidat,
        pdf: ByteArray,
    ): Result<JournalpostId>
}
