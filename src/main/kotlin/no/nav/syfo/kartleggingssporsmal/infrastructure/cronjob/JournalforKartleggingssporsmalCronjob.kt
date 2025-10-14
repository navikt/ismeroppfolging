package no.nav.syfo.kartleggingssporsmal.infrastructure.cronjob

import no.nav.syfo.kartleggingssporsmal.application.IKartleggingssporsmalRepository
import no.nav.syfo.kartleggingssporsmal.infrastructure.journalforing.JournalforingService
import no.nav.syfo.shared.infrastructure.cronjob.Cronjob

class JournalforKartleggingssporsmalCronjob(
    private val kartleggingssporsmalRepository: IKartleggingssporsmalRepository,
    private val journalforingService: JournalforingService,
) : Cronjob {

    override val initialDelayMinutes: Long = 3
    override val intervalDelayMinutes: Long = 2

    override suspend fun run(): List<Result<Any>> {
        val notJournalforteKartleggingssporsmal = kartleggingssporsmalRepository.getNotJournalforteKandidater()
        return notJournalforteKartleggingssporsmal.map { kandidat ->
            journalforingService.journalfor(kandidat).also { result ->
                if (result.isSuccess) {
                    kartleggingssporsmalRepository.updateJournalpostidForKandidat(
                        kandidat = kandidat,
                        journalpostId = result.getOrThrow(),
                    )
                }
            }
        }
    }
}
