package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.util.isAfterOrEqual
import no.nav.syfo.shared.util.isBeforeOrEqual
import no.nav.syfo.shared.util.isMoreThanDaysAgo
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

data class Oppfolgingstilfelle(
    val uuid: UUID,
    val personident: Personident,
    val tilfelleGenerert: OffsetDateTime,
    val tilfelleBitReferanseUuid: UUID,
    val tilfelleStart: LocalDate,
    val tilfelleEnd: LocalDate,
    val antallSykedager: Int?,
    val dodsdato: LocalDate?,
    val isArbeidstakerAtTilfelleEnd: Boolean,
    val virksomhetsnummerList: List<String>,
) {

    fun hasTilfelleWithEndMoreThanThirtyDaysAgo(): Boolean = this.tilfelleEnd isMoreThanDaysAgo 30

    fun isDod() = dodsdato != null

    fun durationInDays(): Long {
        return if (this.antallSykedager != null) {
            antallSykedager.toLong()
        } else {
            ChronoUnit.DAYS.between(this.tilfelleStart, this.tilfelleEnd) + 1
        }
    }

    fun datoInsideCurrentTilfelle(dato: LocalDate): Boolean =
        dato isAfterOrEqual this.tilfelleStart && dato isBeforeOrEqual this.tilfelleEnd
}
