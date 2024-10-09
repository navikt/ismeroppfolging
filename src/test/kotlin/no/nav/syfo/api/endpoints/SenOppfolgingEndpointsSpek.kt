package no.nav.syfo.api.endpoints

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.api.*
import no.nav.syfo.api.model.SenOppfolgingKandidatResponseDTO
import no.nav.syfo.api.model.SenOppfolgingKandidaterRequestDTO
import no.nav.syfo.api.model.SenOppfolgingKandidaterResponseDTO
import no.nav.syfo.api.model.SenOppfolgingVurderingRequestDTO
import no.nav.syfo.domain.*
import no.nav.syfo.infrastructure.NAV_PERSONIDENT_HEADER
import no.nav.syfo.infrastructure.bearerHeader
import no.nav.syfo.infrastructure.database.dropData
import no.nav.syfo.infrastructure.database.repository.SenOppfolgingRepository
import no.nav.syfo.util.configuredJacksonMapper
import no.nav.syfo.util.millisekundOpplosning
import no.nav.syfo.util.nowUTC
import org.amshove.kluent.shouldBeAfter
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*

object SenOppfolgingEndpointsSpek : Spek({

    val kandidatVarselAt = nowUTC()
    val senOppfolgingKandidat = SenOppfolgingKandidat(
        personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
        varselAt = kandidatVarselAt,
    )
    val kandidatSvarAt = nowUTC()
    val svar = SenOppfolgingSvar(svarAt = kandidatSvarAt, onskerOppfolging = OnskerOppfolging.JA)

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

            afterEachTest {
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

                        val responseDTOs =
                            objectMapper.readValue<List<SenOppfolgingKandidatResponseDTO>>(response.content!!)
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

                        val responseDTOs =
                            objectMapper.readValue<List<SenOppfolgingKandidatResponseDTO>>(response.content!!)
                        responseDTOs.size shouldBeEqualTo 1

                        val kandidat = responseDTOs.first()
                        kandidat.uuid shouldBeEqualTo senOppfolgingKandidat.uuid
                        kandidat.personident shouldBeEqualTo senOppfolgingKandidat.personident.value
                        kandidat.createdAt.millisekundOpplosning() shouldBeEqualTo senOppfolgingKandidat.createdAt.toLocalDateTime()
                            .millisekundOpplosning()
                        kandidat.varselAt?.millisekundOpplosning() shouldBeEqualTo kandidatVarselAt.toLocalDateTime()
                            .millisekundOpplosning()
                        kandidat.svar.shouldBeNull()
                    }
                }

                it("Returns kandidat with svar") {
                    senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)
                    senOppfolgingRepository.updateKandidatSvar(
                        senOppfolgingSvar = svar,
                        senOppfolgingKandidaUuid = senOppfolgingKandidat.uuid
                    )

                    with(
                        handleRequest(HttpMethod.Get, kandidaterUrl) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val responseDTOs =
                            objectMapper.readValue<List<SenOppfolgingKandidatResponseDTO>>(response.content!!)
                        responseDTOs.size shouldBeEqualTo 1

                        val kandidat = responseDTOs.first()
                        kandidat.uuid shouldBeEqualTo senOppfolgingKandidat.uuid
                        kandidat.svar.shouldNotBeNull()
                        kandidat.svar!!.svarAt.millisekundOpplosning() shouldBeEqualTo kandidatSvarAt.toLocalDateTime()
                            .millisekundOpplosning()
                        kandidat.svar!!.onskerOppfolging shouldBeEqualTo svar.onskerOppfolging
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

                        val kandidatResponse =
                            objectMapper.readValue(response.content, SenOppfolgingKandidatResponseDTO::class.java)
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
                        handleRequest(
                            HttpMethod.Post,
                            "$senOppfolgingApiBasePath/kandidater/${UUID.randomUUID()}/vurderinger"
                        ) {
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

            describe("POST /get-kandidater") {
                val kandidaterPath = "$senOppfolgingApiBasePath/get-kandidater"
                val otherPersonident = Personident("98765432123")
                val personidenter = listOf(
                    UserConstants.ARBEIDSTAKER_PERSONIDENT.value,
                    otherPersonident.value
                )
                val requestDTO = SenOppfolgingKandidaterRequestDTO(personidenter)

                it("Henter ut kandidater når veileder har tilgang til personene") {
                    senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)
                    senOppfolgingRepository.createKandidat(
                        senOppfolgingKandidat = senOppfolgingKandidat.copy(
                            uuid = UUID.randomUUID(),
                            personident = otherPersonident
                        )
                    )

                    with(
                        handleRequest(HttpMethod.Post, kandidaterPath) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(objectMapper.writeValueAsString(requestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val responseDTO = objectMapper.readValue<SenOppfolgingKandidaterResponseDTO>(response.content!!)

                        responseDTO.kandidater.size shouldBeEqualTo 2
                        responseDTO.kandidater.values.first().personident shouldBeEqualTo UserConstants.ARBEIDSTAKER_PERSONIDENT.value
                        responseDTO.kandidater.values.last().personident shouldBeEqualTo otherPersonident.value
                    }
                }
                it("Henter ikke ut kandidater hvis veileder ikke har tilgang til personen") {
                    senOppfolgingRepository.createKandidat(
                        senOppfolgingKandidat = senOppfolgingKandidat.copy(
                            uuid = UUID.randomUUID(),
                            personident = UserConstants.ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS
                        )
                    )
                    senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)

                    with(
                        handleRequest(HttpMethod.Post, kandidaterPath) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(objectMapper.writeValueAsString(requestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val responseDTO = objectMapper.readValue<SenOppfolgingKandidaterResponseDTO>(response.content!!)

                        responseDTO.kandidater.size shouldBeEqualTo 1
                        responseDTO.kandidater.values.first().personident shouldBeEqualTo UserConstants.ARBEIDSTAKER_PERSONIDENT.value
                    }
                }
                it("Returnerer NoContent når veilder har tilgang, men ingen kandidater") {
                    with(
                        handleRequest(HttpMethod.Post, kandidaterPath) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(objectMapper.writeValueAsString(requestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }
                it("Henter ut nyeste kandidat når personen har blitt kandidat flere ganger") {
                    val latestUUID = UUID.randomUUID()
                    senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)
                    senOppfolgingRepository.createKandidat(
                        senOppfolgingKandidat = senOppfolgingKandidat.copy(
                            uuid = latestUUID,
                            createdAt = OffsetDateTime.now().plusDays(1)
                        )
                    )

                    with(
                        handleRequest(HttpMethod.Post, kandidaterPath) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(objectMapper.writeValueAsString(requestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val responseDTO = objectMapper.readValue<SenOppfolgingKandidaterResponseDTO>(response.content!!)

                        responseDTO.kandidater.size shouldBeEqualTo 1
                        responseDTO.kandidater.values.first().createdAt shouldBeAfter LocalDateTime.now()
                        responseDTO.kandidater.values.first().uuid shouldBeEqualTo latestUUID
                    }
                }
                it("Henter riktig vurdering for siste kandidat når kandidat har vurdering") {
                    val latestUUID = UUID.randomUUID()
                    val otherKandidat = senOppfolgingKandidat.copy(
                        uuid = latestUUID,
                        createdAt = OffsetDateTime.now().plusDays(1),
                    )
                    senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)
                    senOppfolgingRepository.createKandidat(senOppfolgingKandidat = otherKandidat)
                    senOppfolgingRepository.vurderKandidat(
                        senOppfolgingKandidat = senOppfolgingKandidat,
                        vurdering = SenOppfolgingVurdering(
                            veilederident = UserConstants.VEILEDER_IDENT,
                            begrunnelse = "Begrunnelse for gammel vurdering",
                            type = VurderingType.FERDIGBEHANDLET,
                        )
                    )
                    senOppfolgingRepository.vurderKandidat(
                        senOppfolgingKandidat = otherKandidat,
                        vurdering = SenOppfolgingVurdering(
                            veilederident = UserConstants.VEILEDER_IDENT,
                            begrunnelse = "Begrunnelse for ny vurdering",
                            type = VurderingType.FERDIGBEHANDLET,
                        )
                    )

                    with(
                        handleRequest(HttpMethod.Post, kandidaterPath) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(objectMapper.writeValueAsString(requestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val responseDTO = objectMapper.readValue<SenOppfolgingKandidaterResponseDTO>(response.content!!)

                        responseDTO.kandidater.size shouldBeEqualTo 1
                        responseDTO.kandidater.values.first().createdAt shouldBeAfter LocalDateTime.now()
                        responseDTO.kandidater.values.first().uuid shouldBeEqualTo latestUUID
                        responseDTO.kandidater.values.first().vurderinger.first().begrunnelse shouldBeEqualTo "Begrunnelse for ny vurdering"
                    }
                }
            }
        }
    }
})
