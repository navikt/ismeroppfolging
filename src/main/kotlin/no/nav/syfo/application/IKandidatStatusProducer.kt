package no.nav.syfo.application

import no.nav.syfo.domain.SenOppfolgingKandidat

interface IKandidatStatusProducer {
    fun send(kandidatStatus: SenOppfolgingKandidat): Result<SenOppfolgingKandidat>
}
