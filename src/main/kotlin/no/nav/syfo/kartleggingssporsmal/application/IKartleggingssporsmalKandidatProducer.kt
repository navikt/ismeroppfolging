package no.nav.syfo.kartleggingssporsmal.application

import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat

interface IKartleggingssporsmalKandidatProducer {
    fun send(kandidat: KartleggingssporsmalKandidat): Result<KartleggingssporsmalKandidat>
}
