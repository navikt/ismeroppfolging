package no.nav.syfo.infrastructure.clients.veiledertilgang

import io.ktor.server.routing.*
import no.nav.syfo.application.exception.ForbiddenAccessVeilederException
import no.nav.syfo.domain.Personident
import no.nav.syfo.util.getBearerHeader
import no.nav.syfo.util.getCallId

suspend fun RoutingContext.validateVeilederAccess(
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
