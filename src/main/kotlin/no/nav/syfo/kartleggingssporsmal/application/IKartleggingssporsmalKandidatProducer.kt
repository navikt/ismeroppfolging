package no.nav.syfo.kartleggingssporsmal.application

import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidatStatusendring

interface IKartleggingssporsmalKandidatProducer {
    fun send(
        kandidat: KartleggingssporsmalKandidat,
        statusEndring: KartleggingssporsmalKandidatStatusendring,
    ): Result<KartleggingssporsmalKandidat>
}
