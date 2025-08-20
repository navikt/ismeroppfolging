package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.util.DAYS_IN_WEEK
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class KartleggingssporsmalStoppunkt private constructor(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val personident: Personident,
    val tilfelleBitReferanseUuid: UUID,
    val stoppunktAt: LocalDate,
    val processedAt: OffsetDateTime?,
) {

    companion object {
        private const val KARTLEGGINGSSPORSMAL_STOPPUNKT_INTERVAL_DAYS = 30L
        internal const val KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS = 6L * DAYS_IN_WEEK
        internal const val KARTLEGGINGSSPORSMAL_STOPPUNKT_END_DAYS =
            KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS + KARTLEGGINGSSPORSMAL_STOPPUNKT_INTERVAL_DAYS

        fun create(
            oppfolgingstilfelle: Oppfolgingstilfelle,
        ): KartleggingssporsmalStoppunkt? {
            return if (oppfolgingstilfelleWillGenerateStoppunkt(oppfolgingstilfelle)) {
                KartleggingssporsmalStoppunkt(
                    uuid = UUID.randomUUID(),
                    createdAt = OffsetDateTime.now(),
                    personident = oppfolgingstilfelle.personident,
                    tilfelleBitReferanseUuid = oppfolgingstilfelle.tilfelleBitReferanseUuid,
                    stoppunktAt = calculateStoppunktDato(
                        tilfelleStart = oppfolgingstilfelle.tilfelleStart,
                        tilfelleEnd = oppfolgingstilfelle.tilfelleEnd,
                    ),
                    processedAt = null,
                )
            } else {
                null
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

        private fun oppfolgingstilfelleWillGenerateStoppunkt(oppfolgingstilfelle: Oppfolgingstilfelle): Boolean {
            return oppfolgingstilfelleInsideStoppunktInterval(oppfolgingstilfelle) &&
                !oppfolgingstilfelle.isDod() &&
                !oppfolgingstilfelle.hasTilfelleWithEndMoreThanThirtyDaysAgo()
        }

        private fun oppfolgingstilfelleInsideStoppunktInterval(oppfolgingstilfelle: Oppfolgingstilfelle): Boolean {
            // TODO: Hva med de som er før start-tidspunkt på intervallet, og så får en veldig lang sykmelding som strekker seg over intervallet?
            return oppfolgingstilfelle.durationInDays() >= KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS &&
                oppfolgingstilfelle.durationInDays() <= KARTLEGGINGSSPORSMAL_STOPPUNKT_END_DAYS
        }

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
    }
}
