package no.nav.syfo.infrastructure.clients.veiledertilgang

import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import no.nav.syfo.domain.Personident
import no.nav.syfo.util.getBearerHeader
import no.nav.syfo.util.getCallId

suspend fun PipelineContext<out Unit, ApplicationCall>.validateVeilederAccess(
    action: String,
    personident: Personident,
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    requestBlock: suspend () -> Unit,
) {
    val callId = call.getCallId()
    val token = call.getBearerHeader() ?: throw IllegalArgumentException("Failed to complete the following action: $action. No Authorization header supplied")

    val hasVeilederAccess = veilederTilgangskontrollClient.hasAccess(
        callId = callId,
        personIdent = personident,
        token = token,
    )

    if (hasVeilederAccess) {
        requestBlock()
    } else {
        throw ForbiddenAccessVeilederException(
            action = action,
        )
    }
}
