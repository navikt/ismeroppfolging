package no.nav.syfo.shared.util

import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.server.application.*
import no.nav.syfo.shared.domain.Personident

const val JWT_CLAIM_AZP = "azp"
const val JWT_CLAIM_NAVIDENT = "NAVident"

fun ApplicationCall.getCallId(): String = this.request.headers[NAV_CALL_ID_HEADER].toString()

fun ApplicationCall.getPersonident(): Personident? =
    this.request.headers[NAV_PERSONIDENT_HEADER]?.let { Personident(it) }

fun ApplicationCall.getConsumerClientId(): String? =
    getBearerHeader()?.let {
        JWT.decode(it).claims[JWT_CLAIM_AZP]?.asString()
    }

fun ApplicationCall.getNAVIdent(): String {
    val token = getBearerHeader() ?: throw Error("No Authorization header supplied")
    return JWT.decode(token).claims[JWT_CLAIM_NAVIDENT]?.asString()
        ?: throw Error("Missing NAVident in private claims")
}

fun ApplicationCall.getBearerHeader(): String? =
    this.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
