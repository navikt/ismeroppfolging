package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfelleDTO
import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfelleFromKafka
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertEquals

class OppfolgingstilfelleTest {

    @Nested
    @DisplayName("Oppfolgingstilfelle initialization")
    inner class Initialization {
        @Test
        fun `should return newest Oppfolgingstilfelle when several oppfolgingstilfeller in list`() {
            val tilfelle = Oppfolgingstilfelle.createFromKafka(
                uuid = UUID.randomUUID().toString(),
                personident = ARBEIDSTAKER_PERSONIDENT.value,
                oppfolgingstilfelleList = listOf(
                    createOppfolgingstilfelleDTO(
                        tilfelleStart = LocalDate.now().minusDays(50),
                        tilfelleEnd = LocalDate.now().minusDays(40),
                        antallSykedager = 11,
                    ),
                    createOppfolgingstilfelleDTO(
                        tilfelleStart = LocalDate.now(),
                        tilfelleEnd = LocalDate.now().plusDays(10),
                        antallSykedager = 11,
                    )
                ),
                referanseTilfelleBitUuid = UUID.randomUUID().toString(),
                dodsdato = null,
                tilfelleGenerert = OffsetDateTime.now(),
            )

            assertNotNull(tilfelle)
            assertEquals(LocalDate.now(), tilfelle.tilfelleStart)
            assertEquals(LocalDate.now().plusDays(10), tilfelle.tilfelleEnd)
        }

        @Test
        fun `should return null when only tilfeller in the future`() {
            val tilfelle = Oppfolgingstilfelle.createFromKafka(
                uuid = UUID.randomUUID().toString(),
                personident = ARBEIDSTAKER_PERSONIDENT.value,
                oppfolgingstilfelleList = listOf(
                    createOppfolgingstilfelleDTO(
                        tilfelleStart = LocalDate.now().plusDays(3),
                        tilfelleEnd = LocalDate.now().plusDays(10),
                        antallSykedager = 8,
                    ),
                ),
                referanseTilfelleBitUuid = UUID.randomUUID().toString(),
                dodsdato = null,
                tilfelleGenerert = OffsetDateTime.now(),
            )

            assertNull(tilfelle)
        }

        @Test
        fun `should return current tilfelle when tilfeller both in future, is now and is old`() {
            val tilfelle = Oppfolgingstilfelle.createFromKafka(
                uuid = UUID.randomUUID().toString(),
                personident = ARBEIDSTAKER_PERSONIDENT.value,
                oppfolgingstilfelleList = listOf(
                    createOppfolgingstilfelleDTO(
                        tilfelleStart = LocalDate.now().minusDays(50),
                        tilfelleEnd = LocalDate.now().minusDays(40),
                        antallSykedager = 11,
                    ),
                    createOppfolgingstilfelleDTO(
                        tilfelleStart = LocalDate.now(),
                        tilfelleEnd = LocalDate.now().plusDays(10),
                        antallSykedager = 11,
                    ),
                    createOppfolgingstilfelleDTO(
                        tilfelleStart = LocalDate.now().plusDays(30),
                        tilfelleEnd = LocalDate.now().plusDays(40),
                        antallSykedager = 11,
                    ),
                ),
                referanseTilfelleBitUuid = UUID.randomUUID().toString(),
                dodsdato = null,
                tilfelleGenerert = OffsetDateTime.now(),
            )

            assertNotNull(tilfelle)
            assertEquals(LocalDate.now(), tilfelle.tilfelleStart)
            assertEquals(LocalDate.now().plusDays(10), tilfelle.tilfelleEnd)
        }
    }

    @Nested
    @DisplayName("OppfolgingstilfelleFromKafka")
    inner class OppfolgingstilfelleFromKafka {

        @Nested
        @DisplayName("hasTilfelleWithEndMoreThanThirtyDaysAgo")
        inner class HasTilfelleWithEndMoreThanThirtyDaysAgo {
            @Test
            fun `should be true when more than 30 days ago`() {
                val tilfelle = createOppfolgingstilfelleFromKafka(
                    tilfelleStart = LocalDate.now().minusDays(40),
                    tilfelleEnd = LocalDate.now().minusDays(31),
                )

                assertTrue(tilfelle.hasTilfelleWithEndMoreThanThirtyDaysAgo())
            }

            @Test
            fun `should be false when exactly 30 days`() {
                val tilfelle = createOppfolgingstilfelleFromKafka(
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
                val tilfelle = createOppfolgingstilfelleFromKafka(
                    dodsdato = LocalDate.now().minusDays(1),
                )

                assertTrue(tilfelle.isDod())
            }

            @Test
            fun `should be false when dodsdato is null`() {
                val tilfelle = createOppfolgingstilfelleFromKafka(
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
                val tilfelle = createOppfolgingstilfelleFromKafka(
                    antallSykedager = 10,
                )

                assertEquals(10, tilfelle.durationInDays())
            }

            @Test
            fun `calculates duration from start and end when antallSykedager is null`() {
                val tilfelle = createOppfolgingstilfelleFromKafka(
                    antallSykedager = null,
                    tilfelleStart = LocalDate.now(),
                    tilfelleEnd = LocalDate.now().plusDays(1),
                )

                assertEquals(2, tilfelle.durationInDays())
            }
        }
    }

    @Nested
    @DisplayName("OppfolgingstilfelleFromApi")
    inner class OppfolgingstilfelleFromApi {

        @Test
        fun `isActive should be true when tilfelleEnd is in the future`() {
            val tilfelle = Oppfolgingstilfelle.OppfolgingstilfelleFromApi(
                personident = ARBEIDSTAKER_PERSONIDENT,
                tilfelleStart = LocalDate.now().minusDays(1),
                tilfelleEnd = LocalDate.now().plusDays(2),
                antallSykedager = 4,
                dodsdato = null,
                isArbeidstakerAtTilfelleEnd = true,
                virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER),
            )

            assertTrue(tilfelle.isActive())
        }

        @Test
        fun `isActive should be true when tilfelleEnd is today`() {
            val tilfelleEndingToday = Oppfolgingstilfelle.OppfolgingstilfelleFromApi(
                personident = ARBEIDSTAKER_PERSONIDENT,
                tilfelleStart = LocalDate.now().minusDays(2),
                tilfelleEnd = LocalDate.now(),
                antallSykedager = 3,
                dodsdato = null,
                isArbeidstakerAtTilfelleEnd = true,
                virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER),
            )

            assertTrue(tilfelleEndingToday.isActive())
        }

        @Test
        fun `isActive should be false when tilfelleEnd is in past`() {
            val tilfelleEndingToday = Oppfolgingstilfelle.OppfolgingstilfelleFromApi(
                personident = ARBEIDSTAKER_PERSONIDENT,
                tilfelleStart = LocalDate.now().minusDays(20),
                tilfelleEnd = LocalDate.now().minusDays(10),
                antallSykedager = 11,
                dodsdato = null,
                isArbeidstakerAtTilfelleEnd = true,
                virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER),
            )

            assertFalse(tilfelleEndingToday.isActive())
        }
    }
}
