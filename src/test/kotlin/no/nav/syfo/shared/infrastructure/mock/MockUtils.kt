package no.nav.syfo.shared.infrastructure.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.shared.util.configuredJacksonMapper

val mapper = configuredJacksonMapper()

fun <T> MockRequestHandleScope.respond(body: T, statusCode: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
    respond(
        mapper.writeValueAsString(body),
        statusCode,
        headersOf(HttpHeaders.ContentType, "application/json")
    )

suspend inline fun <reified T> HttpRequestData.receiveBody(): T {
    return mapper.readValue(body.toByteArray(), T::class.java)
}
