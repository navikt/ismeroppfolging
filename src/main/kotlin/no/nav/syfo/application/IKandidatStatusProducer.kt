package no.nav.syfo.application

import no.nav.syfo.domain.SenOppfolgingKandidat

interface IKandidatStatusProducer {
    fun send(kandidat: SenOppfolgingKandidat): Result<SenOppfolgingKandidat>
}
