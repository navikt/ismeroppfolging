package no.nav.syfo.senoppfolging.infrastructure.kafka

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.senoppfolging.application.SenOppfolgingService
import no.nav.syfo.senoppfolging.domain.OnskerOppfolging
import no.nav.syfo.senoppfolging.domain.SenOppfolgingKandidat
import no.nav.syfo.senoppfolging.domain.SenOppfolgingSvar
import no.nav.syfo.senoppfolging.domain.VurderingType
import no.nav.syfo.senoppfolging.generators.generateSenOppfolgingSvarRecord
import no.nav.syfo.shared.infrastructure.database.getSenOppfolgingKandidater
import no.nav.syfo.shared.infrastructure.database.getSenOppfolgingVurderinger
import no.nav.syfo.senoppfolging.infrastructure.database.repository.SenOppfolgingRepository
import no.nav.syfo.senoppfolging.infrastructure.kafka.consumer.*
import no.nav.syfo.senoppfolging.infrastructure.kafka.producer.*
import no.nav.syfo.shared.infrastructure.kafka.mockPollConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.*

class SenOppfolgingSvarConsumerTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val kafkaConsumer = mockk<KafkaConsumer<String, SenOppfolgingSvarRecord>>()
    private val kafkaProducer = mockk<KafkaProducer<String, KandidatStatusRecord>>()

    private val senOppfolgingRepository = SenOppfolgingRepository(database = database)
    private val senOppfolgingService = SenOppfolgingService(
        senOppfolgingRepository = senOppfolgingRepository,
        kandidatStatusProducer = KandidatStatusProducer(kafkaProducer),
    )
    private val senOppfolgingSvarConsumer = SenOppfolgingSvarConsumer(senOppfolgingService = senOppfolgingService)

    @BeforeEach
    fun setUp() {
        every { kafkaConsumer.commitSync() } returns Unit
    }

    @AfterEach
    fun tearDown() {
        database.resetDatabase()
        clearAllMocks()
    }

    @Test
    fun `creates kandidat with svar onsker oppfolging JA when answer is JA on BEHOV_FOR_OPPFOLGING`() {
        val recordKey = UUID.randomUUID().toString()
        val question = SenOppfolgingQuestion(
            questionType = SenOppfolgingQuestionType.BEHOV_FOR_OPPFOLGING.name,
            answerType = BehovForOppfolgingSvar.JA.name,
        )
        val senOppfolgingSvarRecord = generateSenOppfolgingSvarRecord(
            question = question,
            varselId = UUID.randomUUID(),
        )

        kafkaConsumer.mockPollConsumerRecords(
            records = listOf(recordKey to senOppfolgingSvarRecord),
            topic = SENOPPFOLGING_SVAR_TOPIC,
        )

        senOppfolgingSvarConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }

        val kandidater = database.getSenOppfolgingKandidater()
        assertEquals(1, kandidater.size)
        val kandidat = kandidater.first()
        assertNotNull(kandidat.svarAt)
        assertEquals(OnskerOppfolging.JA.name, kandidat.onskerOppfolging)
    }

    @Test
    fun `creates kandidat with svar onsker oppfolging NEI when answer is NEI on BEHOV_FOR_OPPFOLGING`() {
        val recordKey = UUID.randomUUID().toString()
        val question = SenOppfolgingQuestion(
            questionType = SenOppfolgingQuestionType.BEHOV_FOR_OPPFOLGING.name,
            answerType = BehovForOppfolgingSvar.NEI.name,
        )
        val senOppfolgingSvarRecord = generateSenOppfolgingSvarRecord(question = question, varselId = UUID.randomUUID())

        kafkaConsumer.mockPollConsumerRecords(
            records = listOf(recordKey to senOppfolgingSvarRecord),
            topic = SENOPPFOLGING_SVAR_TOPIC,
        )

        senOppfolgingSvarConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }

        val kandidater = database.getSenOppfolgingKandidater()
        assertEquals(1, kandidater.size)
        val kandidat = kandidater.first()
        assertNotNull(kandidat.svarAt)
        assertEquals(OnskerOppfolging.NEI.name, kandidat.onskerOppfolging)
    }

    @Test
    fun `updates existing kandidat with svar onsker oppfolging JA when answer is JA on BEHOV_FOR_OPPFOLGING`() {
        senOppfolgingRepository.createKandidat(
            SenOppfolgingKandidat(
                personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
                varselAt = OffsetDateTime.now(),
            )
        )
        val recordKey = UUID.randomUUID().toString()
        val question = SenOppfolgingQuestion(
            questionType = SenOppfolgingQuestionType.BEHOV_FOR_OPPFOLGING.name,
            answerType = BehovForOppfolgingSvar.JA.name,
        )
        val senOppfolgingSvarRecord = generateSenOppfolgingSvarRecord(
            personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT.value,
            question = question,
            varselId = UUID.randomUUID(),
        )

        kafkaConsumer.mockPollConsumerRecords(
            records = listOf(recordKey to senOppfolgingSvarRecord),
            topic = SENOPPFOLGING_SVAR_TOPIC,
        )

        senOppfolgingSvarConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }

        val kandidater = database.getSenOppfolgingKandidater()
        assertEquals(1, kandidater.size)
        val pKandidat = kandidater.first()
        assertNotNull(pKandidat.svarAt)
        assertEquals(OnskerOppfolging.JA.name, pKandidat.onskerOppfolging)
    }

    @Test
    fun `Does update existing kandidat with svar when svar already stored`() {
        val kandidat = senOppfolgingRepository.createKandidat(
            SenOppfolgingKandidat(
                personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
                varselAt = OffsetDateTime.now(),
            )
        )
        senOppfolgingRepository.updateKandidatSvar(
            senOppfolgingSvar = SenOppfolgingSvar(
                svarAt = OffsetDateTime.now(),
                onskerOppfolging = OnskerOppfolging.NEI,
            ),
            senOppfolgingKandidaUuid = kandidat.uuid,
        )
        val recordKey = UUID.randomUUID().toString()
        val question = SenOppfolgingQuestion(
            questionType = SenOppfolgingQuestionType.BEHOV_FOR_OPPFOLGING.name,
            answerType = BehovForOppfolgingSvar.JA.name,
        )
        val senOppfolgingSvarRecord = generateSenOppfolgingSvarRecord(
            personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT.value,
            question = question,
            varselId = UUID.randomUUID(),
        )

        kafkaConsumer.mockPollConsumerRecords(
            records = listOf(recordKey to senOppfolgingSvarRecord),
            topic = SENOPPFOLGING_SVAR_TOPIC,
        )

        senOppfolgingSvarConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }

        val kandidater = database.getSenOppfolgingKandidater()
        assertEquals(1, kandidater.size)
        val pKandidat = kandidater.first()
        assertNotNull(pKandidat.svarAt)
        assertEquals(OnskerOppfolging.JA.name, pKandidat.onskerOppfolging)
    }

    @Test
    fun `Creates new kandidat with svar when existing ferdigbehandlet`() {
        val varselId = UUID.randomUUID()
        val kandidat = senOppfolgingService.createKandidat(
            personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
            varselAt = OffsetDateTime.now(),
            varselId = varselId,
        )
        senOppfolgingService.vurderKandidat(
            kandidat = kandidat,
            veilederident = UserConstants.VEILEDER_IDENT,
            begrunnelse = "begrunnelse",
            type = VurderingType.FERDIGBEHANDLET,
        )

        val recordKey = UUID.randomUUID().toString()
        val question = SenOppfolgingQuestion(
            questionType = SenOppfolgingQuestionType.BEHOV_FOR_OPPFOLGING.name,
            answerType = BehovForOppfolgingSvar.JA.name,
        )
        val senOppfolgingSvarRecord = generateSenOppfolgingSvarRecord(
            personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT.value,
            question = question,
            varselId = varselId,
        )

        kafkaConsumer.mockPollConsumerRecords(
            records = listOf(recordKey to senOppfolgingSvarRecord),
            topic = SENOPPFOLGING_SVAR_TOPIC,
        )

        senOppfolgingSvarConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }

        val kandidater = database.getSenOppfolgingKandidater()
        assertEquals(2, kandidater.size)
        val pKandidat = kandidater[0]
        assertNotNull(pKandidat.svarAt)
        assertEquals(OnskerOppfolging.JA.name, pKandidat.onskerOppfolging)
        val pKandidatOldest = kandidater[1]
        val pVurdering = database.getSenOppfolgingVurderinger().first()
        assertEquals(pKandidatOldest.id, pVurdering.kandidatId)
    }

    @Test
    fun `Creates new kandidat with svar when existing with svar ferdigbehandlet`() {
        val varselId = UUID.randomUUID()
        val kandidat = senOppfolgingService.createKandidat(
            personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
            varselAt = OffsetDateTime.now(),
            varselId = varselId,
        )
        senOppfolgingService.addSvar(kandidat, OffsetDateTime.now(), OnskerOppfolging.NEI)
        senOppfolgingService.vurderKandidat(
            kandidat = kandidat,
            veilederident = UserConstants.VEILEDER_IDENT,
            begrunnelse = "begrunnelse",
            type = VurderingType.FERDIGBEHANDLET,
        )

        val recordKey = UUID.randomUUID().toString()
        val question = SenOppfolgingQuestion(
            questionType = SenOppfolgingQuestionType.BEHOV_FOR_OPPFOLGING.name,
            answerType = BehovForOppfolgingSvar.JA.name,
        )
        val senOppfolgingSvarRecord = generateSenOppfolgingSvarRecord(
            personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT.value,
            question = question,
            varselId = varselId,
        )

        kafkaConsumer.mockPollConsumerRecords(
            records = listOf(recordKey to senOppfolgingSvarRecord),
            topic = SENOPPFOLGING_SVAR_TOPIC,
        )

        senOppfolgingSvarConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }

        val kandidater = database.getSenOppfolgingKandidater()
        assertEquals(2, kandidater.size)
        val pKandidat = kandidater[0]
        assertNotNull(pKandidat.svarAt)
        assertEquals(OnskerOppfolging.JA.name, pKandidat.onskerOppfolging)
        val pKandidatOldest = kandidater[1]
        assertEquals(OnskerOppfolging.NEI.name, pKandidatOldest.onskerOppfolging)
        val pVurdering = database.getSenOppfolgingVurderinger().first()
        assertEquals(pKandidatOldest.id, pVurdering.kandidatId)
    }

    @Test
    fun `updates existing kandidat with correct varselId`() {
        val kandidatWithoutVarselId = senOppfolgingRepository.createKandidat(
            SenOppfolgingKandidat(
                personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
                varselAt = OffsetDateTime.now(),
            )
        )
        val varselId = UUID.randomUUID()
        val kandidatWithVarselId = senOppfolgingRepository.createKandidat(
            SenOppfolgingKandidat(
                personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
                varselAt = OffsetDateTime.now(),
                varselId = varselId,
            )
        )
        val recordKey = UUID.randomUUID().toString()
        val question = SenOppfolgingQuestion(
            questionType = SenOppfolgingQuestionType.BEHOV_FOR_OPPFOLGING.name,
            answerType = BehovForOppfolgingSvar.JA.name,
        )
        val senOppfolgingSvarRecord = generateSenOppfolgingSvarRecord(
            personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT.value,
            question = question,
            varselId = varselId,
        )

        kafkaConsumer.mockPollConsumerRecords(
            records = listOf(recordKey to senOppfolgingSvarRecord),
            topic = SENOPPFOLGING_SVAR_TOPIC,
        )

        senOppfolgingSvarConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }

        val kandidater = database.getSenOppfolgingKandidater()
        assertEquals(2, kandidater.size)
        val pKandidat = kandidater.first()
        assertEquals(kandidatWithVarselId.varselId, pKandidat.varselId)
        assertNotNull(pKandidat.svarAt)
        assertEquals(OnskerOppfolging.JA.name, pKandidat.onskerOppfolging)
        assertEquals(kandidatWithoutVarselId.uuid, kandidater[1].uuid)
        assertNull(kandidater[1].svarAt)
    }
}
