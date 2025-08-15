package no.nav.syfo.kartleggingssporsmal.infrastructure.database

import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class KartleggingssporsmalRepositoryTest {

    val database = ExternalMockEnvironment.instance.database
    val kartleggingssporsmalRepository = KartleggingssporsmalRepository(database)

    @Test
    fun `createStoppunkt should create a stoppunkt in the database`() {
        val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt(
            personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
            tilfelleBitReferanseUuid = UUID.randomUUID(),
            stoppunktAt = LocalDate.now().plusWeeks(1),
        )

        val createdStoppunkt = kartleggingssporsmalRepository.createStoppunkt(kartleggingssporsmalStoppunkt)

        assertEquals(kartleggingssporsmalStoppunkt.personident, createdStoppunkt.personident)
        assertEquals(kartleggingssporsmalStoppunkt.tilfelleBitReferanseUuid, createdStoppunkt.tilfelleBitReferanseUuid)
        assertEquals(kartleggingssporsmalStoppunkt.stoppunktAt, createdStoppunkt.stoppunktAt)
    }
}
