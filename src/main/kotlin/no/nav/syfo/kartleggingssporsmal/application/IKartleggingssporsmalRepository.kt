package no.nav.syfo.kartleggingssporsmal.application

import no.nav.syfo.kartleggingssporsmal.domain.JournalpostId
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.shared.domain.Personident
import java.util.*

interface IKartleggingssporsmalRepository {
    suspend fun createStoppunkt(stoppunkt: KartleggingssporsmalStoppunkt): KartleggingssporsmalStoppunkt
    suspend fun getKandidat(personident: Personident): KartleggingssporsmalKandidat?
    suspend fun getKandidat(uuid: UUID): KartleggingssporsmalKandidat?
    suspend fun createKandidatAndMarkStoppunktAsProcessed(
        kandidat: KartleggingssporsmalKandidat,
        stoppunktId: Int,
    ): KartleggingssporsmalKandidat
    suspend fun markStoppunktAsProcessed(stoppunktId: Int)
    suspend fun getUnprocessedStoppunkter(): List<Pair<Int, KartleggingssporsmalStoppunkt>>
    suspend fun updatePublishedAtForKandidat(kandidat: KartleggingssporsmalKandidat): KartleggingssporsmalKandidat
    suspend fun updateVarsletAtForKandidat(kandidat: KartleggingssporsmalKandidat): KartleggingssporsmalKandidat
    fun getNotJournalforteKandidater(): List<KartleggingssporsmalKandidat>
    fun updateJournalpostidForKandidat(kandidat: KartleggingssporsmalKandidat, journalpostId: JournalpostId)
}
