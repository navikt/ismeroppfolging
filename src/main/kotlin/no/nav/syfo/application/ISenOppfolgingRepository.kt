package no.nav.syfo.application

import no.nav.syfo.domain.SenOppfolgingKandidat
import no.nav.syfo.domain.SenOppfolgingSvar
import no.nav.syfo.domain.SenOppfolgingVurdering
import java.util.*

interface ISenOppfolgingRepository {
    fun createKandidat(senOppfolgingKandidat: SenOppfolgingKandidat): SenOppfolgingKandidat
    fun updateKandidatSvar(senOppfolgingSvar: SenOppfolgingSvar, senOppfolgingKandidaUuid: UUID)
    fun addVurdering(senOppfolgingKandidat: SenOppfolgingKandidat, vurdering: SenOppfolgingVurdering)

    fun getUnpublishedKandidater(): List<SenOppfolgingKandidat>
    fun setPublished(kandidatUuid: UUID)
}
