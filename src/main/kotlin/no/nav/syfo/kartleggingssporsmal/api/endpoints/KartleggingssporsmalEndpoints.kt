package no.nav.syfo.kartleggingssporsmal.api.endpoints

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.kartleggingssporsmal.application.KartleggingssporsmalService
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.shared.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.shared.infrastructure.clients.veiledertilgang.validateVeilederAccess
import no.nav.syfo.shared.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.shared.util.getPersonident

private const val API_ACTION = "access received kartleggingssporsmål for person"

fun Route.registerKartleggingssporsmalEndpoints(
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    kartleggingssporsmalService: KartleggingssporsmalService,
) {
    route("/api/internad/v1/kartleggingssporsmal") {
        get("/kandidater") {
            val personident = call.getPersonident()
                ?: throw IllegalArgumentException("Failed to $API_ACTION: No $NAV_PERSONIDENT_HEADER supplied in request header")
            validateVeilederAccess(
                action = API_ACTION,
                personident = personident,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            ) {
                val kandidat = kartleggingssporsmalService.getKandidat(personident)
                if (kandidat != null) {
                    call.respond<KartleggingssporsmalKandidat>(HttpStatusCode.OK, kandidat)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
