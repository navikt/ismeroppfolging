package no.nav.syfo.kartleggingssporsmal.application

import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat

interface IEsyfovarselProducer {
    fun sendKartleggingssporsmal(kartleggingssporsmalKandidat: KartleggingssporsmalKandidat): Result<KartleggingssporsmalKandidat>
    fun ferdigstillKartleggingssporsmalVarsel(kartleggingssporsmalKandidat: KartleggingssporsmalKandidat): Result<KartleggingssporsmalKandidat>
}
