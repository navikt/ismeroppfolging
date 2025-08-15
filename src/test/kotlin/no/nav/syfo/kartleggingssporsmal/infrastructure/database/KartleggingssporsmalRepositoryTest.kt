package no.nav.syfo.kartleggingssporsmal.infrastructure.database

import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfelle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.time.LocalDate
import kotlin.test.Test

class KartleggingssporsmalRepositoryTest {

    val database = ExternalMockEnvironment.instance.database
    val kartleggingssporsmalRepository = KartleggingssporsmalRepository(database)

    @Test
    fun `createStoppunkt should create a stoppunkt in the database`() {
        val oppfolgingstilfelle = createOppfolgingstilfelle(
            tilfelleStart = LocalDate.now(),
            antallSykedager = 6 * 7,
        )
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
        assertNotNull(kartleggingssporsmalStoppunkt)

        runBlocking {
            val createdStoppunkt = kartleggingssporsmalRepository.createStoppunkt(kartleggingssporsmalStoppunkt)

            assertEquals(kartleggingssporsmalStoppunkt.personident, createdStoppunkt.personident)
            assertEquals(kartleggingssporsmalStoppunkt.tilfelleBitReferanseUuid, createdStoppunkt.tilfelleBitReferanseUuid)
            assertEquals(kartleggingssporsmalStoppunkt.stoppunktAt, createdStoppunkt.stoppunktAt)
            assertNull(createdStoppunkt.processedAt)
        }
    }
}
