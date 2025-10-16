package no.nav.syfo.kartleggingssporsmal.application

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_ERROR
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_HAS_14A
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_INACTIVE
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_NO_ARBEIDSGIVER
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_TILFELLE_DOD
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_TOO_OLD
import no.nav.syfo.kartleggingssporsmal.domain.KandidatStatus
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfelleFromKafka
import no.nav.syfo.kartleggingssporsmal.infrastructure.database.KartleggingssporsmalRepository
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.ArbeidstakerHendelse
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.EsyfovarselHendelse
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.EsyfovarselHendelse.HendelseType
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.EsyfovarselProducer
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.KartleggingssporsmalKandidatProducer
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.KartleggingssporsmalKandidatRecord
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.infrastructure.database.getKandidatByStoppunktUUID
import no.nav.syfo.shared.infrastructure.database.getKartleggingssporsmalStoppunkt
import no.nav.syfo.shared.infrastructure.database.markStoppunktAsProcessed
import no.nav.syfo.shared.util.DAYS_IN_WEEK
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
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.Future
import kotlin.test.assertNull

class KartleggingssporsmalServiceTest {
    private val database = ExternalMockEnvironment.instance.database
    private val kartleggingssporsmalRepository = KartleggingssporsmalRepository(database)

    private val mockEsyfoVarselProducer = mockk<KafkaProducer<String, EsyfovarselHendelse>>(relaxed = true)
    private val esyfovarselProducer = EsyfovarselProducer(mockEsyfoVarselProducer)

    private val mockKandidatProducer = mockk<KafkaProducer<String, KartleggingssporsmalKandidatRecord>>(relaxed = true)
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
        fun `processOppfolgingstilfelle should generate stoppunkt when oppfolgingstilfelle is relevant for stoppunkt`() {
            val oppfolgingstilfelleInsideStoppunktInterval = createOppfolgingstilfelleFromKafka(
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )

            runBlocking {
                kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleInsideStoppunktInterval)
            }

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
        fun `processOppfolgingstilfelle should generate stoppunkt when oppfolgingstilfelle is exactly at stoppunkt start`() {
            val oppfolgingstilfelleAtStoppunktStart = createOppfolgingstilfelleFromKafka(
                antallSykedager = stoppunktStartIntervalDays.toInt(),
            )

            runBlocking {
                kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleAtStoppunktStart)
            }

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
        fun `processOppfolgingstilfelle should generate stoppunkt when oppfolgingstilfelle is exactly at stoppunkt end`() {
            val oppfolgingstilfelleAtStoppunktEnd = createOppfolgingstilfelleFromKafka(
                antallSykedager = stoppunktEndIntervalDays.toInt(),
            )

            runBlocking {
                kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleAtStoppunktEnd)
            }

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
        fun `processOppfolgingstilfelle should generate stoppunkt when tilfelle ending exactly 30 days ago`() {
            val start = LocalDate.now().minusDays(stoppunktStartIntervalDays + 30)
            val end = LocalDate.now().minusDays(30)
            val oppfolgingstilfelleExactly30DaysAgo = createOppfolgingstilfelleFromKafka(
                tilfelleStart = start,
                tilfelleEnd = end,
                antallSykedager = ChronoUnit.DAYS.between(start, end).toInt() + 1,
            )

            runBlocking {
                kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleExactly30DaysAgo)
            }

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
        fun `processOppfolgingstilfelle should generate stoppunkt when antallSykedager is null but tilfelle interval within stoppunkt interval`() {
            val oppfolgingstilfelleWithNullSykedager = createOppfolgingstilfelleFromKafka(
                antallSykedager = null,
                tilfelleStart = LocalDate.now(),
                tilfelleEnd = LocalDate.now().plusDays(stoppunktStartIntervalDays),
            )

            runBlocking {
                kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleWithNullSykedager)
            }

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
        fun `processOppfolgingstilfelle should generate stoppunkt when duration until today is before stoppunkt but new periode sends duration outside interval-end`() {
            val oppfolgingstilfelleWithFutureSykedagerInsideInterval = createOppfolgingstilfelleFromKafka(
                antallSykedager = (stoppunktEndIntervalDays + 1).toInt(),
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays - 2),
            )

