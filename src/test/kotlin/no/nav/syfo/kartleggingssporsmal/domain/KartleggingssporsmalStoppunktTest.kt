package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfelleFromKafka
import no.nav.syfo.shared.util.DAYS_IN_WEEK
import no.nav.syfo.shared.util.fullDaysBetween
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.time.LocalDate
import kotlin.test.assertEquals

class KartleggingssporsmalStoppunktTest {

    val stoppunktStartIntervalDays = 6L * DAYS_IN_WEEK

    @Test
    fun `should return stoppunkt in future when created`() {
        val tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays - 21)
        val tilfelleEnd = LocalDate.now().plusDays(25)
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            tilfelleStart = tilfelleStart,
            tilfelleEnd = tilfelleEnd,
            antallSykedager = fullDaysBetween(tilfelleStart, tilfelleEnd).toInt()
        )
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)

        assertNotNull(kartleggingssporsmalStoppunkt)
        assertEquals(tilfelleStart.plusDays(stoppunktStartIntervalDays), kartleggingssporsmalStoppunkt.stoppunktAt)
    }

    @Test
    fun `should return stoppunkt today when original stoppunkt in the past`() {
        val tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays + 20)
        val tilfelleEnd = LocalDate.now().plusDays(3)
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            tilfelleStart = tilfelleStart,
            tilfelleEnd = tilfelleEnd,
            antallSykedager = fullDaysBetween(tilfelleStart, tilfelleEnd).toInt(),
        )
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)

        assertNotNull(kartleggingssporsmalStoppunkt)
        assertEquals(LocalDate.now(), kartleggingssporsmalStoppunkt.stoppunktAt)
    }

    @Test
    fun `should postpone stoppunkt based on friske dager in period so far`() {
        val tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays)
        val tilfelleEnd = LocalDate.now().plusDays(20)
        val numberOfFriskeDager = 5
        val antallSykedager = fullDaysBetween(
            tilfelleStart,
            tilfelleEnd,
        ).toInt() - numberOfFriskeDager
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            tilfelleStart = tilfelleStart,
            tilfelleEnd = tilfelleEnd,
            antallSykedager = antallSykedager,
        )
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)

        assertNotNull(kartleggingssporsmalStoppunkt)
        assertEquals(tilfelleStart.plusDays(stoppunktStartIntervalDays + numberOfFriskeDager), kartleggingssporsmalStoppunkt.stoppunktAt)
    }

    @Test
    fun `should not postpone stoppunkt based on friske dager when antallSykedager is null`() {
        val tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays)
        val tilfelleEnd = LocalDate.now().plusDays(20)
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            tilfelleStart = tilfelleStart,
            tilfelleEnd = tilfelleEnd,
            antallSykedager = null,
        )
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)

        assertNotNull(kartleggingssporsmalStoppunkt)
        assertEquals(tilfelleStart.plusDays(stoppunktStartIntervalDays), kartleggingssporsmalStoppunkt.stoppunktAt)
    }

    @Test
    fun `initialization should return null when oppfolgingstilfelle does not generate a stoppunkt`() {
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            tilfelleStart = LocalDate.now(),
            antallSykedager = 10,
        )
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)

        assertNull(kartleggingssporsmalStoppunkt)
    }
}
