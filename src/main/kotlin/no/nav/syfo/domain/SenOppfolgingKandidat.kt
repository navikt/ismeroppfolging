package no.nav.syfo.domain

import no.nav.syfo.util.nowUTC
import no.nav.syfo.util.isMoreThanDaysAgo
import java.time.OffsetDateTime
import java.util.*

data class SenOppfolgingKandidat private constructor(
    val uuid: UUID,
    val personident: Personident,
    val createdAt: OffsetDateTime,
    val varselAt: OffsetDateTime?,
    val varselId: UUID?,
    val svar: SenOppfolgingSvar?,
    val status: SenOppfolgingStatus,
    val publishedAt: OffsetDateTime?,
    val vurderinger: List<SenOppfolgingVurdering>,
) {
    constructor(
        personident: Personident,
        varselAt: OffsetDateTime?,
        varselId: UUID? = null,
    ) : this(
        uuid = UUID.randomUUID(),
        personident = personident,
        createdAt = nowUTC(),
        varselAt = varselAt,
        varselId = varselId,
        svar = null,
        status = SenOppfolgingStatus.KANDIDAT,
        publishedAt = null,
        vurderinger = emptyList(),
    )

    fun addSvar(svar: SenOppfolgingSvar): SenOppfolgingKandidat = this.copy(
        svar = svar,
    )

    fun addVurdering(vurdering: SenOppfolgingVurdering): SenOppfolgingKandidat = this.copy(
        status = SenOppfolgingStatus.from(vurdering.type),
        vurderinger = listOf(vurdering) + this.vurderinger,
    )

    fun isFerdigbehandlet(): Boolean = status == SenOppfolgingStatus.FERDIGBEHANDLET

    fun getLatestVurdering(): SenOppfolgingVurdering? = vurderinger.maxByOrNull { it.createdAt }

    fun isVarsletForMinstTiDagerSiden() = varselAt != null && varselAt isMoreThanDaysAgo 10

    companion object {
        fun createFromDatabase(
            uuid: UUID,
            personident: Personident,
            createdAt: OffsetDateTime,
            varselAt: OffsetDateTime?,
            varselId: UUID?,
            svar: SenOppfolgingSvar?,
            status: SenOppfolgingStatus,
            publishedAt: OffsetDateTime?,
            vurderinger: List<SenOppfolgingVurdering>,
        ): SenOppfolgingKandidat = SenOppfolgingKandidat(
            uuid = uuid,
            personident = personident,
            createdAt = createdAt,
            varselAt = varselAt,
            varselId = varselId,
            svar = svar,
            status = status,
            publishedAt = publishedAt,
            vurderinger = vurderinger,
        )
    }
}
