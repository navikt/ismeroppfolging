package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.shared.domain.Personident
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class KartleggingssporsmalStoppunkt private constructor(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val personident: Personident,
    val tilfelleBitReferanseUuid: UUID,
    val stoppunktAt: LocalDate,
    val processedAt: OffsetDateTime?,
) {
    constructor(
        personident: Personident,
        tilfelleBitReferanseUuid: UUID,
        stoppunktAt: LocalDate,
    ) : this(
        uuid = UUID.randomUUID(),
        createdAt = OffsetDateTime.now(),
        personident = personident,
        tilfelleBitReferanseUuid = tilfelleBitReferanseUuid,
        stoppunktAt = stoppunktAt,
        processedAt = null,
    )

    companion object {
        fun createFromDatabase(
            uuid: UUID,
            createdAt: OffsetDateTime,
            personident: Personident,
            tilfelleBitReferanseUuid: UUID,
            stoppunktAt: LocalDate,
            processedAt: OffsetDateTime?,
        ): KartleggingssporsmalStoppunkt {
            return KartleggingssporsmalStoppunkt(
                uuid = uuid,
                createdAt = createdAt,
                personident = personident,
                tilfelleBitReferanseUuid = tilfelleBitReferanseUuid,
                stoppunktAt = stoppunktAt,
                processedAt = processedAt,
            )
        }
    }
}
