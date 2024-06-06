package no.nav.syfo.application

import no.nav.syfo.domain.OnskerOppfolging
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.SenOppfolgingKandidat
import no.nav.syfo.domain.SenOppfolgingSvar
import java.time.OffsetDateTime

class SenOppfolgingService {

    fun createKandidat(personident: Personident, varselAt: OffsetDateTime): SenOppfolgingKandidat {
        val senOppfolgingKandidat = SenOppfolgingKandidat(
            personident = personident,
            varselAt = varselAt,
        )

        // TODO: Store in db

        return senOppfolgingKandidat
    }

    fun addSvar(kandidat: SenOppfolgingKandidat, svarAt: OffsetDateTime, onskerOppfolging: OnskerOppfolging): SenOppfolgingKandidat {
        val svar = SenOppfolgingSvar(svarAt = svarAt, onskerOppfolging = onskerOppfolging)
        val kandidatWithSvar = kandidat.addSvar(svar = svar)

        // TODO: Store svar and update kandidat in db

        return kandidatWithSvar
    }
}
