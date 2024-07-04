package no.nav.syfo.api.endpoints

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.api.*
import no.nav.syfo.infrastructure.NAV_PERSONIDENT_HEADER
import no.nav.syfo.infrastructure.bearerHeader
import no.nav.syfo.infrastructure.database.dropData
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.UUID

object SenOppfolgingEndpointsSpek : Spek({

    describe(SenOppfolgingEndpointsSpek::class.java.simpleName) {
        with(TestApplicationEngine()) {
            start()
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                navIdent = UserConstants.VEILEDER_IDENT,
            )

            application.testApiModule(externalMockEnvironment = externalMockEnvironment)

            beforeEachTest {
                database.dropData()
            }

            describe("Ferdigbehandle kandidat") {
                val kandidatUuid = UUID.randomUUID()
                val ferdigbehandlingUrl = "$senOppfolgingApiBasePath/kandidat/$kandidatUuid/ferdigbehandling"

                it("returns OK if request is successful") {
                    with(
                        handleRequest(HttpMethod.Put, ferdigbehandlingUrl) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }
                }
                it("Returns status Unauthorized if no token is supplied") {
                    testMissingToken(ferdigbehandlingUrl, HttpMethod.Put)
                }
                it("Returns status Forbidden if denied access to person") {
                    testDeniedPersonAccess(ferdigbehandlingUrl, validToken, HttpMethod.Put)
                }
                it("Returns status BadRequest if no $NAV_PERSONIDENT_HEADER is supplied") {
                    testMissingPersonIdent(ferdigbehandlingUrl, validToken, HttpMethod.Put)
                }
                it("Returns status BadRequest if $NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied") {
                    testInvalidPersonIdent(ferdigbehandlingUrl, validToken, HttpMethod.Put)
                }
            }
        }
    }
})
