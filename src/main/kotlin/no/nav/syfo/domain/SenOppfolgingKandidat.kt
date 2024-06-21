package no.nav.syfo.domain

import no.nav.syfo.util.nowUTC
import java.time.OffsetDateTime
import java.util.*

data class SenOppfolgingKandidat private constructor(
    val uuid: UUID,
    val personident: Personident,
    val createdAt: OffsetDateTime,
    val varselAt: OffsetDateTime,
    val svar: SenOppfolgingSvar?,
    val publishedAt: OffsetDateTime?,
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
        publishedAt = null,
    )

    fun addSvar(svar: SenOppfolgingSvar): SenOppfolgingKandidat = this.copy(
        svar = svar,
    )

    fun publish(): SenOppfolgingKandidat = this.copy(publishedAt = nowUTC())

    companion object {
        fun createFromDatabase(
            uuid: UUID,
            personident: Personident,
            createdAt: OffsetDateTime,
            varselAt: OffsetDateTime,
            svar: SenOppfolgingSvar?,
            publishedAt: OffsetDateTime?
        ): SenOppfolgingKandidat = SenOppfolgingKandidat(
            uuid = uuid,
            personident = personident,
            createdAt = createdAt,
            varselAt = varselAt,
            svar = svar,
            publishedAt = publishedAt,
        )
    }
}
