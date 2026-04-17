package no.nav.syfo.shared.infrastructure.clients.veiledertilgang

import io.ktor.server.routing.*
import no.nav.syfo.shared.util.exception.ForbiddenAccessVeilederException
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.util.getBearerHeader
import no.nav.syfo.shared.util.getCallId

suspend fun RoutingContext.validateVeilederAccess(
    action: String,
    personident: Personident,
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    requiresWriteAccess: Boolean = false,
    requestBlock: suspend () -> Unit,
) {
    val callId = call.getCallId()
    val token = call.getBearerHeader() ?: throw IllegalArgumentException("Failed to complete the following action: $action. No Authorization header supplied")

    val hasVeilederAccess = if (requiresWriteAccess) {
        veilederTilgangskontrollClient.hasWriteAccess(
            callId = callId,
            personident = personident,
            token = token,
        )
    } else {
        veilederTilgangskontrollClient.hasAccess(
            callId = callId,
            personident = personident,
            token = token,
        )
    }

    if (hasVeilederAccess) {
        requestBlock()
    } else {
        throw ForbiddenAccessVeilederException(
            action = action,
        )
    }
}
