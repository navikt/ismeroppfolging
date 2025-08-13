package no.nav.syfo.kartleggingssporsmal.infrastructure.clients

import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_NO_FODSELSDATO
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_INACTIVE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PdlClientTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val pdlClient = externalMockEnvironment.pdlClient

    @Test
    fun `should return fodselsdato for person`() {
        runBlocking {
            val result = pdlClient.getPerson(ARBEIDSTAKER_PERSONIDENT)
            val fodselsdato = result.getOrNull()?.foedselsdato?.first()?.foedselsdato

            assertTrue(result.isSuccess)
            assertEquals(LocalDate.now().minusYears(30), fodselsdato)
        }
    }

    @Test
    fun `should return empty list when person has no fodselsdato information`() {
        runBlocking {
            val result = pdlClient.getPerson(ARBEIDSTAKER_NO_FODSELSDATO)

            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()!!.foedselsdato.isEmpty())
        }
    }

    @Test
    fun `should throw error when person not found in pdl`() {
        runBlocking {
            val result = pdlClient.getPerson(ARBEIDSTAKER_PERSONIDENT_INACTIVE)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is RuntimeException)
        }
    }
}
