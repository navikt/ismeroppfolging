package no.nav.syfo.identhendelse

import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.domain.SenOppfolgingKandidat
import no.nav.syfo.generators.generateKafkaIdenthendelseDTO
import no.nav.syfo.infrastructure.database.dropData
import no.nav.syfo.infrastructure.database.repository.SenOppfolgingRepository
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object IdenthendelseServiceSpek : Spek({

    describe(IdenthendelseServiceSpek::class.java.simpleName) {
        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val senOppfolgingRepository = SenOppfolgingRepository(database)

        val identhendelseService = IdenthendelseService(
            senOppfolgingRepository = senOppfolgingRepository,
        )

        afterEachTest {
            database.dropData()
        }

        describe("Happy path") {
            it("Skal oppdatere database når person har fått ny ident") {
                val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO()
                val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!
                val oldIdent = kafkaIdenthendelseDTO.getInactivePersonidenter().first()
                val kandidat = SenOppfolgingKandidat(
                    personident = oldIdent,
                    varselAt = null,
                )
                senOppfolgingRepository.createKandidat(kandidat)
                senOppfolgingRepository.getKandidater(oldIdent).size shouldBeEqualTo 1
                senOppfolgingRepository.getKandidater(newIdent).size shouldBeEqualTo 0

                identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)

                senOppfolgingRepository.getKandidater(oldIdent).size shouldBeEqualTo 0
                senOppfolgingRepository.getKandidater(newIdent).size shouldBeEqualTo 1
            }
            it("Skal ikke oppdatere database når melding allerede har ny ident") {
                val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO()
                val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!
                val oldIdent = kafkaIdenthendelseDTO.getInactivePersonidenter().first()

                val kandidat = SenOppfolgingKandidat(
                    personident = newIdent,
                    varselAt = null,
                )
                senOppfolgingRepository.createKandidat(kandidat)
                senOppfolgingRepository.getKandidater(oldIdent).size shouldBeEqualTo 0
                senOppfolgingRepository.getKandidater(newIdent).size shouldBeEqualTo 1

                identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)

                senOppfolgingRepository.getKandidater(oldIdent).size shouldBeEqualTo 0
                senOppfolgingRepository.getKandidater(newIdent).size shouldBeEqualTo 1
            }
        }
    }
})
