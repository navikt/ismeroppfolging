package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.util.DAYS_IN_WEEK
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
        tilfelleStart: LocalDate,
        tilfelleEnd: LocalDate,
    ) : this(
        uuid = UUID.randomUUID(),
        createdAt = OffsetDateTime.now(),
        personident = personident,
        tilfelleBitReferanseUuid = tilfelleBitReferanseUuid,
        stoppunktAt = calculateStoppunktDato(
            tilfelleStart = tilfelleStart,
            tilfelleEnd = tilfelleEnd,
        ),
        processedAt = null,
    )

    companion object {
        private const val KARTLEGGINGSSPORSMAL_STOPPUNKT_INTERVAL_DAYS = 30L
        internal const val KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS = 6L * DAYS_IN_WEEK
        internal const val KARTLEGGINGSSPORSMAL_STOPPUNKT_END_DAYS =
            KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS + KARTLEGGINGSSPORSMAL_STOPPUNKT_INTERVAL_DAYS

        private fun calculateStoppunktDato(
            tilfelleStart: LocalDate,
            tilfelleEnd: LocalDate,
        ): LocalDate {
            val today = LocalDate.now()
            val stoppunkt = tilfelleStart.plusDays(KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS)
            return if (stoppunkt.isBefore(today) && today.isBefore(tilfelleEnd)) {
                today
            } else {
                stoppunkt
            }
        }

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
