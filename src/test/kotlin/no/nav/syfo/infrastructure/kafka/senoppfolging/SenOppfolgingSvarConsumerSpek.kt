package no.nav.syfo.infrastructure.kafka.senoppfolging

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.application.SenOppfolgingService
import no.nav.syfo.domain.*
import no.nav.syfo.generators.generateSenOppfolgingSvarRecord
import no.nav.syfo.infrastructure.database.dropData
import no.nav.syfo.infrastructure.database.getSenOppfolgingKandidater
import no.nav.syfo.infrastructure.database.getSenOppfolgingVurderinger
import no.nav.syfo.infrastructure.database.repository.SenOppfolgingRepository
import no.nav.syfo.infrastructure.kafka.KandidatStatusProducer
import no.nav.syfo.infrastructure.kafka.KandidatStatusRecord
import no.nav.syfo.mocks.mockPollConsumerRecords
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.OffsetDateTime
import java.util.*

class SenOppfolgingSvarConsumerSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val kafkaConsumer = mockk<KafkaConsumer<String, SenOppfolgingSvarRecord>>()
    val kafkaProducer = mockk<KafkaProducer<String, KandidatStatusRecord>>()

    val senOppfolgingRepository = SenOppfolgingRepository(database = database)
    val senOppfolgingService = SenOppfolgingService(
        senOppfolgingRepository = senOppfolgingRepository,
        kandidatStatusProducer = KandidatStatusProducer(kafkaProducer),
    )
    val senOppfolgingSvarConsumer = SenOppfolgingSvarConsumer(senOppfolgingService = senOppfolgingService)

    beforeEachTest {
        every { kafkaConsumer.commitSync() } returns Unit
    }

    afterEachTest {
        database.dropData()
        clearAllMocks()
    }

    describe("pollAndProcessRecords") {
        it("creates kandidat with svar onsker oppfolging JA when answer is JA on BEHOV_FOR_OPPFOLGING") {
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
            kandidater.size shouldBeEqualTo 1
            val kandidat = kandidater.first()
            kandidat.svarAt.shouldNotBeNull()
            kandidat.onskerOppfolging shouldBeEqualTo OnskerOppfolging.JA.name
        }

        it("creates kandidat with svar onsker oppfolging NEI when answer is NEI on BEHOV_FOR_OPPFOLGING") {
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
            kandidater.size shouldBeEqualTo 1
            val kandidat = kandidater.first()
            kandidat.svarAt.shouldNotBeNull()
            kandidat.onskerOppfolging shouldBeEqualTo OnskerOppfolging.NEI.name
        }

        it("updates existing kandidat with svar onsker oppfolging JA when answer is JA on BEHOV_FOR_OPPFOLGING") {
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
            kandidater.size shouldBeEqualTo 1
            val pKandidat = kandidater.first()
            pKandidat.svarAt.shouldNotBeNull()
            pKandidat.onskerOppfolging shouldBeEqualTo OnskerOppfolging.JA.name
        }
        it("Does update existing kandidat with svar when svar already stored") {
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
            kandidater.size shouldBeEqualTo 1
            val pKandidat = kandidater.first()
            pKandidat.svarAt.shouldNotBeNull()
            pKandidat.onskerOppfolging shouldBeEqualTo OnskerOppfolging.JA.name
        }
        it("Creates new kandidat with svar when existing ferdigbehandlet") {
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
            kandidater.size shouldBeEqualTo 2
            val pKandidat = kandidater[0]
            pKandidat.svarAt.shouldNotBeNull()
            pKandidat.onskerOppfolging shouldBeEqualTo OnskerOppfolging.JA.name
            val pKandidatOldest = kandidater[1]
            val pVurdering = database.getSenOppfolgingVurderinger().first()
            pVurdering.kandidatId shouldBeEqualTo pKandidatOldest.id
        }
        it("Creates new kandidat with svar when existing with svar ferdigbehandlet") {
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
            kandidater.size shouldBeEqualTo 2
            val pKandidat = kandidater[0]
            pKandidat.svarAt.shouldNotBeNull()
            pKandidat.onskerOppfolging shouldBeEqualTo OnskerOppfolging.JA.name
            val pKandidatOldest = kandidater[1]
            pKandidatOldest.onskerOppfolging shouldBeEqualTo OnskerOppfolging.NEI.name
            val pVurdering = database.getSenOppfolgingVurderinger().first()
            pVurdering.kandidatId shouldBeEqualTo pKandidatOldest.id
        }
        it("updates existing kandidat with correct varselId") {
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
            kandidater.size shouldBeEqualTo 2
            val pKandidat = kandidater.first()
            pKandidat.varselId shouldBeEqualTo kandidatWithVarselId.varselId
            pKandidat.svarAt.shouldNotBeNull()
            pKandidat.onskerOppfolging shouldBeEqualTo OnskerOppfolging.JA.name
            kandidater[1].uuid shouldBeEqualTo kandidatWithoutVarselId.uuid
            kandidater[1].svarAt.shouldBeNull()
        }
    }
})
