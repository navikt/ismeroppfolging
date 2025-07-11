package no.nav.syfo.senoppfolging.infrastructure.cronjob

import no.nav.syfo.senoppfolging.application.SenOppfolgingService
import no.nav.syfo.shared.infrastructure.cronjob.Cronjob

class PublishKandidatStatusCronjob(
    private val senOppfolgingService: SenOppfolgingService,
) : Cronjob {
    override val initialDelayMinutes: Long = 5
    override val intervalDelayMinutes: Long = 1

    override suspend fun run() = senOppfolgingService.publishUnpublishedKandidatStatus()
}
