package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.util.fullDaysBetween
import no.nav.syfo.shared.util.isAfterOrEqual
import no.nav.syfo.shared.util.isBeforeOrEqual
import no.nav.syfo.shared.util.isMoreThanDaysAgo
import no.nav.syfo.shared.util.tomorrow
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Represents an oppfolgingstilfelle for a person.
 * Can be created from data received from Kafka or from an API.
 *
 * @param isArbeidstakerAtTilfelleEnd Indicates if the person was an arbeidstaker at the end of the oppfolgingstilfelle.
 * @param tilfelleStart The start date of the oppfolgingstilfelle.
 * @param tilfelleEnd The end date of the oppfolgingstilfelle.
 * @param personident The personal identifier of the person.
 * @param antallSykedager The number of sykedager in oppfolgingstilfellet.
 * Will be shorter or equal to the number of days between tilfelleStart and tilfelleEnd due to feriedager or friske dager in interval.
 * Can be null for old oppfolgingstilfeller.
 * @param dodsdato The date of death, null if still alive.
 * @param virksomhetsnummerList List of virksomhetsnummer the person is sykmeldt from in oppfolgingstilfellet.
 *
 */
sealed class Oppfolgingstilfelle(
    open val isArbeidstakerAtTilfelleEnd: Boolean,
    open val tilfelleStart: LocalDate,
    open val tilfelleEnd: LocalDate,
    open val personident: Personident,
    open val antallSykedager: Int?,
    open val dodsdato: LocalDate?,
    open val virksomhetsnummerList: List<String>,
) {

    data class OppfolgingstilfelleFromKafka internal constructor(
        override val personident: Personident,
        override val tilfelleStart: LocalDate,
        override val tilfelleEnd: LocalDate,
        override val antallSykedager: Int?,
        override val dodsdato: LocalDate?,
        override val isArbeidstakerAtTilfelleEnd: Boolean,
        override val virksomhetsnummerList: List<String>,
        val uuid: UUID,
        val tilfelleGenerert: OffsetDateTime,
        val tilfelleBitReferanseUuid: UUID,
    ) : Oppfolgingstilfelle(
        isArbeidstakerAtTilfelleEnd,
        tilfelleStart,
        tilfelleEnd,
        personident,
        antallSykedager,
        dodsdato,
        virksomhetsnummerList,
    ) {
        fun hasTilfelleWithEndMoreThanThirtyDaysAgo(): Boolean = tilfelleEnd isMoreThanDaysAgo 30
    }

    data class OppfolgingstilfelleFromApi internal constructor(
        override val personident: Personident,
        override val tilfelleStart: LocalDate,
        override val tilfelleEnd: LocalDate,
        override val antallSykedager: Int?,
        override val dodsdato: LocalDate?,
        override val isArbeidstakerAtTilfelleEnd: Boolean,
        override val virksomhetsnummerList: List<String>,
    ) : Oppfolgingstilfelle(
        isArbeidstakerAtTilfelleEnd,
        tilfelleStart,
        tilfelleEnd,
        personident,
        antallSykedager,
        dodsdato,
        virksomhetsnummerList,
    ) {
        fun isActive(): Boolean = LocalDate.now().isBeforeOrEqual(tilfelleEnd)

        fun datoInsideTilfelle(dato: LocalDate): Boolean =
            dato isAfterOrEqual tilfelleStart && dato isBeforeOrEqual tilfelleEnd
    }

    fun isDod() = dodsdato != null

    fun durationInDays(): Long {
        return antallSykedager?.toLong()
            ?: fullDaysBetween(
                tilfelleStart,
                tilfelleEnd,
            )
    }

    companion object {

        fun createFromKafka(
            uuid: String,
            personident: String,
            oppfolgingstilfelleList: List<OppfolgingstilfelleDTO>,
            referanseTilfelleBitUuid: String,
            dodsdato: LocalDate?,
            tilfelleGenerert: OffsetDateTime,
        ): OppfolgingstilfelleFromKafka? {
            return getLatestOppfolgingstilfelle(oppfolgingstilfelleList)
                ?.let { latestOppfolgingstilfelle ->
                    OppfolgingstilfelleFromKafka(
                        uuid = UUID.fromString(uuid),
                        personident = Personident(personident),
                        tilfelleStart = latestOppfolgingstilfelle.start,
                        tilfelleEnd = latestOppfolgingstilfelle.end,
                        antallSykedager = latestOppfolgingstilfelle.antallSykedager,
                        dodsdato = dodsdato,
                        isArbeidstakerAtTilfelleEnd = latestOppfolgingstilfelle.arbeidstakerAtTilfelleEnd,
                        virksomhetsnummerList = latestOppfolgingstilfelle.virksomhetsnummerList,
                        tilfelleGenerert = tilfelleGenerert,
                        tilfelleBitReferanseUuid = UUID.fromString(referanseTilfelleBitUuid),
                    )
                }
        }

        fun createFromApi(
            oppfolgingstilfelleList: List<OppfolgingstilfelleDTO>,
            personident: String,
            dodsdato: LocalDate?,
        ): OppfolgingstilfelleFromApi? {
            return getLatestOppfolgingstilfelle(oppfolgingstilfelleList)
                ?.let { latestOppfolgingstilfelle ->
                    OppfolgingstilfelleFromApi(
                        personident = Personident(personident),
                        tilfelleStart = latestOppfolgingstilfelle.start,
                        tilfelleEnd = latestOppfolgingstilfelle.end,
                        antallSykedager = latestOppfolgingstilfelle.antallSykedager,
                        dodsdato = dodsdato,
                        isArbeidstakerAtTilfelleEnd = latestOppfolgingstilfelle.arbeidstakerAtTilfelleEnd,
                        virksomhetsnummerList = latestOppfolgingstilfelle.virksomhetsnummerList,
                    )
                }
        }

        private fun getLatestOppfolgingstilfelle(oppfolgingstilfelleList: List<OppfolgingstilfelleDTO>): OppfolgingstilfelleDTO? {
            return oppfolgingstilfelleList
                .filter { isNotTilfelleInTheFuture(it.start) }
                .maxByOrNull { it.start }
        }

        private fun isNotTilfelleInTheFuture(tilfelleStart: LocalDate): Boolean {
            return tilfelleStart.isBefore(tomorrow())
        }
    }
}

data class OppfolgingstilfelleDTO(
    val arbeidstakerAtTilfelleEnd: Boolean,
    val start: LocalDate,
    val end: LocalDate,
    val antallSykedager: Int?,
    val virksomhetsnummerList: List<String>,
)
