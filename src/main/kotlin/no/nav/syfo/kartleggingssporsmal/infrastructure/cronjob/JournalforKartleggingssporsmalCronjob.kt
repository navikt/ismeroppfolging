package no.nav.syfo.kartleggingssporsmal.infrastructure.cronjob

import no.nav.syfo.kartleggingssporsmal.infrastructure.journalforing.JournalforingService
import no.nav.syfo.shared.infrastructure.cronjob.Cronjob

class JournalforKartleggingssporsmalCronjob(
    private val journalforingService: JournalforingService,
) : Cronjob {

    override val initialDelayMinutes: Long = 3
    override val intervalDelayMinutes: Long = 2

    override suspend fun run() = journalforingService.journalforKandidater()
}
