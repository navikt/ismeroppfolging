package no.nav.syfo.application

import no.nav.syfo.domain.SenOppfolgingKandidat
import no.nav.syfo.domain.SenOppfolgingVurdering

interface IKandidatStatusProducer {
    fun sendKandidat(kandidat: SenOppfolgingKandidat): Result<SenOppfolgingKandidat>
    fun sendVurdering(vurdering: SenOppfolgingVurdering, kandidat: SenOppfolgingKandidat): Result<SenOppfolgingKandidat>
}
