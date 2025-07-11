package no.nav.syfo.kartleggingssporsmal.infrastructure.clients

import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_INACTIVE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import kotlin.test.assertEquals

class BehandlendeEnhetClientTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val behandlendeEnhetClient = externalMockEnvironment.behandlendeEnhetClient

    @Test
    fun `should get behandlende enhet for person`() {
        val callId = "test-call-id"

        runBlocking {
            val response = behandlendeEnhetClient.getEnhet(
                callId = callId,
                personident = ARBEIDSTAKER_PERSONIDENT,
            )

            assertEquals("0314", response?.geografiskEnhet?.enhetId)
            assertNull(response?.oppfolgingsenhetDTO)
        }
    }

    @Test
    fun `should return null for person without behandlende enhet`() {
        val callId = "test-call-id"

        runBlocking {
            val response = behandlendeEnhetClient.getEnhet(
                callId = callId,
                personident = ARBEIDSTAKER_PERSONIDENT_INACTIVE,
            )

            assertNull(response)
        }
    }
}
