package no.nav.syfo.kartleggingssporsmal.infrastructure.cronjob

import no.nav.syfo.kartleggingssporsmal.application.KartleggingssporsmalKandidatV2PublishService
import no.nav.syfo.shared.infrastructure.cronjob.Cronjob

class PublishKartleggingssporsmalKandidatV2Cronjob(
    private val publishService: KartleggingssporsmalKandidatV2PublishService,
) : Cronjob {

    override val initialDelayMinutes: Long = 4
    override val intervalDelayMinutes: Long = 1

    override suspend fun run(): List<Result<Any>> = publishService.publishStatusendringer()
}
