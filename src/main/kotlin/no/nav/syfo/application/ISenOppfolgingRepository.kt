package no.nav.syfo.application

import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.SenOppfolgingKandidat
import no.nav.syfo.domain.SenOppfolgingSvar
import no.nav.syfo.domain.SenOppfolgingVurdering
import java.util.*

interface ISenOppfolgingRepository {
    fun createKandidat(senOppfolgingKandidat: SenOppfolgingKandidat): SenOppfolgingKandidat
    fun updateKandidatSvar(senOppfolgingSvar: SenOppfolgingSvar, senOppfolgingKandidaUuid: UUID)
    fun addVurdering(senOppfolgingKandidat: SenOppfolgingKandidat, vurdering: SenOppfolgingVurdering)

    fun getKandidat(kandidatUuid: UUID): SenOppfolgingKandidat?
    fun getUnpublishedKandidater(): List<SenOppfolgingKandidat>
    fun getKandidater(personident: Personident): List<SenOppfolgingKandidat>
    fun getKandidaterWithUnpublishedVurderinger(): List<SenOppfolgingKandidat>
    fun setKandidatPublished(kandidatUuid: UUID)
    fun setVurderingPublished(vurderingUuid: UUID)
}
