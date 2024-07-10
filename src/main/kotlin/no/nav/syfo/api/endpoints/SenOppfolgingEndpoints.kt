package no.nav.syfo.api.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.api.model.SenOppfolgingVurderingRequestDTO
import no.nav.syfo.api.model.toResponseDTO
import no.nav.syfo.application.SenOppfolgingService
import no.nav.syfo.infrastructure.NAV_PERSONIDENT_HEADER
import no.nav.syfo.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.infrastructure.clients.veiledertilgang.validateVeilederAccess
import no.nav.syfo.util.getNAVIdent
import no.nav.syfo.util.getPersonident
import java.util.*

const val kandidatUuidParam = "vedtakUUID"
const val senOppfolgingApiBasePath = "/api/internad/v1/senoppfolging"
const val kandidaterPath = "/kandidater"

private const val API_ACTION = "access sen oppfolging kandidat for person"

fun Route.registerSenOppfolgingEndpoints(
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    senOppfolgingService: SenOppfolgingService,
) {
    route(senOppfolgingApiBasePath) {
        get(kandidaterPath) {
            val personident = call.getPersonident()
                ?: throw IllegalArgumentException("Failed to $API_ACTION: No $NAV_PERSONIDENT_HEADER supplied in request header")
            validateVeilederAccess(
                action = API_ACTION,
                personident = personident,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            ) {
                val kandidater = senOppfolgingService.getKandidater(personident)

                call.respond(HttpStatusCode.OK, kandidater.map { it.toResponseDTO() })
            }
        }
        post("$kandidaterPath/{$kandidatUuidParam}/vurderinger") {
            val personident = call.getPersonident()
                ?: throw IllegalArgumentException("Failed to $API_ACTION: No $NAV_PERSONIDENT_HEADER supplied in request header")
            validateVeilederAccess(
                action = API_ACTION,
                personident = personident,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            ) {
                val kandidatUuid = UUID.fromString(this.call.parameters[kandidatUuidParam])
                val requestDTO = call.receive<SenOppfolgingVurderingRequestDTO>()
                val veilederIdent = call.getNAVIdent()

                val senOppfolgingKandidat = senOppfolgingService.getKandidat(kandidatUuid = kandidatUuid)
                    ?.takeIf { it.personident == personident }

                if (senOppfolgingKandidat == null) {
                    call.respond(HttpStatusCode.BadRequest, "Finner ikke kandidat med uuid $kandidatUuid for person")
                } else {
                    val vurdertKandidat = senOppfolgingService.vurderKandidat(
                        kandidat = senOppfolgingKandidat,
                        veilederident = veilederIdent,
                        type = requestDTO.type,
                    )

                    call.respond(HttpStatusCode.OK, vurdertKandidat.toResponseDTO())
                }
            }
        }
    }
}
