package no.nav.syfo.infrastructure.cronjob

import no.nav.syfo.application.SenOppfolgingService

class PublishKandidatStatusCronjob(
    private val senOppfolgingService: SenOppfolgingService,
) : Cronjob {
    override val initialDelayMinutes: Long = 5
    override val intervalDelayMinutes: Long = 1

    override suspend fun run() = senOppfolgingService.publishUnpublishedKandidatStatus()
}
