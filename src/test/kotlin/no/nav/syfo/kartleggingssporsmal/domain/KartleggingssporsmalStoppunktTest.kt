package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.UserConstants
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt.Companion.KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class KartleggingssporsmalStoppunktTest {

    @Test
    fun `calculateStoppunktStartDays should return stoppunkt in future upon initialization`() {
        val tilfelleStart = LocalDate.now().minusDays(KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS - 21)
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt(
            personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
            tilfelleBitReferanseUuid = UUID.randomUUID(),
            tilfelleStart = tilfelleStart,
            tilfelleEnd = LocalDate.now().plusDays(25),
        )

        assertEquals(tilfelleStart.plusDays(KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS), kartleggingssporsmalStoppunkt.stoppunktAt)
    }

    @Test
    fun `calculateStoppunktStartDays should return stoppunkt today when real stoppunkt in the past upon initialization`() {
        val tilfelleStart = LocalDate.now().minusDays(KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS)
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt(
            personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
            tilfelleBitReferanseUuid = UUID.randomUUID(),
            tilfelleStart = tilfelleStart,
            tilfelleEnd = LocalDate.now().plusDays(3),
        )

        assertEquals(LocalDate.now(), kartleggingssporsmalStoppunkt.stoppunktAt)
    }
}
