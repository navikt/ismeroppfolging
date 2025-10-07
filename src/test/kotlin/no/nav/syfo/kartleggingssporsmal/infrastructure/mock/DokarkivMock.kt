package no.nav.syfo.kartleggingssporsmal.infrastructure.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.UserConstants
import no.nav.syfo.kartleggingssporsmal.domain.JournalpostId
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.JournalpostRequest
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.JournalpostResponse
import no.nav.syfo.shared.infrastructure.mock.receiveBody
import no.nav.syfo.shared.infrastructure.mock.respond

const val journalpostId = 123
val mockedJournalpostId = JournalpostId(journalpostId.toString())
val dokarkivResponse = JournalpostResponse(
    journalpostId = journalpostId,
    journalpostferdigstilt = true,
    journalstatus = "status",
)

val dokarkivConflictResponse = JournalpostResponse(
    journalpostId = 2,
    journalpostferdigstilt = true,
    journalstatus = "conflict",
)

suspend fun MockRequestHandleScope.dokarkivMockResponse(request: HttpRequestData): HttpResponseData {
    val journalpostRequest = request.receiveBody<JournalpostRequest>()
    val eksternReferanseId = journalpostRequest.eksternReferanseId

    return when (eksternReferanseId) {
        UserConstants.EXISTING_EKSTERN_REFERANSE_UUID.toString() -> respond(dokarkivConflictResponse, HttpStatusCode.Conflict)
        UserConstants.FAILING_EKSTERN_REFERANSE_UUID.toString() -> respondError(HttpStatusCode.InternalServerError, "Journalføring failed")
        else -> respond(dokarkivResponse)
    }
}
