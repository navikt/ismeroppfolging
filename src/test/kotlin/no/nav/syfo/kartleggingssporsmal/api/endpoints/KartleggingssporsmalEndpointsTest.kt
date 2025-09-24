package no.nav.syfo.kartleggingssporsmal.api.endpoints

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.shared.api.generateJWT
import no.nav.syfo.shared.api.testApiModule
import no.nav.syfo.shared.infrastructure.database.createKartleggingssporsmalMottattTable
import no.nav.syfo.shared.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.shared.util.configure
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KartleggingssporsmalEndpointsTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val validToken = generateJWT(
        audience = externalMockEnvironment.environment.azure.appClientId,
        issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
        navIdent = UserConstants.VEILEDER_IDENT,
    )

    private fun ApplicationTestBuilder.setupApiAndClient(): HttpClient {
        application {
            testApiModule(
                externalMockEnvironment = ExternalMockEnvironment.instance,
            )
        }
        val client = createClient {
            install(ContentNegotiation) {
                jackson { configure() }
            }
        }
        return client
    }

    @Nested
    @DisplayName("Get kartleggingssporsmal")
    inner class GetKartleggingssporsmal {

        private val receivedQuestionsUrl = "/api/internad/v1/kartleggingssporsmal/person"

        @Test
        fun `Returns status OK if valid token is supplied`() {
            testApplication {
                val client = setupApiAndClient()
                database.createKartleggingssporsmalMottattTable()
                val response = client.get(receivedQuestionsUrl) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                }

                assertEquals(HttpStatusCode.OK, response.status)
            }
        }

        @Test
        fun `Returns status Unauthorized if no token is supplied`() {
            testApplication {
                val client = setupApiAndClient()

                val response = client.get(receivedQuestionsUrl)
                assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }

        @Test
        fun `Returns status Forbidden if denied access to person`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.get(receivedQuestionsUrl) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS.value)
                }

                assertEquals(HttpStatusCode.Forbidden, response.status)
            }
        }

        @Test
        fun `Returns status BadRequest if no $NAV_PERSONIDENT_HEADER is supplied`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.get(receivedQuestionsUrl) {
                    bearerAuth(validToken)
                }

                assertEquals(HttpStatusCode.BadRequest, response.status)
            }
        }

        @Test
        fun `Returns status BadRequest if $NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.get(receivedQuestionsUrl) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value.drop(1))
                }

                assertEquals(HttpStatusCode.BadRequest, response.status)
            }
        }
    }
}
