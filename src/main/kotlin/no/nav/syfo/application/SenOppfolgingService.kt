package no.nav.syfo.application

import no.nav.syfo.application.exception.ConflictException
import no.nav.syfo.domain.*
import no.nav.syfo.infrastructure.kafka.KandidatStatusProducer
import java.time.OffsetDateTime
import java.util.*

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

    fun getKandidat(kandidatUuid: UUID): SenOppfolgingKandidat? = senOppfolgingRepository.getKandidat(kandidatUuid = kandidatUuid)

    fun addSvar(kandidat: SenOppfolgingKandidat, svarAt: OffsetDateTime, onskerOppfolging: OnskerOppfolging): SenOppfolgingKandidat {
        val svar = SenOppfolgingSvar(svarAt = svarAt, onskerOppfolging = onskerOppfolging)
        val kandidatWithSvar = kandidat.addSvar(svar = svar)

        senOppfolgingRepository.updateKandidatSvar(senOppfolgingSvar = svar, senOppfolgingKandidaUuid = kandidatWithSvar.uuid)

        return kandidatWithSvar
    }

    fun vurderKandidat(kandidat: SenOppfolgingKandidat, veilederident: String, type: VurderingType): SenOppfolgingKandidat {
        if (type == VurderingType.FERDIGBEHANDLET && kandidat.isFerdigbehandlet()) {
            throw ConflictException("Kandidat med uuid ${kandidat.uuid} er allerede ferdigbehandlet")
        }

        val vurdering = SenOppfolgingVurdering(
            veilederident = veilederident,
            type = type,
        )
        val updatedKandidat = kandidat.addVurdering(vurdering = vurdering).also {
            senOppfolgingRepository.addVurdering(it, vurdering)
        }

        return updatedKandidat
    }

    fun publishUnpublishedKandidatStatus(): List<Result<SenOppfolgingKandidat>> {
        val unpublishedKandidater = senOppfolgingRepository.getUnpublishedKandidater()
        val publishedKandidater = unpublishedKandidater.map { kandidat ->
            kandidatStatusProducer.sendKandidat(kandidat = kandidat)
                .map {
                    senOppfolgingRepository.setKandidatPublished(it.uuid)
                    it
                }
        }
        val unpublishedVurderinger = senOppfolgingRepository.getUnpublishedVurderinger()
        val publishedVurderinger = unpublishedVurderinger.map { (kandidat, vurdering) ->
            kandidatStatusProducer.sendVurdering(vurdering = vurdering, kandidat = kandidat)
                .map {
                    senOppfolgingRepository.setVurderingPublished(vurdering.uuid)
                    it
                }
        }

        return publishedKandidater + publishedVurderinger
    }

    fun getKandidater(personident: Personident): List<SenOppfolgingKandidat> = senOppfolgingRepository.getKandidater(personident = personident)
}
