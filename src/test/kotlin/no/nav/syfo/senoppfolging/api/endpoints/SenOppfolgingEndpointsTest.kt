package no.nav.syfo.senoppfolging.api.endpoints

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.shared.api.generateJWT
import no.nav.syfo.senoppfolging.api.model.SenOppfolgingKandidatResponseDTO
import no.nav.syfo.senoppfolging.api.model.SenOppfolgingKandidaterRequestDTO
import no.nav.syfo.senoppfolging.api.model.SenOppfolgingKandidaterResponseDTO
import no.nav.syfo.senoppfolging.api.model.SenOppfolgingVurderingRequestDTO
import no.nav.syfo.shared.api.testApiModule
import no.nav.syfo.shared.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.senoppfolging.infrastructure.database.repository.SenOppfolgingRepository
import no.nav.syfo.senoppfolging.domain.*
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.util.configure
import no.nav.syfo.shared.util.nowUTC
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*

class SenOppfolgingEndpointsTest {

    private val kandidatVarselAt = nowUTC()
    private val senOppfolgingKandidat = SenOppfolgingKandidat(
        personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
        varselAt = kandidatVarselAt,
    )
    private val kandidatSvarAt = nowUTC()
    private val svar = SenOppfolgingSvar(svarAt = kandidatSvarAt, onskerOppfolging = OnskerOppfolging.JA)

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val validToken = generateJWT(
        audience = externalMockEnvironment.environment.azure.appClientId,
        issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
        navIdent = UserConstants.VEILEDER_IDENT,
    )
    private val senOppfolgingRepository = SenOppfolgingRepository(database)

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

    @AfterEach
    fun tearDown() {
        database.resetDatabase()
    }

    @Nested
    @DisplayName("Get kandidat")
    inner class GetKandidat {

        private val kandidaterUrl = "$senOppfolgingApiBasePath/kandidater"

        @Test
        fun `Returns empty list when person not kandidat`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.get(kandidaterUrl) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                }

