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

    fun createKandidat(
        personident: Personident,
        varselAt: OffsetDateTime? = null,
        varselId: UUID? = null,
    ): SenOppfolgingKandidat {
        val senOppfolgingKandidat = SenOppfolgingKandidat(
            personident = personident,
            varselAt = varselAt,
            varselId = varselId,
        )
        val createdKandidat = senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)

        return createdKandidat
    }

    fun getKandidat(kandidatUuid: UUID): SenOppfolgingKandidat? = senOppfolgingRepository.getKandidat(kandidatUuid = kandidatUuid)

    fun findKandidatFromVarselId(varselId: UUID): SenOppfolgingKandidat? =
        senOppfolgingRepository.findKandidatFromVarselId(varselId = varselId)

    fun findRecentKandidatFromPersonIdent(personident: Personident): SenOppfolgingKandidat? {
        return getKandidater(personident).firstOrNull()?.takeIf { kandidat ->
            kandidat.createdAt > OffsetDateTime.now().minusMonths(3)
        }
    }

    fun addSvar(kandidat: SenOppfolgingKandidat, svarAt: OffsetDateTime, onskerOppfolging: OnskerOppfolging): SenOppfolgingKandidat {
        val svar = SenOppfolgingSvar(svarAt = svarAt, onskerOppfolging = onskerOppfolging)
        val kandidatWithSvar = kandidat.addSvar(svar = svar)

        senOppfolgingRepository.updateKandidatSvar(senOppfolgingSvar = svar, senOppfolgingKandidaUuid = kandidatWithSvar.uuid)

        return kandidatWithSvar
    }

    fun vurderKandidat(
        kandidat: SenOppfolgingKandidat,
        veilederident: String,
        begrunnelse: String?,
        type: VurderingType,
    ): SenOppfolgingKandidat {
        if (type == VurderingType.FERDIGBEHANDLET && kandidat.isFerdigbehandlet()) {
            throw ConflictException("Kandidat med uuid ${kandidat.uuid} er allerede ferdigbehandlet")
        }

        val vurdering = SenOppfolgingVurdering(
            veilederident = veilederident,
            begrunnelse = begrunnelse,
            type = type,
        )
        val updatedKandidat = kandidat.addVurdering(vurdering = vurdering).also {
            senOppfolgingRepository.addVurdering(it, vurdering)
        }

        return updatedKandidat
    }

    fun publishUnpublishedKandidatStatus(): List<Result<SenOppfolgingKandidat>> {
        val unpublishedKandidatStatuser = senOppfolgingRepository.getUnpublishedKandidatStatuser()
        return unpublishedKandidatStatuser
            .filter { kandidat -> kandidat.svar != null || kandidat.isVarsletForMinstTiDagerSiden() }
            .map { kandidat ->
                kandidatStatusProducer.send(kandidat = kandidat).map {
                    val latestVurdering = it.getLatestVurdering()
                    if (it.publishedAt == null) {
                        senOppfolgingRepository.setKandidatPublished(it.uuid)
                    } else if (latestVurdering != null && latestVurdering.publishedAt == null) {
                        senOppfolgingRepository.setVurderingPublished(latestVurdering.uuid)
                    }
                    it
                }
            }
    }

    fun getKandidater(personident: Personident): List<SenOppfolgingKandidat> =
        senOppfolgingRepository.getKandidater(personident = personident)
}
