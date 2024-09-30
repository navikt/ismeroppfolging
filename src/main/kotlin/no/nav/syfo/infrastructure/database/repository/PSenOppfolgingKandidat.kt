package no.nav.syfo.infrastructure.database.repository

import no.nav.syfo.domain.OnskerOppfolging
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.SenOppfolgingKandidat
import no.nav.syfo.domain.SenOppfolgingStatus
import no.nav.syfo.domain.SenOppfolgingSvar
import java.time.OffsetDateTime
import java.util.*

data class PSenOppfolgingKandidat(
    val id: Int,
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val personident: Personident,
    val varselAt: OffsetDateTime?,
    val svarAt: OffsetDateTime?,
    val onskerOppfolging: String?,
    val publishedAt: OffsetDateTime?,
    val status: SenOppfolgingStatus,
) {
    fun toSenOppfolgingKandidat(vurderinger: List<PSenOppfolgingVurdering>): SenOppfolgingKandidat {
        val svar = if (svarAt != null && onskerOppfolging != null) SenOppfolgingSvar.createFromDatabase(
            svarAt = svarAt,
            onskerOppfolging = OnskerOppfolging.valueOf(onskerOppfolging)
        ) else null

        return SenOppfolgingKandidat.createFromDatabase(
            uuid = uuid,
            personident = personident,
            createdAt = createdAt,
            varselAt = varselAt,
            svar = svar,
            status = status,
            publishedAt = publishedAt,
            vurderinger = vurderinger.map { it.toSenOppfolgingVurdering() },
        )
    }
}
