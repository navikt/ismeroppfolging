package no.nav.syfo.api.endpoints

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
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
import no.nav.syfo.infrastructure.database.dropData
import no.nav.syfo.infrastructure.database.repository.SenOppfolgingRepository
import no.nav.syfo.util.configure
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

    describe(SenOppfolgingEndpointsSpek::class.java.simpleName) {
        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val validToken = generateJWT(
            audience = externalMockEnvironment.environment.azure.appClientId,
            issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
            navIdent = UserConstants.VEILEDER_IDENT,
        )
        val senOppfolgingRepository = SenOppfolgingRepository(database)

        fun ApplicationTestBuilder.setupApiAndClient(): HttpClient {
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

        afterEachTest {
            database.dropData()
        }

        describe("Get kandidat") {
            val kandidaterUrl = "$senOppfolgingApiBasePath/kandidater"

            it("Returns empty list when person not kandidat") {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(kandidaterUrl) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val responseDTOs = response.body<List<SenOppfolgingKandidatResponseDTO>>()
                    responseDTOs.size shouldBeEqualTo 0
                }
            }

            it("Returns list of kandidat for person") {
                senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(kandidaterUrl) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val responseDTOs = response.body<List<SenOppfolgingKandidatResponseDTO>>()
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

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(kandidaterUrl) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val responseDTOs = response.body<List<SenOppfolgingKandidatResponseDTO>>()
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
                testApplication {
                    val client = setupApiAndClient()

                    val response = client.get(kandidaterUrl)
                    response.status shouldBeEqualTo HttpStatusCode.Unauthorized
                }
            }

            it("Returns status Forbidden if denied access to person") {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(kandidaterUrl) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS.value)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.Forbidden
                }
            }

            it("Returns status BadRequest if no $NAV_PERSONIDENT_HEADER is supplied") {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(kandidaterUrl) {
                        bearerAuth(validToken)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.BadRequest
                }
            }

            it("Returns status BadRequest if $NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied") {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(kandidaterUrl) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value.drop(1))
                    }

                    response.status shouldBeEqualTo HttpStatusCode.BadRequest
                }
            }
        }

        describe("Ferdigbehandle kandidat") {
            val kandidatUuid = senOppfolgingKandidat.uuid
            val ferdigbehandlingUrl = "$senOppfolgingApiBasePath/kandidater/$kandidatUuid/vurderinger"
            val vurderingRequestDTO =
                SenOppfolgingVurderingRequestDTO(begrunnelse = "Begrunnelse", type = VurderingType.FERDIGBEHANDLET)

            it("Returns OK if request is successful") {
                senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(ferdigbehandlingUrl) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                        contentType(ContentType.Application.Json)
                        setBody(vurderingRequestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.Created
                    val kandidatResponse = response.body<SenOppfolgingKandidatResponseDTO>()
                    kandidatResponse.uuid shouldBeEqualTo kandidatUuid
                    kandidatResponse.status shouldBeEqualTo SenOppfolgingStatus.FERDIGBEHANDLET
                    kandidatResponse.vurderinger.first().begrunnelse shouldBeEqualTo "Begrunnelse"
                    val ferdigbehandletVurdering =
                        kandidatResponse.vurderinger.first { it.type == VurderingType.FERDIGBEHANDLET }
                    ferdigbehandletVurdering.veilederident shouldBeEqualTo UserConstants.VEILEDER_IDENT
                }
            }

            it("Returns status BadRequest when unknown kandidat") {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post("$senOppfolgingApiBasePath/kandidater/${UUID.randomUUID()}/vurderinger") {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                        contentType(ContentType.Application.Json)
                        setBody(vurderingRequestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.BadRequest
                }
            }

            it("Returns status Conflict when kandidat already ferdigbehandlet") {
                senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)

                testApplication {
                    val client = setupApiAndClient()
                    client.post(ferdigbehandlingUrl) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                        contentType(ContentType.Application.Json)
                        setBody(vurderingRequestDTO)
                    }.apply {
                        status shouldBeEqualTo HttpStatusCode.Created
                    }

                    client.post(ferdigbehandlingUrl) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                        contentType(ContentType.Application.Json)
                        setBody(vurderingRequestDTO)
                    }.apply {
                        status shouldBeEqualTo HttpStatusCode.Conflict
                    }
                }
            }

            it("Returns status Unauthorized if no token is supplied") {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(ferdigbehandlingUrl)

                    response.status shouldBeEqualTo HttpStatusCode.Unauthorized
                }
            }

            it("Returns status Forbidden if denied access to person") {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(ferdigbehandlingUrl) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS.value)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.Forbidden
                }
            }

            it("Returns status BadRequest if no $NAV_PERSONIDENT_HEADER is supplied") {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(ferdigbehandlingUrl) {
                        bearerAuth(validToken)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.BadRequest
                }
            }

            it("Returns status BadRequest if $NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied") {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(ferdigbehandlingUrl) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value.drop(1))
                    }

                    response.status shouldBeEqualTo HttpStatusCode.BadRequest
                }
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

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(kandidaterPath) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val responseDTO = response.body<SenOppfolgingKandidaterResponseDTO>()

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

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(kandidaterPath) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val responseDTO = response.body<SenOppfolgingKandidaterResponseDTO>()

                    responseDTO.kandidater.size shouldBeEqualTo 1
                    responseDTO.kandidater.values.first().personident shouldBeEqualTo UserConstants.ARBEIDSTAKER_PERSONIDENT.value
                }
            }
            it("Returnerer NoContent når veilder har tilgang, men ingen kandidater") {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(kandidaterPath) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.NoContent
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

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(kandidaterPath) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val responseDTO = response.body<SenOppfolgingKandidaterResponseDTO>()

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

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(kandidaterPath) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val responseDTO = response.body<SenOppfolgingKandidaterResponseDTO>()

                    responseDTO.kandidater.size shouldBeEqualTo 1
                    responseDTO.kandidater.values.first().createdAt shouldBeAfter LocalDateTime.now()
                    responseDTO.kandidater.values.first().uuid shouldBeEqualTo latestUUID
                    responseDTO.kandidater.values.first().vurderinger.first().begrunnelse shouldBeEqualTo "Begrunnelse for ny vurdering"
                }
            }
        }
    }
})
