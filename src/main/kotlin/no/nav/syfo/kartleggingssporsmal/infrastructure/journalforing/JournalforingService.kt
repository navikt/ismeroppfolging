package no.nav.syfo.kartleggingssporsmal.infrastructure.journalforing

import no.nav.syfo.infrastructure.clients.pdfgen.PdfGenClient
import no.nav.syfo.infrastructure.clients.pdfgen.PdfModel
import no.nav.syfo.kartleggingssporsmal.application.IJournalforingService
import no.nav.syfo.kartleggingssporsmal.domain.JournalpostId
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.DokarkivClient
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.AvsenderMottaker
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.BrevkodeType
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.Bruker
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.BrukerIdType
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.Dokument
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.Dokumentvariant
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.FiltypeType
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.JournalpostRequest
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.VariantformatType
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.pdl.PdlClient
import org.slf4j.LoggerFactory

class JournalforingService(
    private val dokarkivClient: DokarkivClient,
    private val pdlClient: PdlClient,
    private val pdfClient: PdfGenClient,
    private val isJournalforingRetryEnabled: Boolean,
) : IJournalforingService {

    override suspend fun journalfor(kandidat: KartleggingssporsmalKandidat): Result<JournalpostId> = runCatching {
        val navn = pdlClient.getPerson(kandidat.personident).getOrNull()?.fullName
            ?: throw IllegalStateException("Klarte ikke hente navn fra PDL for personident ${kandidat.personident}")
        val pdf = pdfClient.createKartleggingPdf(
            payload = PdfModel.KartleggingPdfModel(
                mottakerFodselsnummer = kandidat.personident,
                mottakerNavn = navn,
            ),
            callId = kandidat.uuid.toString(),
        )
        val journalpostRequest = createJournalpostRequest(
            kandidat = kandidat,
            navn = navn,
            pdf = pdf
        )

        val journalpostId = try {
            dokarkivClient.journalfor(journalpostRequest).journalpostId
        } catch (exc: Exception) {
            if (isJournalforingRetryEnabled) {
                throw exc
            } else {
                log.error("Journalføring failed, skipping retry (should only happen in dev-gcp)", exc)
                // Defaulting'en til DEFAULT_FAILED_JP_ID skal bare forekomme i dev-gcp:
                // Har dette fordi vi ellers spammer ned dokarkiv med forsøk på å journalføre
                // på personer som mangler aktør-id.
                DEFAULT_FAILED_JP_ID
            }
        }
        JournalpostId(journalpostId.toString())
    }

    private fun createJournalpostRequest(
        kandidat: KartleggingssporsmalKandidat,
        navn: String,
        pdf: ByteArray,
    ): JournalpostRequest {
        val avsenderMottaker = AvsenderMottaker.create(
            id = kandidat.personident.value,
            idType = BrukerIdType.PERSON_IDENT,
            navn = navn,
        )
        val bruker = Bruker.create(
            id = kandidat.personident.value,
            idType = BrukerIdType.PERSON_IDENT,
        )

        val dokumenter = listOf(
            Dokument.create(
                brevkode = BrevkodeType.VARSEL_KARTLEGGINGSSPORSMAL,
                dokumentvarianter = listOf(
                    Dokumentvariant.create(
                        filnavn = TITTEL,
                        filtype = FiltypeType.PDFA,
                        fysiskDokument = pdf,
                        variantformat = VariantformatType.ARKIV,
                    )
                ),
                tittel = TITTEL,
            ),
        )

        return JournalpostRequest(
            avsenderMottaker = avsenderMottaker,
            tittel = TITTEL,
            bruker = bruker,
            dokumenter = dokumenter,
            eksternReferanseId = kandidat.uuid.toString(),
        )
    }
    companion object {
        const val DEFAULT_FAILED_JP_ID = 0
        const val TITTEL = "Varsel om kartleggingsspørsmål"
        private val log = LoggerFactory.getLogger(JournalforingService::class.java)
    }
}
