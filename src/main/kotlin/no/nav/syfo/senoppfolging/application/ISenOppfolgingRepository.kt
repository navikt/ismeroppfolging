package no.nav.syfo.senoppfolging.application

import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.senoppfolging.domain.SenOppfolgingKandidat
import no.nav.syfo.senoppfolging.domain.SenOppfolgingSvar
import no.nav.syfo.senoppfolging.domain.SenOppfolgingVurdering
import java.util.*

interface ISenOppfolgingRepository {
    fun createKandidat(senOppfolgingKandidat: SenOppfolgingKandidat): SenOppfolgingKandidat
    fun updateKandidatSvar(senOppfolgingSvar: SenOppfolgingSvar, senOppfolgingKandidaUuid: UUID)
    fun vurderKandidat(senOppfolgingKandidat: SenOppfolgingKandidat, vurdering: SenOppfolgingVurdering): SenOppfolgingVurdering

    fun getKandidat(kandidatUuid: UUID): SenOppfolgingKandidat?
    fun updateKandidatPersonident(kandidater: List<SenOppfolgingKandidat>, newPersonident: Personident)

    fun findKandidatFromVarselId(varselId: UUID): SenOppfolgingKandidat?

    fun getUnpublishedKandidatStatuser(): List<SenOppfolgingKandidat>
    fun getKandidater(personident: Personident): List<SenOppfolgingKandidat>
    fun setKandidatPublished(kandidatUuid: UUID)
    fun setVurderingPublished(vurderingUuid: UUID)

    fun getKandidaterForPersoner(personidenter: List<Personident>): Map<Personident, SenOppfolgingKandidat>
}
