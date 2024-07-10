package no.nav.syfo.api.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.SenOppfolgingService
import no.nav.syfo.infrastructure.NAV_PERSONIDENT_HEADER
import no.nav.syfo.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.infrastructure.clients.veiledertilgang.validateVeilederAccess
import no.nav.syfo.util.getNAVIdent
import no.nav.syfo.util.getPersonident
import java.util.*

const val kandidatUuidParam = "vedtakUUID"
const val senOppfolgingApiBasePath = "/api/internad/v1/senoppfolging"
const val ferdigbehandlingPath = "/kandidat/{$kandidatUuidParam}/ferdigbehandling"

private const val API_ACTION = "access sen oppfolging kandidat for person"

fun Route.registerSenOppfolgingEndpoints(
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    senOppfolgingService: SenOppfolgingService,
) {
    route(senOppfolgingApiBasePath) {
        put(ferdigbehandlingPath) {
            val personident = call.getPersonident()
                ?: throw IllegalArgumentException("Failed to $API_ACTION: No $NAV_PERSONIDENT_HEADER supplied in request header")
            validateVeilederAccess(
                action = API_ACTION,
                personident = personident,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            ) {
                val kandidatUuid = UUID.fromString(this.call.parameters[kandidatUuidParam])
                val veilederIdent = call.getNAVIdent()
                call.respond(HttpStatusCode.OK)
                /* TODO
                - get kandidat by personident and uuid
                - validate not ferdigbehandlet
                - call service to ferdigbehandle
                 */
            }
        }
    }
}
