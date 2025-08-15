package no.nav.syfo.kartleggingssporsmal.infrastructure.database

import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.shared.domain.Personident
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class PKartleggingssporsmalStoppunkt(
    val id: Int,
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val personident: Personident,
    val tilfelleBitReferanseUuid: UUID,
    val stoppunktAt: LocalDate,
    val processedAt: OffsetDateTime?,
) {
    fun toKartleggingssporsmalStoppunkt(): KartleggingssporsmalStoppunkt {
        return KartleggingssporsmalStoppunkt.createFromDatabase(
            uuid = uuid,
            createdAt = createdAt,
            personident = personident,
            tilfelleBitReferanseUuid = tilfelleBitReferanseUuid,
            stoppunktAt = stoppunktAt,
            processedAt = processedAt,
        )
    }
}
