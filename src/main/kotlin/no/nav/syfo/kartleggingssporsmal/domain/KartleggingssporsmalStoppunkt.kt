package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.util.DAYS_IN_WEEK
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.min

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
            oppfolgingstilfelle: Oppfolgingstilfelle.OppfolgingstilfelleFromKafka,
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

        private fun oppfolgingstilfelleWillGenerateStoppunkt(oppfolgingstilfelle: Oppfolgingstilfelle.OppfolgingstilfelleFromKafka): Boolean {
            return oppfolgingstilfelleInsideStoppunktInterval(oppfolgingstilfelle) &&
                !oppfolgingstilfelle.isDod() &&
                !oppfolgingstilfelle.hasTilfelleWithEndMoreThanThirtyDaysAgo()
        }

        /**
         * Funksjonen skal oppdage om et oppfolgingstilfelle er innenfor et gitt intervall.
         * På grunn av oppfølgingstilfellers natur der det kan komme inn informasjon på ulike tidspunkt både frem og tilbake i tid, må vi håndtere flere edge-caser.
         * Dekker tre caser:
         * - Det kommer inn informasjon som dytter duration inn i intervallet ila perioden: hasDurationInsideInterval
         * - Det kommer inn informasjon som dytter duration forbi intervall-slutt, men vi er fortsatt ikke forbi stoppunkttidspunktet i dag (typisk lang sykmelding): willHaveSykedagInIntervalDuringThisPeriod
         * - Det kommer inn informasjon som sier at duration frem til idag er innenfor intervallet: isInsideIntervalNow
         */
        private fun oppfolgingstilfelleInsideStoppunktInterval(oppfolgingstilfelle: Oppfolgingstilfelle.OppfolgingstilfelleFromKafka): Boolean {
            val durationDays = oppfolgingstilfelle.durationInDays()
            val durationInDaysUntilNow = min(
                durationDays,
                ChronoUnit.DAYS.between(oppfolgingstilfelle.tilfelleStart, LocalDate.now()) + 1
            )
            val hasDurationInsideInterval = durationDays in KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS..KARTLEGGINGSSPORSMAL_STOPPUNKT_END_DAYS
            val willHaveSykedagInIntervalDuringThisPeriod = durationInDaysUntilNow <= KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS && durationDays >= KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS
            val isInsideIntervalNow = durationInDaysUntilNow in KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS..KARTLEGGINGSSPORSMAL_STOPPUNKT_END_DAYS

            return hasDurationInsideInterval || willHaveSykedagInIntervalDuringThisPeriod || isInsideIntervalNow
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
