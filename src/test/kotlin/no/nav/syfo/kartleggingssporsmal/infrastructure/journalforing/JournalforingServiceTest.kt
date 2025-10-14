package no.nav.syfo.kartleggingssporsmal.infrastructure.journalforing

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.infrastructure.clients.pdfgen.PdfGenClient
import no.nav.syfo.kartleggingssporsmal.domain.KandidatStatus
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.generators.generateJournalpostRequest
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.DokarkivClient
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.BrevkodeType
import no.nav.syfo.kartleggingssporsmal.infrastructure.mock.dokarkivResponse
import no.nav.syfo.kartleggingssporsmal.infrastructure.mock.mockedJournalpostId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.getOrThrow
import kotlin.test.assertEquals

class JournalforingServiceTest {

    val externalMockEnvironment = ExternalMockEnvironment.instance
    val dokarkivMock = mockk<DokarkivClient>(relaxed = true)
    val pdfClientMock = mockk<PdfGenClient>(relaxed = true)
    val journalforingService = JournalforingService(
        dokarkivClient = dokarkivMock,
        pdlClient = externalMockEnvironment.pdlClient,
        pdfClient = pdfClientMock,
        isJournalforingRetryEnabled = externalMockEnvironment.environment.isJournalforingRetryEnabled,
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        coEvery { dokarkivMock.journalfor(any()) } returns dokarkivResponse
        coEvery { pdfClientMock.createKartleggingPdf(any(), any()) } returns UserConstants.PDF_DOKUMENT
    }

    @Test
    fun `sender forventet journalpost til dokarkiv`() {
        val kandidat = KartleggingssporsmalKandidat(
            personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
            status = KandidatStatus.KANDIDAT,
        )
        val journalpostId = runBlocking {
            journalforingService.journalfor(
                kandidat = kandidat,
            )
        }.getOrThrow()

        assertEquals(journalpostId, mockedJournalpostId)

        coVerify(exactly = 1) {
            dokarkivMock.journalfor(
                journalpostRequest = generateJournalpostRequest(
                    tittel = "Varsel om kartleggingsspørsmål",
                    brevkodeType = BrevkodeType.VARSEL_KARTLEGGINGSSPORSMAL,
                    pdf = UserConstants.PDF_DOKUMENT,
                    eksternReferanse = kandidat.uuid,
                    mottakerPersonident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
                    mottakerNavn = UserConstants.PERSON_FULLNAME,
                    brukerPersonident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
                )
            )
        }
    }

    @Test
    fun `feiler når kall til pdl feiler`() {
        val kandidat = KartleggingssporsmalKandidat(
            personident = UserConstants.ARBEIDSTAKER_PERSONIDENT_PDL_FAILS,
            status = KandidatStatus.KANDIDAT,
        )

        val result = runBlocking {
            journalforingService.journalfor(
                kandidat = kandidat,
            )
        }

        assertEquals(result.isFailure, true)

        coVerify(exactly = 0) { dokarkivMock.journalfor(any()) }
    }
}