                assertEquals(HttpStatusCode.OK, response.status)
                val responseDTOs = response.body<List<SenOppfolgingKandidatResponseDTO>>()
                assertEquals(0, responseDTOs.size)
            }
        }

        @Test
        fun `Returns list of kandidat for person`() {
            senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)

            testApplication {
                val client = setupApiAndClient()
                val response = client.get(kandidaterUrl) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                }

                assertEquals(HttpStatusCode.OK, response.status)
                val responseDTOs = response.body<List<SenOppfolgingKandidatResponseDTO>>()
                assertEquals(1, responseDTOs.size)

                val kandidat = responseDTOs.first()
                assertEquals(senOppfolgingKandidat.uuid, kandidat.uuid)
                assertEquals(senOppfolgingKandidat.personident.value, kandidat.personident)
                assertEquals(senOppfolgingKandidat.createdAt.toLocalDateTime().withNano(0), kandidat.createdAt.withNano(0))
                assertEquals(kandidatVarselAt.toLocalDateTime().withNano(0), kandidat.varselAt?.withNano(0))
                assertNull(kandidat.svar)
            }
        }

        @Test
        fun `Returns kandidat with svar`() {
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

                assertEquals(HttpStatusCode.OK, response.status)
                val responseDTOs = response.body<List<SenOppfolgingKandidatResponseDTO>>()
                assertEquals(1, responseDTOs.size)

                val kandidat = responseDTOs.first()
                assertEquals(senOppfolgingKandidat.uuid, kandidat.uuid)
                assertNotNull(kandidat.svar)
                assertEquals(kandidatSvarAt.toLocalDateTime().withNano(0), kandidat.svar!!.svarAt.withNano(0))
                assertEquals(svar.onskerOppfolging, kandidat.svar.onskerOppfolging)
            }
        }

        @Test
        fun `Returns status Unauthorized if no token is supplied`() {
            testApplication {
                val client = setupApiAndClient()

                val response = client.get(kandidaterUrl)
                assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }

        @Test
        fun `Returns status Forbidden if denied access to person`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.get(kandidaterUrl) {
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
                val response = client.get(kandidaterUrl) {
                    bearerAuth(validToken)
                }

                assertEquals(HttpStatusCode.BadRequest, response.status)
            }
        }

        @Test
        fun `Returns status BadRequest if $NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.get(kandidaterUrl) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value.drop(1))
                }

                assertEquals(HttpStatusCode.BadRequest, response.status)
            }
        }
    }

    @Nested
    @DisplayName("Ferdigbehandle kandidat")
    inner class FerdigbehandleKandidat {

        private val kandidatUuid = senOppfolgingKandidat.uuid
        private val ferdigbehandlingUrl = "$senOppfolgingApiBasePath/kandidater/$kandidatUuid/vurderinger"
        private val vurderingRequestDTO =
            SenOppfolgingVurderingRequestDTO(begrunnelse = "Begrunnelse", type = VurderingType.FERDIGBEHANDLET)

        @Test
        fun `Returns OK if request is successful`() {
            senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)

            testApplication {
                val client = setupApiAndClient()
                val response = client.post(ferdigbehandlingUrl) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                    contentType(ContentType.Application.Json)
                    setBody(vurderingRequestDTO)
                }

                assertEquals(HttpStatusCode.Created, response.status)
                val kandidatResponse = response.body<SenOppfolgingKandidatResponseDTO>()
                assertEquals(kandidatUuid, kandidatResponse.uuid)
                assertEquals(SenOppfolgingStatus.FERDIGBEHANDLET, kandidatResponse.status)
                assertEquals("Begrunnelse", kandidatResponse.vurderinger.first().begrunnelse)
                val ferdigbehandletVurdering = kandidatResponse.vurderinger.first { it.type == VurderingType.FERDIGBEHANDLET }
                assertEquals(UserConstants.VEILEDER_IDENT, ferdigbehandletVurdering.veilederident)
            }
        }

        @Test
        fun `Returns status BadRequest when unknown kandidat`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.post("$senOppfolgingApiBasePath/kandidater/${UUID.randomUUID()}/vurderinger") {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                    contentType(ContentType.Application.Json)
                    setBody(vurderingRequestDTO)
                }

                assertEquals(HttpStatusCode.BadRequest, response.status)
            }
        }

        @Test
        fun `Returns status Conflict when kandidat already ferdigbehandlet`() {
            senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)

            testApplication {
                val client = setupApiAndClient()
                val firstResponse = client.post(ferdigbehandlingUrl) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                    contentType(ContentType.Application.Json)
                    setBody(vurderingRequestDTO)
                }
                assertEquals(HttpStatusCode.Created, firstResponse.status)

                val secondResponse = client.post(ferdigbehandlingUrl) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                    contentType(ContentType.Application.Json)
                    setBody(vurderingRequestDTO)
                }
                assertEquals(HttpStatusCode.Conflict, secondResponse.status)
            }
        }

        @Test
        fun `Returns status Unauthorized if no token is supplied`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.post(ferdigbehandlingUrl)

                assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }

        @Test
        fun `Returns status Forbidden if denied access to person`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.post(ferdigbehandlingUrl) {
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
                val response = client.post(ferdigbehandlingUrl) {
                    bearerAuth(validToken)
                }

                assertEquals(HttpStatusCode.BadRequest, response.status)
            }
        }

        @Test
        fun `Returns status BadRequest if $NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.post(ferdigbehandlingUrl) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value.drop(1))
                }

                assertEquals(HttpStatusCode.BadRequest, response.status)
            }
        }
    }

    @Nested
    @DisplayName("POST /get-kandidater")
    inner class PostGetKandidater {

        private val kandidaterPath = "$senOppfolgingApiBasePath/get-kandidater"
        private val otherPersonident = Personident("98765432123")
        private val personidenter = listOf(
            UserConstants.ARBEIDSTAKER_PERSONIDENT.value,
            otherPersonident.value
        )
        private val requestDTO = SenOppfolgingKandidaterRequestDTO(personidenter)

        @Test
        fun `Henter ut kandidater når veileder har tilgang til personene`() {
            senOppfolgingRepository.createKandidat(senOppfolgingKandidat = senOppfolgingKandidat)
            senOppfolgingRepository.createKandidat(
                senOppfolgingKandidat = senOppfolgingKandidat.copy(
                    uuid = UUID.randomUUID(),
                    personident = otherPersonident,
                    createdAt = nowUTC().plusSeconds(1)
                )
            )

            testApplication {
                val client = setupApiAndClient()
                val response = client.post(kandidaterPath) {
                    bearerAuth(validToken)
                    contentType(ContentType.Application.Json)
                    setBody(requestDTO)
                }

                assertEquals(HttpStatusCode.OK, response.status)
                val responseDTO = response.body<SenOppfolgingKandidaterResponseDTO>()

                assertEquals(2, responseDTO.kandidater.size)
                assertEquals(otherPersonident.value, responseDTO.kandidater.values.first().personident)
                assertEquals(UserConstants.ARBEIDSTAKER_PERSONIDENT.value, responseDTO.kandidater.values.last().personident)
            }
        }

        @Test
        fun `Henter ikke ut kandidater hvis veileder ikke har tilgang til personen`() {
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

                assertEquals(HttpStatusCode.OK, response.status)
                val responseDTO = response.body<SenOppfolgingKandidaterResponseDTO>()

                assertEquals(1, responseDTO.kandidater.size)
                assertEquals(UserConstants.ARBEIDSTAKER_PERSONIDENT.value, responseDTO.kandidater.values.first().personident)
            }
        }

        @Test
        fun `Returnerer NoContent når veilder har tilgang, men ingen kandidater`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.post(kandidaterPath) {
                    bearerAuth(validToken)
                    contentType(ContentType.Application.Json)
                    setBody(requestDTO)
                }

                assertEquals(HttpStatusCode.NoContent, response.status)
            }
        }

        @Test
        fun `Henter ut nyeste kandidat når personen har blitt kandidat flere ganger`() {
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

                assertEquals(HttpStatusCode.OK, response.status)
                val responseDTO = response.body<SenOppfolgingKandidaterResponseDTO>()

                assertEquals(1, responseDTO.kandidater.size)
                assertTrue(responseDTO.kandidater.values.first().createdAt.isAfter(LocalDateTime.now()))
                assertEquals(latestUUID, responseDTO.kandidater.values.first().uuid)
            }
        }

        @Test
        fun `Henter riktig vurdering for siste kandidat når kandidat har vurdering`() {
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

                assertEquals(HttpStatusCode.OK, response.status)
                val responseDTO = response.body<SenOppfolgingKandidaterResponseDTO>()

                assertEquals(1, responseDTO.kandidater.size)
                assertTrue(responseDTO.kandidater.values.first().createdAt.isAfter(LocalDateTime.now()))
                assertEquals(latestUUID, responseDTO.kandidater.values.first().uuid)
                assertEquals("Begrunnelse for ny vurdering", responseDTO.kandidater.values.first().vurderinger.first().begrunnelse)
            }
        }
    }
}
