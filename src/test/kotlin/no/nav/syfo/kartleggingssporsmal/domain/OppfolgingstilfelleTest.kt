package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfelle
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppfolgingstilfelleTest {

    @Nested
    @DisplayName("hasTilfelleWithEndMoreThanThirtyDaysAgo")
    inner class HasTilfelleWithEndMoreThanThirtyDaysAgo {
        @Test
        fun `should be true when more than 30 days ago`() {
            val tilfelle = createOppfolgingstilfelle(
                tilfelleStart = LocalDate.now().minusDays(40),
                tilfelleEnd = LocalDate.now().minusDays(31),
            )

            assertTrue(tilfelle.hasTilfelleWithEndMoreThanThirtyDaysAgo())
        }

        @Test
        fun `should be false when exactly 30 days`() {
            val tilfelle = createOppfolgingstilfelle(
                tilfelleStart = LocalDate.now().minusDays(40),
                tilfelleEnd = LocalDate.now().minusDays(30),
            )

            assertFalse(tilfelle.hasTilfelleWithEndMoreThanThirtyDaysAgo())
        }
    }

    @Nested
    @DisplayName("isDod")
    inner class IsDod {
        @Test
        fun `should be true when dodsdato is not null`() {
            val tilfelle = createOppfolgingstilfelle(
                dodsdato = LocalDate.now().minusDays(1),
            )

            assertTrue(tilfelle.isDod())
        }

        @Test
        fun `should be false when dodsdato is null`() {
            val tilfelle = createOppfolgingstilfelle(
                dodsdato = null,
            )

            assertFalse(tilfelle.isDod())
        }
    }

    @Nested
    @DisplayName("durationInDays")
    inner class DurationInDays {
        @Test
        fun `returns duration for antallSykedager when not null`() {
            val tilfelle = createOppfolgingstilfelle(
                antallSykedager = 10,
            )

            assertEquals(10, tilfelle.durationInDays())
        }

        @Test
        fun `calculates duration from start and end when antallSykedager is null`() {
            val tilfelle = createOppfolgingstilfelle(
                antallSykedager = null,
                tilfelleStart = LocalDate.now(),
                tilfelleEnd = LocalDate.now().plusDays(1),
            )

            assertEquals(2, tilfelle.durationInDays())
        }
    }
}
