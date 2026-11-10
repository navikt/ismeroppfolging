package no.nav.syfo.shared.infrastructure.clients.wellknown

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.shared.infrastructure.clients.httpClientProxy

private val wellKnownHttpClient by lazy { httpClientProxy() }

fun getWellKnown(wellKnownUrl: String): WellKnown =
    runBlocking {
        wellKnownHttpClient.get(wellKnownUrl).body<WellKnownDTO>().toWellKnown()
    }
