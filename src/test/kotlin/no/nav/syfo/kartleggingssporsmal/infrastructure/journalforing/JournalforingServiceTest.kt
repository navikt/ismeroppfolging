package no.nav.syfo.kartleggingssporsmal.infrastructure.journalforing

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.infrastructure.clients.pdfgen.BrevData
import no.nav.syfo.infrastructure.clients.pdfgen.PdfGenClient
import no.nav.syfo.infrastructure.clients.pdfgen.PdfModel
import no.nav.syfo.kartleggingssporsmal.application.KartleggingssporsmalService
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidatStatusendring
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfelleFromKafka
import no.nav.syfo.kartleggingssporsmal.generators.generateJournalpostRequest
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.DokarkivClient
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.BrevkodeType
import no.nav.syfo.kartleggingssporsmal.infrastructure.database.KartleggingssporsmalRepository
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.EsyfovarselHendelse
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.EsyfovarselProducer
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.KartleggingssporsmalKandidatProducer
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.KartleggingssporsmalKandidatStatusRecord
import no.nav.syfo.kartleggingssporsmal.infrastructure.mock.dokarkivResponse
import no.nav.syfo.kartleggingssporsmal.infrastructure.mock.mockedJournalpostId
import no.nav.syfo.shared.infrastructure.database.dropData
import no.nav.syfo.shared.infrastructure.database.updateKandidatAsVarslet
import no.nav.syfo.shared.util.DAYS_IN_WEEK
import no.nav.syfo.shared.util.toLocalDateOslo
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class JournalforingServiceTest {

    val externalMockEnvironment = ExternalMockEnvironment.instance
    val testDatabase = externalMockEnvironment.database
    val dokarkivMock = mockk<DokarkivClient>(relaxed = true)
    val pdfClientMock = mockk<PdfGenClient>(relaxed = true)
    val kartleggingssporsmalRepository = KartleggingssporsmalRepository(testDatabase)
    val journalforingService = JournalforingService(
        kartleggingssporsmalRepository = kartleggingssporsmalRepository,
        dokarkivClient = dokarkivMock,
        pdlClient = externalMockEnvironment.pdlClient,
        pdfClient = pdfClientMock,
        isJournalforingRetryEnabled = externalMockEnvironment.environment.isJournalforingRetryEnabled,
    )

    private val mockEsyfoVarselProducer = mockk<KafkaProducer<String, EsyfovarselHendelse>>()
    private val esyfovarselProducer = EsyfovarselProducer(mockEsyfoVarselProducer)
    private val mockKandidatProducer = mockk<KafkaProducer<String, KartleggingssporsmalKandidatStatusRecord>>()
    private val kartleggingssporsmalKandidatProducer = KartleggingssporsmalKandidatProducer(mockKandidatProducer)

    private val kartleggingssporsmalService = KartleggingssporsmalService(
        behandlendeEnhetClient = externalMockEnvironment.behandlendeEnhetClient,
        kartleggingssporsmalRepository = kartleggingssporsmalRepository,
        oppfolgingstilfelleClient = externalMockEnvironment.oppfolgingstilfelleClient,
        esyfoVarselProducer = esyfovarselProducer,
        kartleggingssporsmalKandidatProducer = kartleggingssporsmalKandidatProducer,
        pdlClient = externalMockEnvironment.pdlClient,
        vedtak14aClient = externalMockEnvironment.vedtak14aClient,
        senOppfolgingService = externalMockEnvironment.senOppfolgingService,
        isKandidatPublishingEnabled = false,
    )
    val stoppunktStartIntervalDays = 6L * DAYS_IN_WEEK

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        coEvery { dokarkivMock.journalfor(any()) } returns dokarkivResponse
        coEvery { pdfClientMock.createKartleggingPdf(any(), any()) } returns UserConstants.PDF_DOKUMENT
        testDatabase.dropData()
    }

    @Test
    fun `sender forventet journalpost til dokarkiv`() {
        val tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays)
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            personident = ARBEIDSTAKER_PERSONIDENT,
            tilfelleStart = tilfelleStart,
            tilfelleEnd = LocalDate.now(),
        )
        val firstKandidat = runBlocking {
            kartleggingssporsmalRepository.createStoppunkt(KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)!!)
            kartleggingssporsmalService.processStoppunkter()
            kartleggingssporsmalRepository.getLatestKandidat(ARBEIDSTAKER_PERSONIDENT)
        }

        assertTrue(firstKandidat?.status is KartleggingssporsmalKandidatStatusendring.Kandidat)
        testDatabase.updateKandidatAsVarslet(firstKandidat!!)

        val varsletKandidat = runBlocking {
            kartleggingssporsmalRepository.getKandidat(firstKandidat.uuid)!!
        }

        val journalpostId = runBlocking {
            journalforingService.journalforKandidater()
        }.first().getOrThrow()

        assertEquals(journalpostId, mockedJournalpostId)

        coVerify(exactly = 1) {
            pdfClientMock.createKartleggingPdf(
                payload = PdfModel(
                    brevdata = BrevData(
                        createdAt = varsletKandidat.varsletAt!!.toLocalDateOslo().format(PdfModel.formatter)
                    ),
                ),
                callId = any(),
            )
        }
        coVerify(exactly = 1) {
            dokarkivMock.journalfor(
                journalpostRequest = generateJournalpostRequest(
                    tittel = "Varsel om kartleggingsspørsmål",
                    brevkodeType = BrevkodeType.VARSEL_KARTLEGGINGSSPORSMAL,
                    pdf = UserConstants.PDF_DOKUMENT,
                    eksternReferanse = firstKandidat.uuid,
                    mottakerPersonident = ARBEIDSTAKER_PERSONIDENT,
                    mottakerNavn = UserConstants.PERSON_FULLNAME,
                    brukerPersonident = ARBEIDSTAKER_PERSONIDENT,
                )
            )
        }
        val updatedKandidat = runBlocking {
            kartleggingssporsmalRepository.getLatestKandidat(ARBEIDSTAKER_PERSONIDENT)
        }
        assertEquals(journalpostId, updatedKandidat!!.journalpostId)
    }

    @Test
    fun `feiler når kall til pdl feiler`() {
        val kandidat = KartleggingssporsmalKandidat.create(personident = UserConstants.ARBEIDSTAKER_PERSONIDENT_PDL_FAILS)

        val result = runBlocking {
            journalforingService.journalfor(
                kandidat = kandidat,
            )
        }

        assertEquals(result.isFailure, true)

        coVerify(exactly = 0) { dokarkivMock.journalfor(any()) }
    }
}
