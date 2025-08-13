package no.nav.syfo.kartleggingssporsmal.infrastructure.kafka

import no.nav.syfo.kartleggingssporsmal.generators.createKafkaOppfolgingstilfelle
import no.nav.syfo.kartleggingssporsmal.generators.createKafkaOppfolgingstilfellePerson
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class KafkaOppfolgingstilfellePersonDTOTest {

    @Test
    fun `should return newest Oppfolgingstilfelle when several oppfolgingstilfeller in list`() {
        val oppfolgingstilfellePersonDTO = createKafkaOppfolgingstilfellePerson(
            tilfelleStart = LocalDate.now(),
            tilfelleEnd = LocalDate.now().plusDays(20),
            extraOppfolgingstilfeller = listOf(
                createKafkaOppfolgingstilfelle(
                    tilfelleStart = LocalDate.now().minusDays(50),
                    tilfelleEnd = LocalDate.now().minusDays(40),
                )
            )
        )

        val latestOppfolgingstilfelle = oppfolgingstilfellePersonDTO.toLatestOppfolgingstilfelle()

        assertEquals(LocalDate.now(), latestOppfolgingstilfelle?.tilfelleStart)
        assertEquals(LocalDate.now().plusDays(20), latestOppfolgingstilfelle?.tilfelleEnd)
    }

    @Test
    fun `should return null when only tilfeller in the future`() {
        val oppfolgingstilfellePersonDTO = createKafkaOppfolgingstilfellePerson(
            tilfelleStart = LocalDate.now().plusWeeks(1),
            tilfelleEnd = LocalDate.now().plusWeeks(2),
        )

        val latestOppfolgingstilfelle = oppfolgingstilfellePersonDTO.toLatestOppfolgingstilfelle()

        assertEquals(null, latestOppfolgingstilfelle)
    }

    @Test
    fun `should return current tilfelle when tilfeller both in future, is now and is old`() {
        val oppfolgingstilfellePersonDTO = createKafkaOppfolgingstilfellePerson(
            tilfelleStart = LocalDate.now(),
            tilfelleEnd = LocalDate.now().plusDays(20),
            extraOppfolgingstilfeller = listOf(
                createKafkaOppfolgingstilfelle(
                    tilfelleStart = LocalDate.now().minusDays(50),
                    tilfelleEnd = LocalDate.now().minusDays(40),
                ),
                createKafkaOppfolgingstilfelle(
                    tilfelleStart = LocalDate.now().plusWeeks(6),
                    tilfelleEnd = LocalDate.now().plusWeeks(7),
                )
            )
        )

        val latestOppfolgingstilfelle = oppfolgingstilfellePersonDTO.toLatestOppfolgingstilfelle()

        assertEquals(LocalDate.now(), latestOppfolgingstilfelle?.tilfelleStart)
        assertEquals(LocalDate.now().plusDays(20), latestOppfolgingstilfelle?.tilfelleEnd)
    }
}
