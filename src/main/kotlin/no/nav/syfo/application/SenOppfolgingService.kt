package no.nav.syfo.application

import no.nav.syfo.domain.OnskerOppfolging
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.SenOppfolgingKandidat
import no.nav.syfo.domain.SenOppfolgingSvar
import java.time.OffsetDateTime

class SenOppfolgingService(private val senOppfolgingRepository: ISenOppfolgingRepository) {

    fun createKandidat(personident: Personident, varselAt: OffsetDateTime): SenOppfolgingKandidat {
        val senOppfolgingKandidat = SenOppfolgingKandidat(
            personident = personident,
            varselAt = varselAt,
        )
        val createdKandidat = senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)

        return createdKandidat
    }

    fun addSvar(kandidat: SenOppfolgingKandidat, svarAt: OffsetDateTime, onskerOppfolging: OnskerOppfolging): SenOppfolgingKandidat {
        val svar = SenOppfolgingSvar(svarAt = svarAt, onskerOppfolging = onskerOppfolging)
        val kandidatWithSvar = kandidat.addSvar(svar = svar)

        senOppfolgingRepository.updateKandidatSvar(senOppfolgingSvar = svar, senOppfolgingKandidaUuid = kandidatWithSvar.uuid)

        return kandidatWithSvar
    }
}
