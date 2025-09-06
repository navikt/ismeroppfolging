package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfelleFromKafka
import no.nav.syfo.shared.util.DAYS_IN_WEEK
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.time.LocalDate
import kotlin.test.assertEquals

class KartleggingssporsmalStoppunktTest {

    val stoppunktStartIntervalDays = 6L * DAYS_IN_WEEK

    @Test
    fun `calculateStoppunktStartDays should return stoppunkt in future upon initialization`() {
        val tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays - 21)
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            tilfelleStart = tilfelleStart,
            tilfelleEnd = LocalDate.now().plusDays(25),
        )
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)

        assertNotNull(kartleggingssporsmalStoppunkt)
        assertEquals(tilfelleStart.plusDays(stoppunktStartIntervalDays), kartleggingssporsmalStoppunkt.stoppunktAt)
    }

    @Test
    fun `calculateStoppunktStartDays should return stoppunkt today when real stoppunkt in the past upon initialization`() {
        val tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays)
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            tilfelleStart = tilfelleStart,
            tilfelleEnd = LocalDate.now().plusDays(3),
        )
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)

        assertNotNull(kartleggingssporsmalStoppunkt)
        assertEquals(LocalDate.now(), kartleggingssporsmalStoppunkt.stoppunktAt)
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
