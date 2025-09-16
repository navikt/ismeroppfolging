package no.nav.syfo.kartleggingssporsmal.infrastructure.clients

import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_ERROR
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_INACTIVE
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.vedtak14a.Innsatsgruppe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class Vedtak14aClientTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val vedtak14aClient = externalMockEnvironment.vedtak14aClient

    @Test
    fun `should get gjeldende vedtak 14a for person`() {
        runBlocking {
            val response = vedtak14aClient.hentGjeldende14aVedtak(ARBEIDSTAKER_PERSONIDENT)

            assertTrue(response.isSuccess)
            assertEquals(response.getOrNull()?.innsatsgruppe, Innsatsgruppe.GODE_MULIGHETER)
        }
    }

    @Test
    fun `should return null for user without 14a-vedtak`() {
        runBlocking {
            val response = vedtak14aClient.hentGjeldende14aVedtak(ARBEIDSTAKER_PERSONIDENT_INACTIVE)

            assertTrue(response.isSuccess)
            assertNull(response.getOrNull())
        }
    }

    @Test
    fun `should fail when API-call returns an error`() {
        runBlocking {
            val response = vedtak14aClient.hentGjeldende14aVedtak(ARBEIDSTAKER_PERSONIDENT_ERROR)

            assertTrue(response.isFailure)
            assertThrows<RuntimeException> { response.getOrThrow() }
        }
    }
}
