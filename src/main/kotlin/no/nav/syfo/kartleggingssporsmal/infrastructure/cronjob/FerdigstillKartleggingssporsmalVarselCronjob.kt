package no.nav.syfo.kartleggingssporsmal.infrastructure.cronjob

import no.nav.syfo.kartleggingssporsmal.application.IEsyfovarselProducer
import no.nav.syfo.kartleggingssporsmal.application.IKartleggingssporsmalRepository
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.shared.infrastructure.cronjob.Cronjob
import java.util.*

class FerdigstillKartleggingssporsmalVarselCronjob(
    private val kartleggingssporsmalRepository: IKartleggingssporsmalRepository,
    private val esyfovarselProducer: IEsyfovarselProducer,
) : Cronjob {

    override val intervalDelayMinutes: Long = 10_000
    override val initialDelayMinutes: Long = 2

    override suspend fun run(): List<Result<KartleggingssporsmalKandidat>> {
        return KANDIDAT_UUIDS.map { uuid ->
            val kandidat = kartleggingssporsmalRepository.getKandidat(UUID.fromString(uuid))
            kandidat?.let {
                esyfovarselProducer.ferdigstillKartleggingssporsmalVarsel(kandidat)
            } ?: throw RuntimeException("Fant ikke kandidat for manuell ferdigstilling av varsel, uuid: $uuid")
        }
    }

    companion object {
        private val KANDIDAT_UUIDS: List<String> = listOf("78114ced-de49-463b-a87d-08f3a0734a08")
    }
}
