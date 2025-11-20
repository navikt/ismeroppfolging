package no.nav.syfo.kartleggingssporsmal.application

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_ERROR
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_HAS_14A
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_INACTIVE
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_NO_ARBEIDSGIVER
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_TILFELLE_DOD
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT_DURATION_LEFT
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT_DURATION_LEFT_BUT_LONG
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_TOO_OLD
import no.nav.syfo.kartleggingssporsmal.domain.KandidatStatus
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidatStatusendring
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfelleFromKafka
import no.nav.syfo.kartleggingssporsmal.infrastructure.database.KartleggingssporsmalRepository
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.*
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.EsyfovarselHendelse.HendelseType
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.infrastructure.database.getKandidatByStoppunktUUID
import no.nav.syfo.shared.infrastructure.database.getKartleggingssporsmalStoppunkt
import no.nav.syfo.shared.infrastructure.database.markStoppunktAsProcessed
import no.nav.syfo.shared.util.DAYS_IN_WEEK
import no.nav.syfo.shared.util.fullDaysBetween
import no.nav.syfo.shared.util.toLocalDateOslo
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.Future
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KartleggingssporsmalServiceTest {
    private val database = ExternalMockEnvironment.instance.database
    private val kartleggingssporsmalRepository = KartleggingssporsmalRepository(database)

    private val mockEsyfoVarselProducer = mockk<KafkaProducer<String, EsyfovarselHendelse>>(relaxed = true)
    private val esyfovarselProducer = EsyfovarselProducer(mockEsyfoVarselProducer)

    private val mockKandidatProducer = mockk<KafkaProducer<String, KartleggingssporsmalKandidatStatusRecord>>(relaxed = true)
    private val kartleggingssporsmalKandidatProducer = KartleggingssporsmalKandidatProducer(mockKandidatProducer)

    private val kartleggingssporsmalService = KartleggingssporsmalService(
        behandlendeEnhetClient = ExternalMockEnvironment.instance.behandlendeEnhetClient,
        kartleggingssporsmalRepository = kartleggingssporsmalRepository,
        oppfolgingstilfelleClient = ExternalMockEnvironment.instance.oppfolgingstilfelleClient,
        esyfoVarselProducer = esyfovarselProducer,
        kartleggingssporsmalKandidatProducer = kartleggingssporsmalKandidatProducer,
        pdlClient = ExternalMockEnvironment.instance.pdlClient,
        vedtak14aClient = ExternalMockEnvironment.instance.vedtak14aClient,
        isKandidatPublishingEnabled = false,
    )

    private val kartleggingssporsmalServiceWithKandidatPublishingEnabled = KartleggingssporsmalService(
        behandlendeEnhetClient = ExternalMockEnvironment.instance.behandlendeEnhetClient,
        kartleggingssporsmalRepository = kartleggingssporsmalRepository,
        oppfolgingstilfelleClient = ExternalMockEnvironment.instance.oppfolgingstilfelleClient,
        esyfoVarselProducer = esyfovarselProducer,
        kartleggingssporsmalKandidatProducer = kartleggingssporsmalKandidatProducer,
        pdlClient = ExternalMockEnvironment.instance.pdlClient,
        vedtak14aClient = ExternalMockEnvironment.instance.vedtak14aClient,
        isKandidatPublishingEnabled = true,
    )

    @BeforeEach
    fun setUp() {
        database.resetDatabase()
        coEvery { mockKandidatProducer.send(any()) } returns mockk<Future<RecordMetadata>>(relaxed = true)
        coEvery { mockEsyfoVarselProducer.send(any()) } returns mockk<Future<RecordMetadata>>(relaxed = true)
    }

    val stoppunktStartIntervalDays = 6L * DAYS_IN_WEEK
    val stoppunktEndIntervalDays = stoppunktStartIntervalDays + 30L

    @Nested
    @DisplayName("Test processing of oppfolgingstilfelle for generating stoppunkt")
    inner class ProcessOppfolgingstilfelle {

        @Test
        fun `processOppfolgingstilfelle should generate stoppunkt when oppfolgingstilfelle is relevant for stoppunkt`() = runTest {
            val oppfolgingstilfelleInsideStoppunktInterval = createOppfolgingstilfelleFromKafka(
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )

            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleInsideStoppunktInterval)

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(1, stoppunkter.size)
            assertEquals(oppfolgingstilfelleInsideStoppunktInterval.personident, stoppunkter.first().personident)
            assertEquals(oppfolgingstilfelleInsideStoppunktInterval.tilfelleBitReferanseUuid, stoppunkter.first().tilfelleBitReferanseUuid)
            assertEquals(
                oppfolgingstilfelleInsideStoppunktInterval.tilfelleStart.plusDays(stoppunktStartIntervalDays),
                stoppunkter.first().stoppunktAt,
            )
        }

        @Test
        fun `processOppfolgingstilfelle should generate stoppunkt when oppfolgingstilfelle is exactly at stoppunkt start`() = runTest {
            val oppfolgingstilfelleAtStoppunktStart = createOppfolgingstilfelleFromKafka(
                antallSykedager = stoppunktStartIntervalDays.toInt(),
            )

            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleAtStoppunktStart)

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(1, stoppunkter.size)
            assertEquals(oppfolgingstilfelleAtStoppunktStart.personident, stoppunkter.first().personident)
            assertEquals(oppfolgingstilfelleAtStoppunktStart.tilfelleBitReferanseUuid, stoppunkter.first().tilfelleBitReferanseUuid)
            assertEquals(
                oppfolgingstilfelleAtStoppunktStart.tilfelleStart.plusDays(stoppunktStartIntervalDays),
                stoppunkter.first().stoppunktAt,
            )
        }

        @Test
        fun `processOppfolgingstilfelle should generate stoppunkt when oppfolgingstilfelle is exactly at stoppunkt end`() = runTest {
            val oppfolgingstilfelleAtStoppunktEnd = createOppfolgingstilfelleFromKafka(
                antallSykedager = stoppunktEndIntervalDays.toInt(),
            )

            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleAtStoppunktEnd)

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(1, stoppunkter.size)
            assertEquals(oppfolgingstilfelleAtStoppunktEnd.personident, stoppunkter.first().personident)
            assertEquals(oppfolgingstilfelleAtStoppunktEnd.tilfelleBitReferanseUuid, stoppunkter.first().tilfelleBitReferanseUuid)
            assertEquals(
                oppfolgingstilfelleAtStoppunktEnd.tilfelleStart.plusDays(stoppunktStartIntervalDays),
                stoppunkter.first().stoppunktAt,
            )
        }

        @Test
        fun `processOppfolgingstilfelle should generate stoppunkt when tilfelle ending exactly 30 days ago`() = runTest {
            val start = LocalDate.now().minusDays(stoppunktStartIntervalDays + 30)
            val end = LocalDate.now().minusDays(30)
            val oppfolgingstilfelleExactly30DaysAgo = createOppfolgingstilfelleFromKafka(
                tilfelleStart = start,
                tilfelleEnd = end,
                antallSykedager = fullDaysBetween(start, end).toInt(),
            )

            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleExactly30DaysAgo)

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(1, stoppunkter.size)
            assertEquals(oppfolgingstilfelleExactly30DaysAgo.personident, stoppunkter.first().personident)
            assertEquals(oppfolgingstilfelleExactly30DaysAgo.tilfelleBitReferanseUuid, stoppunkter.first().tilfelleBitReferanseUuid)
            assertEquals(
                oppfolgingstilfelleExactly30DaysAgo.tilfelleStart.plusDays(stoppunktStartIntervalDays),
                stoppunkter.first().stoppunktAt,
            )
        }

        @Test
        fun `processOppfolgingstilfelle should generate stoppunkt when antallSykedager is null but tilfelle interval within stoppunkt interval`() = runTest {
            val oppfolgingstilfelleWithNullSykedager = createOppfolgingstilfelleFromKafka(
                antallSykedager = null,
                tilfelleStart = LocalDate.now(),
                tilfelleEnd = LocalDate.now().plusDays(stoppunktStartIntervalDays),
            )

            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleWithNullSykedager)

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(1, stoppunkter.size)
            assertEquals(oppfolgingstilfelleWithNullSykedager.personident, stoppunkter.first().personident)
            assertEquals(oppfolgingstilfelleWithNullSykedager.tilfelleBitReferanseUuid, stoppunkter.first().tilfelleBitReferanseUuid)
            assertEquals(
                oppfolgingstilfelleWithNullSykedager.tilfelleStart.plusDays(stoppunktStartIntervalDays),
                stoppunkter.first().stoppunktAt,
            )
        }

        @Test
        fun `processOppfolgingstilfelle should generate stoppunkt when duration until today is before stoppunkt but new periode sends duration outside interval-end`() = runTest {
            val oppfolgingstilfelleWithFutureSykedagerInsideInterval = createOppfolgingstilfelleFromKafka(
                antallSykedager = (stoppunktEndIntervalDays + 1).toInt(),
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays - 2),
            )

            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleWithFutureSykedagerInsideInterval)

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(1, stoppunkter.size)
            assertEquals(oppfolgingstilfelleWithFutureSykedagerInsideInterval.personident, stoppunkter.first().personident)
            assertEquals(
                oppfolgingstilfelleWithFutureSykedagerInsideInterval.tilfelleBitReferanseUuid,
                stoppunkter.first().tilfelleBitReferanseUuid
            )
            assertEquals(
                oppfolgingstilfelleWithFutureSykedagerInsideInterval.tilfelleStart.plusDays(stoppunktStartIntervalDays),
                stoppunkter.first().stoppunktAt,
            )
        }

        @Test
        fun `processOppfolgingstilfelle should generate stoppunkt today when duration until today is inside interval but new periode sends duration outside interval-end`() = runTest {
            val oppfolgingstilfelleWithDurationUntilNowInsideInterval = createOppfolgingstilfelleFromKafka(
                antallSykedager = (stoppunktEndIntervalDays + 10).toInt(),
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays + 10),
            )

            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleWithDurationUntilNowInsideInterval)

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(1, stoppunkter.size)
            assertEquals(oppfolgingstilfelleWithDurationUntilNowInsideInterval.personident, stoppunkter.first().personident)
            assertEquals(
                oppfolgingstilfelleWithDurationUntilNowInsideInterval.tilfelleBitReferanseUuid,
                stoppunkter.first().tilfelleBitReferanseUuid
            )
            assertEquals(LocalDate.now(), stoppunkter.first().stoppunktAt)
        }

        @Test
        fun `processOppfolgingstilfelle should ignore when too short tilfelle is consumed when 'today' is inside interval`() = runTest {
            val oppfolgingstilfelleWithDurationUntilNowInsideInterval = createOppfolgingstilfelleFromKafka(
                antallSykedager = (stoppunktStartIntervalDays - 10).toInt(),
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays + 10),
            )

            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleWithDurationUntilNowInsideInterval)

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(0, stoppunkter.size)
        }

        @Test
        fun `processOppfolgingstilfelle should ignore when oppfolgingstilfelle is before stoppunkt interval`() = runTest {
            val oppfolgingstilfelleBeforeStoppunktInterval = createOppfolgingstilfelleFromKafka(
                antallSykedager = stoppunktStartIntervalDays.toInt() - 1,
            )

            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleBeforeStoppunktInterval)

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(0, stoppunkter.size)
        }

        @Test
        fun `processOppfolgingstilfelle should ignore when oppfolgingstilfelle is after stoppunkt interval`() = runTest {
            val oppfolgingstilfelleOutsideStoppunktInterval = createOppfolgingstilfelleFromKafka(
                tilfelleStart = LocalDate.now().minusDays(stoppunktEndIntervalDays + 1),
                antallSykedager = stoppunktEndIntervalDays.toInt() + 1,
            )

            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleOutsideStoppunktInterval)

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(0, stoppunkter.size)
        }

        @Test
        fun `processOppfolgingstilfelle should ignore when person is dead`() = runTest {
            val oppfolgingstilfelleDod = createOppfolgingstilfelleFromKafka(
                antallSykedager = stoppunktStartIntervalDays.toInt(),
                dodsdato = LocalDate.now().minusDays(1),
            )

            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleDod)

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(0, stoppunkter.size)
        }

        @Test
        fun `processOppfolgingstilfelle should ignore when not in pilot enhet`() = runTest {
            val oppfolgingstilfelleNotPilot = createOppfolgingstilfelleFromKafka(
                personident = ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET,
                antallSykedager = stoppunktStartIntervalDays.toInt(),
            )

            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleNotPilot)

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(0, stoppunkter.size)
        }

        @Test
        fun `processOppfolgingstilfelle should return false when cannot find behandlende enhet`() = runTest {
            val oppfolgingstilfelleNotPilot = createOppfolgingstilfelleFromKafka(
                personident = ARBEIDSTAKER_PERSONIDENT_INACTIVE,
                antallSykedager = stoppunktStartIntervalDays.toInt(),
            )

            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleNotPilot)

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(0, stoppunkter.size)
        }

        @Test
        fun `processOppfolgingstilfelle should ignore when multiple negative conditions - dead and not in pilot`() = runTest {
            val oppfolgingstilfelleDodNotPilot = createOppfolgingstilfelleFromKafka(
                personident = ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET,
                antallSykedager = stoppunktStartIntervalDays.toInt(),
                dodsdato = LocalDate.now().minusDays(1),
            )

            kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleDodNotPilot)

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(0, stoppunkter.size)
        }
    }

    @Nested
    @DisplayName("Test processing of stoppunkter to generate kandidater")
    inner class ProcessStoppunkter {

        @Test
        fun `processStoppunkter should process unprocessed stoppunkt and create KANDIDAT but not publish when not enabled`() = runTest {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                personident = ARBEIDSTAKER_PERSONIDENT,
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(stoppunkt)

            kartleggingssporsmalRepository.createStoppunkt(stoppunkt)

            val results = kartleggingssporsmalService.processStoppunkter()

            assertEquals(1, results.size)
            assertTrue(results.first().isSuccess)

            val stoppunktProcesed = results.first().getOrThrow()
            val kandidat = database.getKandidatByStoppunktUUID(stoppunktProcesed.uuid)!!
            val kandidatStatusList = kartleggingssporsmalRepository.getKandidatStatusendringer(kandidat.uuid)
            assertEquals(1, kandidatStatusList.size)
            val kandidatStatus = kandidatStatusList.first()

            assertEquals(oppfolgingstilfelle.personident, kandidat.personident)
            assertEquals(KandidatStatus.KANDIDAT.name, kandidat.status)
            assertNull(kandidat.varsletAt)
            assertNull(kandidatStatus.publishedAt)
            verify(exactly = 0) { mockKandidatProducer.send(any()) }
            verify(exactly = 0) { mockEsyfoVarselProducer.send(any()) }

            val dbStoppunkt = database.getKartleggingssporsmalStoppunkt().first()
            assertEquals(kandidat.personident, dbStoppunkt.personident)
            assertNotNull(dbStoppunkt.processedAt)
        }

        @Test
        fun `processStoppunkter should process unprocessed stoppunkt and create KANDIDAT and publish when enabled`() = runTest {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                personident = ARBEIDSTAKER_PERSONIDENT,
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(stoppunkt)

            kartleggingssporsmalRepository.createStoppunkt(stoppunkt)

            val results = kartleggingssporsmalServiceWithKandidatPublishingEnabled.processStoppunkter()

            assertEquals(1, results.size)
            assertTrue(results.first().isSuccess)

            val stoppunktProcesed = results.first().getOrThrow()
            val kandidat = database.getKandidatByStoppunktUUID(stoppunktProcesed.uuid)!!
            val kandidatStatusList = kartleggingssporsmalRepository.getKandidatStatusendringer(kandidat.uuid)
            val kandidatStatus = kandidatStatusList.first()

            assertEquals(oppfolgingstilfelle.personident, kandidat.personident)
            assertEquals(KandidatStatus.KANDIDAT.name, kandidat.status)
            assertNotNull(kandidat.varsletAt)
            assertNotNull(kandidatStatus.publishedAt)

            val producerRecordSlot = slot<ProducerRecord<String, KartleggingssporsmalKandidatStatusRecord>>()
            verify(exactly = 1) { mockKandidatProducer.send(capture(producerRecordSlot)) }
            val record = producerRecordSlot.captured.value()
            assertEquals(kandidat.uuid, record.kandidatUuid)
            assertEquals(kandidat.personident.value, record.personident)
            assertEquals(KandidatStatus.KANDIDAT.name, record.status)

            val producerEsyfoRecordSlot = slot<ProducerRecord<String, EsyfovarselHendelse>>()
            verify(exactly = 1) { mockEsyfoVarselProducer.send(capture(producerEsyfoRecordSlot)) }
            val recordEsyfoVarsel = producerEsyfoRecordSlot.captured.value()
            assertEquals(kandidat.personident.value, (recordEsyfoVarsel as ArbeidstakerHendelse).arbeidstakerFnr)
            assertEquals(HendelseType.SM_KARTLEGGINGSSPORSMAL, recordEsyfoVarsel.type)

            val dbStoppunkt = database.getKartleggingssporsmalStoppunkt().first()
            assertEquals(kandidat.personident, dbStoppunkt.personident)
            assertNotNull(dbStoppunkt.processedAt)
        }

        @Test
        fun `processStoppunkter should create kandidat when long tilfelle even if few days left`() = runTest {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                personident = ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT_DURATION_LEFT_BUT_LONG,
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(stoppunkt)

            kartleggingssporsmalRepository.createStoppunkt(stoppunkt)

            val results = kartleggingssporsmalService.processStoppunkter()

            assertEquals(1, results.size)
            assertTrue(results.first().isSuccess)

            val stoppunktProcessed = results.first().getOrThrow()
            val kandidat = database.getKandidatByStoppunktUUID(stoppunktProcessed.uuid)
            assertNotNull(kandidat)

            val dbStoppunkt = database.getKartleggingssporsmalStoppunkt().first()
            assertNotNull(dbStoppunkt.processedAt)
        }

        @ParameterizedTest(name = "processStoppunkter should process unprocessed stoppunkt and not create kandidat for personident={0}, reason={1}")
        @MethodSource("no.nav.syfo.kartleggingssporsmal.application.KartleggingssporsmalServiceTest#provideIkkeKandidatScenarios")
        fun `processStoppunkter should not create kandidat for various conditions`(
            personident: String,
            reason: String,
        ) = runTest {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                personident = Personident(personident),
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(stoppunkt)

            kartleggingssporsmalRepository.createStoppunkt(stoppunkt)

            val results = kartleggingssporsmalService.processStoppunkter()

            assertEquals(1, results.size)
            assertTrue(results.first().isSuccess)

            val stoppunktProcessed = results.first().getOrThrow()
            val kandidat = database.getKandidatByStoppunktUUID(stoppunktProcessed.uuid)
            assertNull(kandidat, "Expected no kandidat because $reason")

            val dbStoppunkt = database.getKartleggingssporsmalStoppunkt().first()
            assertNotNull(dbStoppunkt.processedAt)
        }

        @Test
        fun `processStoppunkter should process unprocessed stoppunkt and not create kandidat when already KANDIDAT in current tilfelle`() = runTest {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                personident = ARBEIDSTAKER_PERSONIDENT,
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val firstStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(firstStoppunkt)

            // Genererer først et stoppunkt som fører til en kandidat
            kartleggingssporsmalRepository.createStoppunkt(firstStoppunkt)
            val firstResults = kartleggingssporsmalService.processStoppunkter()
            assertTrue(firstResults.first().isSuccess)
            val firstProcessedStoppunkt = firstResults.first().getOrThrow()
            val firstKandidat = database.getKandidatByStoppunktUUID(firstProcessedStoppunkt.uuid)!!
            assertEquals(KandidatStatus.KANDIDAT.name, firstKandidat.status)

            // Nytt stoppunkt, skal ikke føre til kandidat
            val secondStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(secondStoppunkt)
            kartleggingssporsmalRepository.createStoppunkt(secondStoppunkt)
            val secondResults = kartleggingssporsmalService.processStoppunkter()

            assertEquals(1, secondResults.size)
            assertTrue(secondResults.first().isSuccess)

            val secondProcessedStoppunkt = secondResults.first().getOrThrow()
            val secondKandidat = database.getKandidatByStoppunktUUID(secondProcessedStoppunkt.uuid)
            assertNull(secondKandidat)
        }

        @Test
        fun `processStoppunkter should not process when stoppunkter already processed`() = runTest {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                personident = ARBEIDSTAKER_PERSONIDENT,
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(stoppunkt)

            kartleggingssporsmalRepository.createStoppunkt(stoppunkt)
            database.markStoppunktAsProcessed(stoppunkt)

            val results = kartleggingssporsmalService.processStoppunkter()

            assertEquals(0, results.size)
        }

        @Test
        fun `processStoppunkter should not process when error from other systems`() = runTest {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                personident = ARBEIDSTAKER_PERSONIDENT_ERROR,
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(stoppunkt)

            kartleggingssporsmalRepository.createStoppunkt(stoppunkt)

            val results = kartleggingssporsmalService.processStoppunkter()

            assertEquals(1, results.size)
            assertTrue(results.first().isFailure)

            val processedStoppunkt = database.getKartleggingssporsmalStoppunkt().first()
            assertEquals(stoppunkt.personident, processedStoppunkt.personident)
            assertNull(processedStoppunkt.processedAt)
        }

        @Test
        fun `processStoppunkter should not process when no stoppunkt`() = runTest {
            val results = kartleggingssporsmalService.processStoppunkter()

            assertEquals(0, results.size)
        }
    }

    @Nested
    @DisplayName("Test registrering of svar")
    inner class RegistrerSvar {

        @Test
        fun `registrerSvar should store svar, publish on kafka and ferdigstille varsel when svar on existing kandidat`() = runTest {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(stoppunkt)

            kartleggingssporsmalRepository.createStoppunkt(stoppunkt)
            val createdStoppunkt = database.getKartleggingssporsmalStoppunkt().first()

            val kandidat = KartleggingssporsmalKandidat.create(personident = ARBEIDSTAKER_PERSONIDENT)
                .copy(varsletAt = OffsetDateTime.now())
            val createdKandidat = kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                kandidat = kandidat,
                stoppunktId = createdStoppunkt.id,
            )
            assertTrue(kandidat.status !is KartleggingssporsmalKandidatStatusendring.SvarMottatt)

            val svarAt = OffsetDateTime.now().minusDays(1)
            kartleggingssporsmalService.registrerSvar(
                kandidatUuid = createdKandidat.uuid,
                svarAt = svarAt,
                svarId = UUID.randomUUID(),
            )

            val fetchedKandidat = kartleggingssporsmalRepository.getKandidat(createdKandidat.uuid)
            assertTrue(fetchedKandidat?.status is KartleggingssporsmalKandidatStatusendring.SvarMottatt)
            assertEquals(
                (fetchedKandidat?.status as KartleggingssporsmalKandidatStatusendring.SvarMottatt).svarAt.toLocalDateOslo(),
                svarAt.toLocalDateOslo()
            )

            val producerRecordSlotKandidat = slot<ProducerRecord<String, KartleggingssporsmalKandidatStatusRecord>>()
            val producerRecordSlotVarsel = slot<ProducerRecord<String, EsyfovarselHendelse>>()
            verify(exactly = 1) {
                mockKandidatProducer.send(capture(producerRecordSlotKandidat))
                mockEsyfoVarselProducer.send(capture(producerRecordSlotVarsel))
            }

            val kandidatHendelse = producerRecordSlotKandidat.captured.value()
            assertEquals(kandidatHendelse.status, KandidatStatus.SVAR_MOTTATT.name)

            val esyfovarselHendelse = producerRecordSlotVarsel.captured.value()
            assertEquals(esyfovarselHendelse.type, HendelseType.SM_KARTLEGGINGSSPORSMAL)
            assertTrue(esyfovarselHendelse.ferdigstill!!)
        }

        @Test
        fun `registrerSvar should store svar, publish on kafka and ferdigstille varsel twice when two svar on existing kandidat`() = runTest {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(stoppunkt)

            kartleggingssporsmalRepository.createStoppunkt(stoppunkt)
            val createdStoppunkt = database.getKartleggingssporsmalStoppunkt().first()

            val kandidat = KartleggingssporsmalKandidat.create(personident = ARBEIDSTAKER_PERSONIDENT)
                .copy(varsletAt = OffsetDateTime.now())
            val createdKandidat = kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                kandidat = kandidat,
                stoppunktId = createdStoppunkt.id,
            )
            assertTrue(createdKandidat.status is KartleggingssporsmalKandidatStatusendring.Kandidat)

            kartleggingssporsmalService.registrerSvar(
                kandidatUuid = createdKandidat.uuid,
                svarAt = OffsetDateTime.now().minusDays(1),
                svarId = UUID.randomUUID(),
            )
            val secondSvarAt = OffsetDateTime.now()
            kartleggingssporsmalService.registrerSvar(
                kandidatUuid = createdKandidat.uuid,
                svarAt = secondSvarAt,
                svarId = UUID.randomUUID(),
            )

            val fetchedKandidat = kartleggingssporsmalRepository.getKandidat(createdKandidat.uuid)
            val statusendringer = kartleggingssporsmalRepository.getKandidatStatusendringer(createdKandidat.uuid)
            assertEquals(2, statusendringer.filter { it.kandidatStatus == KandidatStatus.SVAR_MOTTATT }.size)

            assertTrue(fetchedKandidat?.status is KartleggingssporsmalKandidatStatusendring.SvarMottatt)
            assertEquals(
                secondSvarAt.toLocalDateOslo(),
                (fetchedKandidat?.status as KartleggingssporsmalKandidatStatusendring.SvarMottatt).svarAt.toLocalDateOslo()
            )

            verify(exactly = 2) {
                mockKandidatProducer.send(any())
                mockEsyfoVarselProducer.send(any())
            }
        }

        @Test
        fun `registrerSvar should not store svar when no existing kandidat`() = runTest {
            val kandidatUuid = UUID.randomUUID()
            kartleggingssporsmalService.registrerSvar(
                kandidatUuid = kandidatUuid,
                svarAt = OffsetDateTime.now(),
                svarId = UUID.randomUUID(),
            )

            val fetchedKandidat = kartleggingssporsmalRepository.getKandidat(kandidatUuid)
            assertNull(fetchedKandidat)

            verify(exactly = 0) {
                mockKandidatProducer.send(any())
                mockEsyfoVarselProducer.send(any())
            }
        }
    }

    @Nested
    @DisplayName("Test registrering of ferdig behandlet")
    inner class RegistrerFerdigBehandlet {

        @Test
        fun `registrer ferdig behandlet should store status and veilederident`() = runTest {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)!!
            kartleggingssporsmalRepository.createStoppunkt(stoppunkt)
            val createdStoppunkt = database.getKartleggingssporsmalStoppunkt().first()

            val kandidat = KartleggingssporsmalKandidat.create(personident = ARBEIDSTAKER_PERSONIDENT)
                .copy(varsletAt = OffsetDateTime.now())
            val createdKandidat = kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                kandidat = kandidat,
                stoppunktId = createdStoppunkt.id,
            )

            kartleggingssporsmalService.registrerSvar(
                kandidatUuid = createdKandidat.uuid,
                svarAt = OffsetDateTime.now(),
                svarId = UUID.randomUUID(),
            )
            val ferdigbehandletBy = UserConstants.VEILEDER_IDENT
            val returnedKandidat = kartleggingssporsmalService.registrerFerdigbehandlet(
                uuid = createdKandidat.uuid,
                veilederident = ferdigbehandletBy,
            )
            assertEquals(createdKandidat.uuid, returnedKandidat.uuid)
            assertEquals(ARBEIDSTAKER_PERSONIDENT, returnedKandidat.personident)
            assertTrue(returnedKandidat.status is KartleggingssporsmalKandidatStatusendring.Ferdigbehandlet)
            assertEquals(
                ferdigbehandletBy,
                (returnedKandidat.status as KartleggingssporsmalKandidatStatusendring.Ferdigbehandlet).veilederident
            )

            val fetchedKandidat = kartleggingssporsmalRepository.getKandidat(createdKandidat.uuid)
            assertTrue(fetchedKandidat?.status is KartleggingssporsmalKandidatStatusendring.Ferdigbehandlet)
            assertEquals(
                UserConstants.VEILEDER_IDENT,
                (fetchedKandidat?.status as KartleggingssporsmalKandidatStatusendring.Ferdigbehandlet).veilederident
            )
            assertNotNull(fetchedKandidat.status.publishedAt)
            val producerRecordSlot = mutableListOf<ProducerRecord<String, KartleggingssporsmalKandidatStatusRecord>>()
            verify(exactly = 2) { mockKandidatProducer.send(capture(producerRecordSlot)) }
            val lastRecord = producerRecordSlot.last().value()
            assertEquals(createdKandidat.uuid, lastRecord.kandidatUuid)
            assertEquals(ARBEIDSTAKER_PERSONIDENT.value, lastRecord.personident)
            assertEquals(KandidatStatus.FERDIGBEHANDLET.name, lastRecord.status)
        }

        @Test
        fun `registrer ferdig behandlet should not store status if not svar mottatt`() = runTest {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)!!
            kartleggingssporsmalRepository.createStoppunkt(stoppunkt)
            val createdStoppunkt = database.getKartleggingssporsmalStoppunkt().first()

            val kandidat = KartleggingssporsmalKandidat.create(personident = ARBEIDSTAKER_PERSONIDENT)
            kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                kandidat = kandidat,
                stoppunktId = createdStoppunkt.id,
            )
            assertThrows<IllegalArgumentException> {
                kartleggingssporsmalService.registrerFerdigbehandlet(
                    uuid = kandidat.uuid,
                    veilederident = UserConstants.VEILEDER_IDENT,
                )
            }
        }

        @Test
        fun `registrer ferdig behandlet should not store status if no kandidat`() = runTest {
            assertThrows<IllegalArgumentException> {
                kartleggingssporsmalService.registrerFerdigbehandlet(
                    uuid = UUID.randomUUID(),
                    veilederident = UserConstants.VEILEDER_IDENT,
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun provideIkkeKandidatScenarios() = listOf(
            Arguments.of(ARBEIDSTAKER_PERSONIDENT_HAS_14A.value, "has 14a vedtak"),
            Arguments.of(ARBEIDSTAKER_PERSONIDENT_TOO_OLD.value, "too old"),
            Arguments.of(ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT.value, "tilfelle is not longer than stoppunkt anymore"),
            Arguments.of(ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT_DURATION_LEFT.value, "tilfelle ends in a few days"),
            Arguments.of(ARBEIDSTAKER_PERSONIDENT_TILFELLE_DOD.value, "person is dod"),
            Arguments.of(ARBEIDSTAKER_PERSONIDENT_NO_ARBEIDSGIVER.value, "person is not arbeidstaker"),
            Arguments.of(ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET.value, "person not in pilot"),
        )
    }
}
