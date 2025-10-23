package no.nav.syfo.kartleggingssporsmal.api.endpoints

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.kartleggingssporsmal.api.endpoints.dto.KandidatStatusDTO
import no.nav.syfo.kartleggingssporsmal.api.endpoints.dto.toKandidatStatusDTO
import no.nav.syfo.kartleggingssporsmal.application.KartleggingssporsmalService
import no.nav.syfo.shared.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.shared.infrastructure.clients.veiledertilgang.validateVeilederAccess
import no.nav.syfo.shared.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.shared.util.getNAVIdent
import no.nav.syfo.shared.util.getPersonident
import java.util.UUID

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
                    val kandidatStatusListe = kartleggingssporsmalService.getKandidatStatus(kandidat.uuid)
                    call.respond<KandidatStatusDTO>(
                        status = HttpStatusCode.OK,
                        message = kandidat.toKandidatStatusDTO(kandidatStatusListe)
                    )
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        put("/kandidater/{uuid}") {
            val veilederident = call.getNAVIdent()
                ?: throw IllegalArgumentException("Failed to $API_ACTION: No NAV_IDENT supplied in request header")
            val kandidatUUID = call.parameters["uuid"]?.let { UUID.fromString(it) }
                ?: throw IllegalArgumentException("Failed to $API_ACTION: No kandidat UUID supplied in request path")
            val kandidat = kartleggingssporsmalService.getKandidat(kandidatUUID)
                ?: throw IllegalArgumentException("Failed to $API_ACTION: No kandidat found for UUID $kandidatUUID")

            validateVeilederAccess(
                action = API_ACTION,
                personident = kandidat.personident,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            ) {
                val kandidat = kartleggingssporsmalService.registrerFerdigbehandlet(
                    uuid = kandidatUUID,
                    veilederident = veilederident,
                )
                val kandidatStatusListe = kartleggingssporsmalService.getKandidatStatus(kandidat.uuid)

                call.respond<KandidatStatusDTO>(
                    status = HttpStatusCode.OK,
                    message = kandidat.toKandidatStatusDTO(kandidatStatusListe)
                )
            }
        }
    }
}
