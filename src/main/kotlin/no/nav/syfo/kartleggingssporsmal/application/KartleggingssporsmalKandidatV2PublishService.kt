package no.nav.syfo.kartleggingssporsmal.application

import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import org.slf4j.LoggerFactory

class KartleggingssporsmalKandidatV2PublishService(
    private val kartleggingssporsmalRepository: IKartleggingssporsmalRepository,
    private val kandidatV2Producer: IKartleggingssporsmalKandidatV2Producer,
    private val isKandidatV2PublishingEnabled: Boolean,
) {

    // Publiserer i created_at-rekkefølge og stopper ved første feil for å bevare rekkefølgen per person
    fun publishStatusendringer(): List<Result<KartleggingssporsmalKandidat>> {
        if (!isKandidatV2PublishingEnabled) {
            log.info("Kandidat v2 publishing is disabled, skipping")
            return emptyList()
        }
        val results = mutableListOf<Result<KartleggingssporsmalKandidat>>()
        for (kandidat in kartleggingssporsmalRepository.getKandidaterWithStatusendringNotPublishedV2()) {
            val result = kandidatV2Producer.send(kandidat).mapCatching {
                kartleggingssporsmalRepository.updatePublishedV2AtForKandidatStatusendring(kandidat)
                kandidat
            }
            results.add(result)
            if (result.isFailure) break
        }
        val antallPubliserteStatusendringer = results.count { it.isSuccess }
        if (antallPubliserteStatusendringer >= 1) {
            log.info(
                "Published {} kartleggingssporsmal kandidat v2 statusendringer to topic {}",
                antallPubliserteStatusendringer,
                KARTLEGGINGSSPORSMAL_KANDIDAT_V2_TOPIC,
            )
        }
        return results
    }

    companion object {
        private const val KARTLEGGINGSSPORSMAL_KANDIDAT_V2_TOPIC = "teamsykefravr.ismeroppfolging-kartleggingssporsmal-kandidat-v2"
        private val log = LoggerFactory.getLogger(KartleggingssporsmalKandidatV2PublishService::class.java)
    }
}
