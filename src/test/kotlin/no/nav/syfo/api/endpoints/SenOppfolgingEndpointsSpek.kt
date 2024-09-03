package no.nav.syfo.api.endpoints

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.api.*
import no.nav.syfo.api.model.SenOppfolgingKandidatResponseDTO
import no.nav.syfo.api.model.SenOppfolgingVurderingRequestDTO
import no.nav.syfo.domain.SenOppfolgingKandidat
import no.nav.syfo.domain.SenOppfolgingStatus
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.infrastructure.NAV_PERSONIDENT_HEADER
import no.nav.syfo.infrastructure.bearerHeader
import no.nav.syfo.infrastructure.database.dropData
import no.nav.syfo.infrastructure.database.repository.SenOppfolgingRepository
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.OffsetDateTime
import java.util.UUID

object SenOppfolgingEndpointsSpek : Spek({

    val senOppfolgingKandidat = SenOppfolgingKandidat(
        personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
        varselAt = OffsetDateTime.now(),
    )
    val objectMapper: ObjectMapper = configuredJacksonMapper()

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
            val senOppfolgingRepository = SenOppfolgingRepository(database)

            application.testApiModule(externalMockEnvironment = externalMockEnvironment)

            beforeEachTest {
                database.dropData()
            }

            describe("Get kandidat") {
                val kandidaterUrl = "$senOppfolgingApiBasePath/kandidater"

                it("Returns empty list when person not kandidat") {
                    with(
                        handleRequest(HttpMethod.Get, kandidaterUrl) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val responseDTOs = objectMapper.readValue<List<SenOppfolgingKandidatResponseDTO>>(response.content!!)
                        responseDTOs.size shouldBeEqualTo 0
                    }
                }

                it("Returns list of kandidat for person") {
                    senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)

                    with(
                        handleRequest(HttpMethod.Get, kandidaterUrl) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val responseDTOs = objectMapper.readValue<List<SenOppfolgingKandidatResponseDTO>>(response.content!!)
                        responseDTOs.size shouldBeEqualTo 1

                        val kandidat = responseDTOs.first()
                        kandidat.uuid shouldBeEqualTo senOppfolgingKandidat.uuid
                        kandidat.personident shouldBeEqualTo senOppfolgingKandidat.personident.value
                    }
                }

                it("Returns status Unauthorized if no token is supplied") {
                    testMissingToken(kandidaterUrl, HttpMethod.Get)
                }

                it("Returns status Forbidden if denied access to person") {
                    testDeniedPersonAccess(kandidaterUrl, validToken, HttpMethod.Get)
                }

                it("Returns status BadRequest if no $NAV_PERSONIDENT_HEADER is supplied") {
                    testMissingPersonIdent(kandidaterUrl, validToken, HttpMethod.Get)
                }

                it("Returns status BadRequest if $NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied") {
                    testInvalidPersonIdent(kandidaterUrl, validToken, HttpMethod.Get)
                }
            }

            describe("Ferdigbehandle kandidat") {
                val kandidatUuid = senOppfolgingKandidat.uuid
                val ferdigbehandlingUrl = "$senOppfolgingApiBasePath/kandidater/$kandidatUuid/vurderinger"
                val vurderingRequestDTO =
                    SenOppfolgingVurderingRequestDTO(begrunnelse = "Begrunnelse", type = VurderingType.FERDIGBEHANDLET)

                it("Returns OK if request is successful") {
                    senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)

                    with(
                        handleRequest(HttpMethod.Post, ferdigbehandlingUrl) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(objectMapper.writeValueAsString(vurderingRequestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.Created

                        val kandidatResponse = objectMapper.readValue(response.content, SenOppfolgingKandidatResponseDTO::class.java)
                        kandidatResponse.uuid shouldBeEqualTo kandidatUuid
                        kandidatResponse.status shouldBeEqualTo SenOppfolgingStatus.FERDIGBEHANDLET
                        kandidatResponse.vurderinger.first().begrunnelse shouldBeEqualTo "Begrunnelse"
                        val ferdigbehandletVurdering =
                            kandidatResponse.vurderinger.first { it.type == VurderingType.FERDIGBEHANDLET }
                        ferdigbehandletVurdering.veilederident shouldBeEqualTo UserConstants.VEILEDER_IDENT
                    }
                }

                it("Returns status BadRequest when unknown kandidat") {
                    with(
                        handleRequest(HttpMethod.Post, "$senOppfolgingApiBasePath/kandidater/${UUID.randomUUID()}/vurderinger") {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(objectMapper.writeValueAsString(vurderingRequestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                    }
                }

                it("Returns status Conflict when kandidat already ferdigbehandlet") {
                    senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)

                    with(
                        handleRequest(HttpMethod.Post, ferdigbehandlingUrl) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(objectMapper.writeValueAsString(vurderingRequestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.Created
                    }

                    with(
                        handleRequest(HttpMethod.Post, ferdigbehandlingUrl) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(objectMapper.writeValueAsString(vurderingRequestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.Conflict
                    }
                }

                it("Returns status Unauthorized if no token is supplied") {
                    testMissingToken(ferdigbehandlingUrl, HttpMethod.Post)
                }

                it("Returns status Forbidden if denied access to person") {
                    testDeniedPersonAccess(ferdigbehandlingUrl, validToken, HttpMethod.Post)
                }

                it("Returns status BadRequest if no $NAV_PERSONIDENT_HEADER is supplied") {
                    testMissingPersonIdent(ferdigbehandlingUrl, validToken, HttpMethod.Post)
                }

                it("Returns status BadRequest if $NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied") {
                    testInvalidPersonIdent(ferdigbehandlingUrl, validToken, HttpMethod.Post)
                }
            }
        }
    }
})
