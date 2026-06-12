package no.nav.syfo.kartleggingssporsmal.infrastructure.cronjob

import no.nav.syfo.kartleggingssporsmal.application.KartleggingssporsmalService
import no.nav.syfo.shared.infrastructure.cronjob.Cronjob

class KandidatStoppunktCronjob(
    private val kartleggingssporsmalService: KartleggingssporsmalService,
) : Cronjob {

    override val intervalDelayMinutes: Long = 5
    override val initialDelayMinutes: Long = 2

    override suspend fun run(): List<Result<Any>> {
        val stoppunktResults = kartleggingssporsmalService.processStoppunkter()
        val recoveryResults = kartleggingssporsmalService.processKandidaterWithMissingPublishOrVarsel()
        return stoppunktResults + recoveryResults
    }
}
