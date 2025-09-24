package no.nav.syfo.kartleggingssporsmal.application

import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.shared.domain.Personident

interface IKartleggingssporsmalRepository {
    suspend fun createStoppunkt(stoppunkt: KartleggingssporsmalStoppunkt): KartleggingssporsmalStoppunkt
    suspend fun getKandidat(personident: Personident): KartleggingssporsmalKandidat?
    suspend fun createKandidatAndMarkStoppunktAsProcessed(kandidat: KartleggingssporsmalKandidat, stoppunktId: Int): KartleggingssporsmalKandidat
    suspend fun getUnprocessedStoppunkter(): List<Pair<Int, KartleggingssporsmalStoppunkt>>
    suspend fun hasReceivedQuestions(personident: Personident): Boolean
}
