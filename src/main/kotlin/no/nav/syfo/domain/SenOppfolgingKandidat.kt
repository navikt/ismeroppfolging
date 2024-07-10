package no.nav.syfo.domain

import java.time.OffsetDateTime
import java.util.*

data class SenOppfolgingKandidat private constructor(
    val uuid: UUID,
    val personident: Personident,
    val createdAt: OffsetDateTime,
    val varselAt: OffsetDateTime,
    val svar: SenOppfolgingSvar?,
    val status: SenOppfolgingStatus,
    val vurderinger: List<SenOppfolgingVurdering>,
) {
    constructor(
        personident: Personident,
        varselAt: OffsetDateTime,
    ) : this(
        uuid = UUID.randomUUID(),
        personident = personident,
        createdAt = OffsetDateTime.now(),
        varselAt = varselAt,
        svar = null,
        status = SenOppfolgingStatus.KANDIDAT,
        vurderinger = emptyList(),
    )

    fun addSvar(svar: SenOppfolgingSvar): SenOppfolgingKandidat = this.copy(
        svar = svar,
    )

    fun addVurdering(vurdering: SenOppfolgingVurdering): SenOppfolgingKandidat = this.copy(
        status = vurdering.status,
        vurderinger = listOf(vurdering) + this.vurderinger,
    )

    fun isFerdigbehandlet(): Boolean = status == SenOppfolgingStatus.FERDIGBEHANDLET

    companion object {
        fun createFromDatabase(
            uuid: UUID,
            personident: Personident,
            createdAt: OffsetDateTime,
            varselAt: OffsetDateTime,
            svar: SenOppfolgingSvar?,
            status: String,
            vurderinger: List<SenOppfolgingVurdering>,
        ): SenOppfolgingKandidat = SenOppfolgingKandidat(
            uuid = uuid,
            personident = personident,
            createdAt = createdAt,
            varselAt = varselAt,
            svar = svar,
            status = SenOppfolgingStatus.valueOf(status),
            vurderinger = vurderinger,
        )
    }
}