            runBlocking {
                kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleWithFutureSykedagerInsideInterval)
            }

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
        fun `processOppfolgingstilfelle should generate stoppunkt today when duration until today is inside interval but new periode sends duration outside interval-end`() {
            val oppfolgingstilfelleWithDurationUntilNowInsideInterval = createOppfolgingstilfelleFromKafka(
                antallSykedager = (stoppunktEndIntervalDays + 10).toInt(),
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays + 10),
            )

            runBlocking {
                kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleWithDurationUntilNowInsideInterval)
            }

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
        fun `processOppfolgingstilfelle should ignore when too short tilfelle is consumed when 'today' is inside interval`() {
            val oppfolgingstilfelleWithDurationUntilNowInsideInterval = createOppfolgingstilfelleFromKafka(
                antallSykedager = (stoppunktStartIntervalDays - 10).toInt(),
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays + 10),
            )

            runBlocking {
                kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleWithDurationUntilNowInsideInterval)
            }

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(0, stoppunkter.size)
        }

        @Test
        fun `processOppfolgingstilfelle should ignore when oppfolgingstilfelle is before stoppunkt interval`() {
            val oppfolgingstilfelleBeforeStoppunktInterval = createOppfolgingstilfelleFromKafka(
                antallSykedager = stoppunktStartIntervalDays.toInt() - 1,
            )

            runBlocking {
                kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleBeforeStoppunktInterval)
            }

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(0, stoppunkter.size)
        }

        @Test
        fun `processOppfolgingstilfelle should ignore when oppfolgingstilfelle is after stoppunkt interval`() {
            val oppfolgingstilfelleOutsideStoppunktInterval = createOppfolgingstilfelleFromKafka(
                tilfelleStart = LocalDate.now().minusDays(stoppunktEndIntervalDays + 1),
                antallSykedager = stoppunktEndIntervalDays.toInt() + 1,
            )

            runBlocking {
                kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleOutsideStoppunktInterval)
            }

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(0, stoppunkter.size)
        }

        @Test
        fun `processOppfolgingstilfelle should ignore when person is dead`() {
            val oppfolgingstilfelleDod = createOppfolgingstilfelleFromKafka(
                antallSykedager = stoppunktStartIntervalDays.toInt(),
                dodsdato = LocalDate.now().minusDays(1),
            )

            runBlocking {
                kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleDod)
            }

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(0, stoppunkter.size)
        }

        @Test
        fun `processOppfolgingstilfelle should ignore when not in pilot enhet`() {
            val oppfolgingstilfelleNotPilot = createOppfolgingstilfelleFromKafka(
                personident = ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET,
                antallSykedager = stoppunktStartIntervalDays.toInt(),
            )

            runBlocking {
                kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleNotPilot)
            }

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(0, stoppunkter.size)
        }

        @Test
        fun `processOppfolgingstilfelle should return false when cannot find behandlende enhet`() {
            val oppfolgingstilfelleNotPilot = createOppfolgingstilfelleFromKafka(
                personident = ARBEIDSTAKER_PERSONIDENT_INACTIVE,
                antallSykedager = stoppunktStartIntervalDays.toInt(),
            )

            runBlocking {
                kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleNotPilot)
            }

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(0, stoppunkter.size)
        }

        @Test
        fun `processOppfolgingstilfelle should ignore when multiple negative conditions - dead and not in pilot`() {
            val oppfolgingstilfelleDodNotPilot = createOppfolgingstilfelleFromKafka(
                personident = ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET,
                antallSykedager = stoppunktStartIntervalDays.toInt(),
                dodsdato = LocalDate.now().minusDays(1),
            )

            runBlocking {
                kartleggingssporsmalService.processOppfolgingstilfelle(oppfolgingstilfelleDodNotPilot)
            }

            val stoppunkter = database.getKartleggingssporsmalStoppunkt()
            assertEquals(0, stoppunkter.size)
        }
    }

    @Nested
    @DisplayName("Test processing of stoppunkter to generate kandidater")
    inner class ProcessStoppunkter {

        @Test
        fun `processStoppunkter should process unprocessed stoppunkt and create KANDIDAT but not publish when not enabled`() {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                personident = ARBEIDSTAKER_PERSONIDENT,
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(stoppunkt)

            runBlocking {
                kartleggingssporsmalRepository.createStoppunkt(stoppunkt)

                val results = kartleggingssporsmalService.processStoppunkter()

                assertEquals(1, results.size)
                assertTrue(results.first().isSuccess)

                val stoppunkt = results.first().getOrThrow()
                val kandidat = database.getKandidatByStoppunktUUID(stoppunkt.uuid)!!

                assertEquals(oppfolgingstilfelle.personident, kandidat.personident)
                assertEquals(KandidatStatus.KANDIDAT.name, kandidat.status)
                assertNull(kandidat.varsletAt)
                assertNull(kandidat.publishedAt)
                verify(exactly = 0) { mockKandidatProducer.send(any()) }
                verify(exactly = 0) { mockEsyfoVarselProducer.send(any()) }

                val processedStoppunkt = database.getKartleggingssporsmalStoppunkt().first()
                assertEquals(kandidat.personident, processedStoppunkt.personident)
                assertNotNull(processedStoppunkt.processedAt)
            }
        }

        @Test
        fun `processStoppunkter should process unprocessed stoppunkt and create KANDIDAT and publish when enabled`() {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                personident = ARBEIDSTAKER_PERSONIDENT,
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(stoppunkt)

            runBlocking {
                kartleggingssporsmalRepository.createStoppunkt(stoppunkt)

                val results = kartleggingssporsmalServiceWithKandidatPublishingEnabled.processStoppunkter()

                assertEquals(1, results.size)
                assertTrue(results.first().isSuccess)

                val stoppunkt = results.first().getOrThrow()
                val kandidat = database.getKandidatByStoppunktUUID(stoppunkt.uuid)!!

                assertEquals(oppfolgingstilfelle.personident, kandidat.personident)
                assertEquals(KandidatStatus.KANDIDAT.name, kandidat.status)
                assertNotNull(kandidat.varsletAt)
                assertNotNull(kandidat.publishedAt)

                val producerRecordSlot = slot<ProducerRecord<String, KartleggingssporsmalKandidatRecord>>()
                verify(exactly = 1) { mockKandidatProducer.send(capture(producerRecordSlot)) }
                val record = producerRecordSlot.captured.value()
                assertEquals(kandidat.uuid, record.uuid)
                assertEquals(kandidat.personident.value, record.personident)
                assertEquals(KandidatStatus.KANDIDAT.name, record.status)

                val producerEsyfoRecordSlot = slot<ProducerRecord<String, EsyfovarselHendelse>>()
                verify(exactly = 1) { mockEsyfoVarselProducer.send(capture(producerEsyfoRecordSlot)) }
                val recordEsyfoVarsel = producerEsyfoRecordSlot.captured.value()
                assertEquals(kandidat.personident.value, (recordEsyfoVarsel as ArbeidstakerHendelse).arbeidstakerFnr)
                assertEquals(HendelseType.SM_KARTLEGGINGSSPORSMAL, recordEsyfoVarsel.type)

                val processedStoppunkt = database.getKartleggingssporsmalStoppunkt().first()
                assertEquals(kandidat.personident, processedStoppunkt.personident)
                assertNotNull(processedStoppunkt.processedAt)
            }
        }

        @ParameterizedTest(name = "processStoppunkter should process unprocessed stoppunkt and not create kandidat for personident={0}, reason={1}")
        @MethodSource("no.nav.syfo.kartleggingssporsmal.application.KartleggingssporsmalServiceTest#provideIkkeKandidatScenarios")
        fun `processStoppunkter should not create kandidat for various conditions`(
            personident: String,
            reason: String,
        ) {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                personident = Personident(personident),
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(stoppunkt)

            runBlocking {
                kartleggingssporsmalRepository.createStoppunkt(stoppunkt)

                val results = kartleggingssporsmalService.processStoppunkter()

                assertEquals(1, results.size)
                assertTrue(results.first().isSuccess)

                val stoppunkt = results.first().getOrThrow()
                val kandidat = database.getKandidatByStoppunktUUID(stoppunkt.uuid)
                assertNull(kandidat)

                val processedStoppunkt = database.getKartleggingssporsmalStoppunkt().first()
                assertNotNull(processedStoppunkt.processedAt)
            }
        }

        @Test
        fun `processStoppunkter should process unprocessed stoppunkt and not create kandidat when already KANDIDAT in current tilfelle`() {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                personident = ARBEIDSTAKER_PERSONIDENT,
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val firstStoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(firstStoppunkt)

            runBlocking {
                // Genererer først et stoppunkt som fører til en kandidat
                kartleggingssporsmalRepository.createStoppunkt(firstStoppunkt)
                val firstResults = kartleggingssporsmalService.processStoppunkter()
                assertTrue(firstResults.first().isSuccess)
                val firstProcessedStoppunkt = firstResults.first().getOrThrow()
                val firstKandidat = database.getKandidatByStoppunktUUID(firstProcessedStoppunkt.uuid)!!
                assertEquals(KandidatStatus.KANDIDAT.name, firstKandidat.status)

                // Genererer et nytt stoppunkt, som ikke skal føre til en kandidat fordi det allerede finnes en KANDIDAT i samme tilfelle
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
        }

        @Test
        fun `processStoppunkter should not process when stoppunkter already processed`() {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                personident = ARBEIDSTAKER_PERSONIDENT,
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(stoppunkt)

            runBlocking {
                kartleggingssporsmalRepository.createStoppunkt(stoppunkt)
                database.markStoppunktAsProcessed(stoppunkt)

                val results = kartleggingssporsmalService.processStoppunkter()

                assertEquals(0, results.size)
            }
        }

        @Test
        fun `processStoppunkter should not process when error from other systems`() {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                personident = ARBEIDSTAKER_PERSONIDENT_ERROR,
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(stoppunkt)

            runBlocking {
                kartleggingssporsmalRepository.createStoppunkt(stoppunkt)

                val results = kartleggingssporsmalService.processStoppunkter()

                assertEquals(1, results.size)
                assertTrue(results.first().isFailure)

                val processedStoppunkt = database.getKartleggingssporsmalStoppunkt().first()
                assertEquals(stoppunkt.personident, processedStoppunkt.personident)
                assertNull(processedStoppunkt.processedAt)
            }
        }

        @Test
        fun `processStoppunkter should not process when no stoppunkt`() {
            runBlocking {
                val results = kartleggingssporsmalService.processStoppunkter()

                assertEquals(0, results.size)
            }
        }
    }

    @Nested
    @DisplayName("Test registrering of svar")
    inner class RegistrerSvar {

        @Test
        fun `registrerSvar should store svar when svar on existing kandidat`() {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(stoppunkt)

            runBlocking {
                kartleggingssporsmalRepository.createStoppunkt(stoppunkt)
                val createdStoppunkt = database.getKartleggingssporsmalStoppunkt().first()

                val kandidat = KartleggingssporsmalKandidat(
                    personident = ARBEIDSTAKER_PERSONIDENT,
                    status = KandidatStatus.KANDIDAT,
                )
                val createdKandidat = kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                    kandidat = kandidat,
                    stoppunktId = createdStoppunkt.id,
                )
                assertNull(createdKandidat.svarAt)

                kartleggingssporsmalService.registrerSvar(
                    kandidatUuid = createdKandidat.uuid,
                    svarAt = OffsetDateTime.now(),
                    svarId = UUID.randomUUID(),
                )

                val fetchedKandidat = kartleggingssporsmalRepository.getKandidat(createdKandidat.uuid)
                assertNotNull(fetchedKandidat?.svarAt)
            }
        }

        @Test
        fun `registrerSvar should store svar again when two svar on existing kandidat`() {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(stoppunkt)

            runBlocking {
                kartleggingssporsmalRepository.createStoppunkt(stoppunkt)
                val createdStoppunkt = database.getKartleggingssporsmalStoppunkt().first()

                val kandidat = KartleggingssporsmalKandidat(
                    personident = ARBEIDSTAKER_PERSONIDENT,
                    status = KandidatStatus.KANDIDAT,
                )
                val createdKandidat = kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                    kandidat = kandidat,
                    stoppunktId = createdStoppunkt.id,
                )
                assertNull(createdKandidat.svarAt)

                kartleggingssporsmalService.registrerSvar(
                    kandidatUuid = createdKandidat.uuid,
                    svarAt = OffsetDateTime.now().minusHours(1),
                    svarId = UUID.randomUUID(),
                )
                val secondSvarAt = OffsetDateTime.now()
                kartleggingssporsmalService.registrerSvar(
                    kandidatUuid = createdKandidat.uuid,
                    svarAt = secondSvarAt,
                    svarId = UUID.randomUUID(),
                )

                val fetchedKandidat = kartleggingssporsmalRepository.getKandidat(createdKandidat.uuid)
                assertEquals(secondSvarAt.toLocalDateOslo(), fetchedKandidat?.svarAt?.toLocalDateOslo())
            }
        }

        @Test
        fun `registrerSvar should not store svar when IKKE_KANDIDAT`() {
            val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
                tilfelleStart = LocalDate.now().minusDays(stoppunktStartIntervalDays),
                antallSykedager = stoppunktStartIntervalDays.toInt() + 1,
            )
            val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)
            assertNotNull(stoppunkt)

            runBlocking {
                kartleggingssporsmalRepository.createStoppunkt(stoppunkt)
                val createdStoppunkt = database.getKartleggingssporsmalStoppunkt().first()

                val kandidat = KartleggingssporsmalKandidat(
                    personident = ARBEIDSTAKER_PERSONIDENT,
                    status = KandidatStatus.IKKE_KANDIDAT,
                )
                val createdKandidat = kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                    kandidat = kandidat,
                    stoppunktId = createdStoppunkt.id,
                )

                kartleggingssporsmalService.registrerSvar(
                    kandidatUuid = createdKandidat.uuid,
                    svarAt = OffsetDateTime.now(),
                    svarId = UUID.randomUUID(),
                )

                val fetchedKandidat = kartleggingssporsmalRepository.getKandidat(createdKandidat.uuid)
                assertNull(fetchedKandidat?.svarAt)
            }
        }

        @Test
        fun `registrerSvar should not store svar when no existing kandidat`() {
            val kandidatUuid = UUID.randomUUID()
            runBlocking {
                kartleggingssporsmalService.registrerSvar(
                    kandidatUuid = kandidatUuid,
                    svarAt = OffsetDateTime.now(),
                    svarId = UUID.randomUUID(),
                )

                val fetchedKandidat = kartleggingssporsmalRepository.getKandidat(kandidatUuid)
                assertNull(fetchedKandidat)
            }
        }
    }

    companion object {
        @JvmStatic
        fun provideIkkeKandidatScenarios() = listOf(
            Arguments.of(ARBEIDSTAKER_PERSONIDENT_HAS_14A.value, "has 14a vedtak"),
            Arguments.of(ARBEIDSTAKER_PERSONIDENT_TOO_OLD.value, "too old"),
            Arguments.of(ARBEIDSTAKER_PERSONIDENT_TILFELLE_SHORT.value, "tilfelle is not longer than stoppunkt anymore"),
            Arguments.of(ARBEIDSTAKER_PERSONIDENT_TILFELLE_DOD.value, "person is dod"),
            Arguments.of(ARBEIDSTAKER_PERSONIDENT_NO_ARBEIDSGIVER.value, "person is not arbeidstaker"),
            Arguments.of(ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET.value, "person not in pilot"),
        )
    }
}
