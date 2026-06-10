package no.nav.syfo.kartleggingssporsmal.infrastructure.cronjob

import no.nav.syfo.kartleggingssporsmal.application.IEsyfovarselProducer
import no.nav.syfo.kartleggingssporsmal.application.IKartleggingssporsmalRepository
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.shared.infrastructure.cronjob.Cronjob

class FerdigstillKartleggingssporsmalVarselCronjob(
    private val kartleggingssporsmalRepository: IKartleggingssporsmalRepository,
    private val esyfovarselProducer: IEsyfovarselProducer,
) : Cronjob {

    override val intervalDelayMinutes: Long = 60
    override val initialDelayMinutes: Long = 2

    override suspend fun run(): List<Result<KartleggingssporsmalKandidat>> {
        val kandidater = kartleggingssporsmalRepository.getKandidaterMedSvarUtenFerdigstiltVarsel()
        return kandidater.map { kandidat ->
            esyfovarselProducer.ferdigstillKartleggingssporsmalVarsel(kandidat)
                .map { ferdigstiltKandidat ->
                    val updated = ferdigstiltKandidat.ferdigstillVarsel()
                    kartleggingssporsmalRepository.updateVarselFerdigstiltAtForKandidat(updated)
                    updated
                }
        }
    }
}
