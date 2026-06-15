package no.nav.syfo.kartleggingssporsmal.infrastructure.cronjob

import no.nav.syfo.kartleggingssporsmal.application.KartleggingssporsmalService
import no.nav.syfo.shared.infrastructure.cronjob.Cronjob
import org.slf4j.LoggerFactory

class KandidatStoppunktCronjob(
    private val kartleggingssporsmalService: KartleggingssporsmalService,
) : Cronjob {

    override val intervalDelayMinutes: Long = 5
    override val initialDelayMinutes: Long = 2

    override suspend fun run(): List<Result<Any>> {
        val stoppunktResults = kartleggingssporsmalService.processStoppunkter()
        val recoveryResults = kartleggingssporsmalService.processKandidaterWithMissingPublishOrVarsel()
        if (recoveryResults.isNotEmpty()) {
            val failed = recoveryResults.filter { it.isFailure }
            val success = recoveryResults.filter { it.isSuccess }
            log.info("Processed kandidater with missing publish or varsel. Failed: $failed, success: $success")
        }
        return stoppunktResults
    }

    companion object {
        private val log = LoggerFactory.getLogger(KandidatStoppunktCronjob::class.java)
    }
}
