package no.nav.syfo.application

import no.nav.syfo.domain.SenOppfolgingKandidat
import no.nav.syfo.domain.SenOppfolgingSvar
import java.util.*

interface ISenOppfolgingRepository {
    fun createKandidat(senOppfolgingKandidat: SenOppfolgingKandidat): SenOppfolgingKandidat
    fun updateKandidatSvar(senOppfolgingSvar: SenOppfolgingSvar, senOppfolgingKandidaUuid: UUID)

    fun getUnpublishedKandidater(): List<SenOppfolgingKandidat>

    fun setPublished(kandidatUuid: UUID)
}
