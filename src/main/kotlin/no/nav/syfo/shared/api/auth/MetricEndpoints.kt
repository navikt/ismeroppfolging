package no.nav.syfo.shared.api.auth

import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.shared.infrastructure.metric.METRICS_REGISTRY

const val podMetricsPath = "/internal/metrics"

fun Routing.metricEndpoints() {
    get(podMetricsPath) {
        call.respondText(METRICS_REGISTRY.scrape())
    }
}
