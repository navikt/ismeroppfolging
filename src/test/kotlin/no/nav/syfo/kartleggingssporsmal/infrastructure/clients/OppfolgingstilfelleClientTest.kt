package no.nav.syfo.kartleggingssporsmal.infrastructure.clients

import io.ktor.client.plugins.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_ERROR
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_NO_TILFELLE
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_TILFELLE_DOD
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows

class OppfolgingstilfelleClientTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val oppfolgingstilfelleClient = externalMockEnvironment.oppfolgingstilfelleClient

    @Test
    fun `should return oppfolgingstilfelle for person`() {
        runBlocking {
            val tilfelle = oppfolgingstilfelleClient.getOppfolgingstilfelle(ARBEIDSTAKER_PERSONIDENT).getOrThrow()

            assertNotNull(tilfelle)
            assertEquals(tilfelle.personident.value, ARBEIDSTAKER_PERSONIDENT.value)
            assertEquals(tilfelle.antallSykedager, 42)
        }
    }

    @Test
    fun `should return oppfolgingstilfelle for dead person`() {
        runBlocking {
            val tilfelle = oppfolgingstilfelleClient.getOppfolgingstilfelle(ARBEIDSTAKER_PERSONIDENT_TILFELLE_DOD).getOrThrow()

            assertNotNull(tilfelle)
            assertEquals(tilfelle.personident.value, ARBEIDSTAKER_PERSONIDENT_TILFELLE_DOD.value)
            assertNotNull(tilfelle.dodsdato)
        }
    }

    @Test
    fun `should return oppfolgingstilfelle not active anymore`() {
        runBlocking {
            val tilfelle = oppfolgingstilfelleClient.getOppfolgingstilfelle(ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT).getOrThrow()

            assertNotNull(tilfelle)
            assertEquals(tilfelle.personident.value, ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT.value)
            assertEquals(tilfelle.antallSykedager, 10)
        }
    }

    @Test
    fun `should return null when person without oppfolgingstilfelle`() {
        runBlocking {
            val tilfelle = oppfolgingstilfelleClient.getOppfolgingstilfelle(ARBEIDSTAKER_PERSONIDENT_NO_TILFELLE).getOrThrow()

            assertNull(tilfelle)
        }
    }

    @Test
    fun `should throw error when call fails`() {
        runBlocking {
            assertThrows<ResponseException> {
                oppfolgingstilfelleClient.getOppfolgingstilfelle(ARBEIDSTAKER_PERSONIDENT_ERROR).getOrThrow()
            }
        }
    }
}
