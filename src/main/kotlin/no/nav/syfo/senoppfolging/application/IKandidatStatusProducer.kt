package no.nav.syfo.senoppfolging.application

import no.nav.syfo.senoppfolging.domain.SenOppfolgingKandidat

interface IKandidatStatusProducer {
    fun send(kandidat: SenOppfolgingKandidat): Result<SenOppfolgingKandidat>
}
