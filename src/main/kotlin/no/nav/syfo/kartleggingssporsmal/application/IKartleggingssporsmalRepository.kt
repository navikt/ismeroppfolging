package no.nav.syfo.kartleggingssporsmal.application

import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt

interface IKartleggingssporsmalRepository {
    fun createStoppunkt(stoppunkt: KartleggingssporsmalStoppunkt): KartleggingssporsmalStoppunkt
}
