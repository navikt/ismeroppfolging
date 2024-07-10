package no.nav.syfo.application

import no.nav.syfo.domain.*
import no.nav.syfo.infrastructure.kafka.KandidatStatusProducer
import java.time.OffsetDateTime

class SenOppfolgingService(
    private val senOppfolgingRepository: ISenOppfolgingRepository,
    private val kandidatStatusProducer: KandidatStatusProducer,
) {

    fun createKandidat(personident: Personident, varselAt: OffsetDateTime): SenOppfolgingKandidat {
        val senOppfolgingKandidat = SenOppfolgingKandidat(
            personident = personident,
            varselAt = varselAt,
        )
        val createdKandidat = senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)

        return createdKandidat
    }

    fun addSvar(kandidat: SenOppfolgingKandidat, svarAt: OffsetDateTime, onskerOppfolging: OnskerOppfolging): SenOppfolgingKandidat {
        val svar = SenOppfolgingSvar(svarAt = svarAt, onskerOppfolging = onskerOppfolging)
        val kandidatWithSvar = kandidat.addSvar(svar = svar)

        senOppfolgingRepository.updateKandidatSvar(senOppfolgingSvar = svar, senOppfolgingKandidaUuid = kandidatWithSvar.uuid)

        return kandidatWithSvar
    }

    fun ferdigbehandleKandidat(kandidat: SenOppfolgingKandidat, veilederident: String): SenOppfolgingKandidat {
        val vurdering = SenOppfolgingVurdering(
            veilederident = veilederident,
            status = SenOppfolgingStatus.FERDIGBEHANDLET,
        )
        val ferdigbehandletKandidat = kandidat.addVurdering(vurdering = vurdering).also {
            senOppfolgingRepository.addVurdering(it, vurdering)
        }

        return ferdigbehandletKandidat
    }

    fun publishUnpublishedKandidatStatus(): List<Result<SenOppfolgingKandidat>> {
        val unpublished = senOppfolgingRepository.getUnpublishedKandidater()
        return unpublished.map { kandidat ->
            kandidatStatusProducer.send(kandidatStatus = kandidat)
                .map {
                    senOppfolgingRepository.setPublished(it.uuid)
                    it
                }
        }
    }
}
