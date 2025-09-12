package no.nav.syfo.kartleggingssporsmal.infrastructure.clients

import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_NO_FODSELSDATO
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_INACTIVE
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_ONLY_FODSELSAAR
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.pdl.model.getAlder
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
            val result = pdlClient.getPerson(ARBEIDSTAKER_PERSONIDENT_NO_FODSELSDATO)

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
    
    @Test
    fun `should return correct alder for person with foedseldato`(){
        runBlocking {
            val result = pdlClient.getPerson(ARBEIDSTAKER_PERSONIDENT)
            val alder = result.getOrNull()?.getAlder()

            assertTrue(result.isSuccess)
            assertEquals(30, alder)
        }
    }
    
    @Test
    fun `should return correct alder for person with foedselsaar only`(){
        runBlocking {
            val result = pdlClient.getPerson(ARBEIDSTAKER_PERSONIDENT_ONLY_FODSELSAAR)
            val alder = result.getOrNull()?.getAlder()

            assertTrue(result.isSuccess)
            assertEquals(40, alder)
        }
    }
}
