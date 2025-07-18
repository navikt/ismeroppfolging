package no.nav.syfo.shared.api

import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import no.nav.syfo.ApplicationState
import no.nav.syfo.Environment
import no.nav.syfo.shared.api.auth.JwtIssuer
import no.nav.syfo.shared.api.auth.JwtIssuerType
import no.nav.syfo.shared.api.auth.installJwtAuthentication
import no.nav.syfo.shared.api.auth.metricEndpoints
import no.nav.syfo.shared.api.auth.podEndpoints
import no.nav.syfo.senoppfolging.api.endpoints.registerSenOppfolgingEndpoints
import no.nav.syfo.senoppfolging.application.SenOppfolgingService
import no.nav.syfo.shared.util.exception.ConflictException
import no.nav.syfo.shared.util.exception.ForbiddenAccessVeilederException
import no.nav.syfo.shared.util.NAV_CALL_ID_HEADER
import no.nav.syfo.shared.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.shared.infrastructure.clients.wellknown.WellKnown
import no.nav.syfo.shared.infrastructure.database.DatabaseInterface
import no.nav.syfo.shared.infrastructure.metric.METRICS_REGISTRY
import no.nav.syfo.shared.util.configure
import no.nav.syfo.shared.util.getCallId
import no.nav.syfo.shared.util.getConsumerClientId
import java.time.Duration
import java.util.*

fun Application.apiModule(
    applicationState: ApplicationState,
    environment: Environment,
    wellKnownInternalAzureAD: WellKnown,
    database: DatabaseInterface,
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    senOppfolgingService: SenOppfolgingService,
) {
    installMetrics()
    installCallId()
    installContentNegotiation()
    installStatusPages()
    installJwtAuthentication(
        jwtIssuerList =
        listOf(
            JwtIssuer(
                acceptedAudienceList = listOf(environment.azure.appClientId),
                jwtIssuerType = JwtIssuerType.INTERNAL_AZUREAD,
                wellKnown = wellKnownInternalAzureAD
            )
        )
    )

    routing {
        podEndpoints(applicationState = applicationState, database = database)
        metricEndpoints()
        authenticate(JwtIssuerType.INTERNAL_AZUREAD.name) {
            registerSenOppfolgingEndpoints(
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
                senOppfolgingService = senOppfolgingService,
            )
        }
    }
}

fun Application.installContentNegotiation() {
    install(ContentNegotiation) {
        jackson { configure() }
    }
}

fun Application.installMetrics() {
    install(MicrometerMetrics) {
        registry = METRICS_REGISTRY
        distributionStatisticConfig =
            DistributionStatisticConfig.Builder()
                .percentilesHistogram(true)
                .maximumExpectedValue(Duration.ofSeconds(20).toNanos().toDouble())
                .build()
    }
}

fun Application.installCallId() {
    install(CallId) {
        retrieve { it.request.headers[NAV_CALL_ID_HEADER] }
        generate { UUID.randomUUID().toString() }
        verify { callId: String -> callId.isNotEmpty() }
        header(NAV_CALL_ID_HEADER)
    }
}

fun Application.installStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val callId = call.getCallId()
            val consumerClientId = call.getConsumerClientId()
            val logExceptionMessage = "Caught exception, callId=$callId, consumerClientId=$consumerClientId"
            val log = call.application.log
            when (cause) {
                is ForbiddenAccessVeilederException -> {
                    log.warn(logExceptionMessage, cause)
                }

                else -> {
                    log.error(logExceptionMessage, cause)
                }
            }

            var isUnexpectedException = false

            val responseStatus: HttpStatusCode =
                when (cause) {
                    is ResponseException -> {
                        cause.response.status
                    }

                    is IllegalArgumentException -> {
                        HttpStatusCode.BadRequest
                    }

                    is ForbiddenAccessVeilederException -> {
                        HttpStatusCode.Forbidden
                    }
                    is ConflictException -> {
                        HttpStatusCode.Conflict
                    }

                    else -> {
                        isUnexpectedException = true
                        HttpStatusCode.InternalServerError
                    }
                }
            val message =
                if (isUnexpectedException) {
                    "The server reported an unexpected error and cannot complete the request."
                } else {
                    cause.message ?: "Unknown error"
                }
            call.respond(responseStatus, message)
        }
    }
}
