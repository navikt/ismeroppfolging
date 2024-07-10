package no.nav.syfo.domain

import no.nav.syfo.util.nowUTC
import java.time.OffsetDateTime
import java.util.UUID

data class SenOppfolgingVurdering(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val veilederident: String,
    val status: SenOppfolgingStatus,
) {
    constructor(
        veilederident: String,
        status: SenOppfolgingStatus,
    ) : this(
        uuid = UUID.randomUUID(),
        createdAt = nowUTC(),
        veilederident = veilederident,
        status = status,
    )

    companion object {
        fun createFromDatabase(
            uuid: UUID,
            createdAt: OffsetDateTime,
            veilederident: String,
            status: SenOppfolgingStatus,
        ) = SenOppfolgingVurdering(
            uuid = uuid,
            createdAt = createdAt,
            veilederident = veilederident,
            status = status,
        )
    }
}
