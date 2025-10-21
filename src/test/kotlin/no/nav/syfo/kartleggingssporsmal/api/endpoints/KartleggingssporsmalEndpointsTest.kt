package no.nav.syfo.kartleggingssporsmal.api.endpoints

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.kartleggingssporsmal.application.KartleggingssporsmalService
import no.nav.syfo.kartleggingssporsmal.domain.KandidatStatus
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.shared.api.generateJWT
import no.nav.syfo.shared.api.testApiModule
import no.nav.syfo.shared.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.shared.util.configure
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KartleggingssporsmalEndpointsTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val kartleggingssporsmalServiceMock = mockk<KartleggingssporsmalService>(relaxed = true)
    private val validToken = generateJWT(
        audience = externalMockEnvironment.environment.azure.appClientId,
        issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
        navIdent = UserConstants.VEILEDER_IDENT,
    )
    private val kartleggingssporsmalUrl = "/api/internad/v1/kartleggingssporsmal/kandidater"

    private fun ApplicationTestBuilder.setupApiAndClient(kartleggingssporsmalServiceMock: KartleggingssporsmalService? = null): HttpClient {
        application {
            testApiModule(
                externalMockEnvironment = ExternalMockEnvironment.instance,
                kartleggingssporsmalServiceMock = kartleggingssporsmalServiceMock,
            )
        }
        val client = createClient {
            install(ContentNegotiation) {
                jackson { configure() }
            }
        }
        return client
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Nested
    @DisplayName("Get kartleggingssporsmal")
    inner class GetKartleggingssporsmal {
        @Test
        fun `Returns status OK if valid token is supplied and kandidat exists`() = testApplication {
            val client = setupApiAndClient(kartleggingssporsmalServiceMock)
            val kandidat = KartleggingssporsmalKandidat(
                personident = ARBEIDSTAKER_PERSONIDENT,
                status = KandidatStatus.KANDIDAT,
            )
            coEvery { kartleggingssporsmalServiceMock.getKandidat(ARBEIDSTAKER_PERSONIDENT) } returns kandidat

            val response = client.get(kartleggingssporsmalUrl) {
                bearerAuth(validToken)
                header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        @Test
        fun `Returns status NotFound if valid token is supplied, but kandidat doesn't exist`() = testApplication {
            val client = setupApiAndClient(kartleggingssporsmalServiceMock)
            coEvery { kartleggingssporsmalServiceMock.getKandidat(ARBEIDSTAKER_PERSONIDENT) } returns null

            val response = client.get(kartleggingssporsmalUrl) {
                bearerAuth(validToken)
                header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        @Test
        fun `Returns status Unauthorized if no token is supplied`() = testApplication {
            val client = setupApiAndClient()

            val response = client.get(kartleggingssporsmalUrl)

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `Returns status Forbidden if denied access to person`() = testApplication {
            val client = setupApiAndClient()

            val response = client.get(kartleggingssporsmalUrl) {
                bearerAuth(validToken)
                header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS.value)
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

        @Test
        fun `Returns status BadRequest if no $NAV_PERSONIDENT_HEADER is supplied`() = testApplication {
            val client = setupApiAndClient()

            val response = client.get(kartleggingssporsmalUrl) {
                bearerAuth(validToken)
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `Returns status BadRequest if $NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied`() =
            testApplication {
                val client = setupApiAndClient()

                val response = client.get(kartleggingssporsmalUrl) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value.drop(1))
                }

                assertEquals(HttpStatusCode.BadRequest, response.status)
            }
    }

    @Nested
    @DisplayName("Post kartleggingssporsmal")
    inner class PostKartleggingssporsmal {
        @Test
        fun `Returns status OK if valid token is supplied and kandidat exists`() = testApplication {
            val client = setupApiAndClient(kartleggingssporsmalServiceMock)
            coEvery { kartleggingssporsmalServiceMock.registrerFerdigBehandlet(ARBEIDSTAKER_PERSONIDENT, any()) } just Runs

            val response = client.post(kartleggingssporsmalUrl) {
                bearerAuth(validToken)
                header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        @Test
        fun `Returns status NotFound if valid token is supplied, but kandidat doesn't exist`() = testApplication {
            val client = setupApiAndClient(kartleggingssporsmalServiceMock)
            coEvery { kartleggingssporsmalServiceMock.registrerFerdigBehandlet(ARBEIDSTAKER_PERSONIDENT, any()) } throws IllegalArgumentException()

            val response = client.post(kartleggingssporsmalUrl) {
                bearerAuth(validToken)
                header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }
}
