package no.nav.syfo.kartleggingssporsmal.application

import no.nav.syfo.kartleggingssporsmal.domain.KandidatStatus
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import java.time.OffsetDateTime

interface IKartleggingssporsmalKandidatProducer {
    fun send(
        kandidat: KartleggingssporsmalKandidat,
        status: KandidatStatus,
        statusTidspunkt: OffsetDateTime,
    ): Result<KartleggingssporsmalKandidat>
}
