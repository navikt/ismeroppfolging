package no.nav.syfo.identhendelse

import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.domain.SenOppfolgingKandidat
import no.nav.syfo.generators.generateKafkaIdenthendelseDTO
import no.nav.syfo.infrastructure.database.repository.SenOppfolgingRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IdenthendelseServiceTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val senOppfolgingRepository = SenOppfolgingRepository(database)

    private val identhendelseService = IdenthendelseService(
        senOppfolgingRepository = senOppfolgingRepository,
    )

    @AfterEach
    fun tearDown() {
        database.resetDatabase()
    }

    @Test
    fun `Skal oppdatere database når person har fått ny ident`() {
        val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO()
        val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!
        val oldIdent = kafkaIdenthendelseDTO.getInactivePersonidenter().first()
        val kandidat = SenOppfolgingKandidat(
            personident = oldIdent,
            varselAt = null,
        )
        senOppfolgingRepository.createKandidat(kandidat)
        assertEquals(1, senOppfolgingRepository.getKandidater(oldIdent).size)
        assertEquals(0, senOppfolgingRepository.getKandidater(newIdent).size)

        identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)

        assertEquals(0, senOppfolgingRepository.getKandidater(oldIdent).size)
        assertEquals(1, senOppfolgingRepository.getKandidater(newIdent).size)
    }

    @Test
    fun `Skal ikke oppdatere database når melding allerede har ny ident`() {
        val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO()
        val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!
        val oldIdent = kafkaIdenthendelseDTO.getInactivePersonidenter().first()

        val kandidat = SenOppfolgingKandidat(
            personident = newIdent,
            varselAt = null,
        )
        senOppfolgingRepository.createKandidat(kandidat)
        assertEquals(0, senOppfolgingRepository.getKandidater(oldIdent).size)
        assertEquals(1, senOppfolgingRepository.getKandidater(newIdent).size)

        identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)

        assertEquals(0, senOppfolgingRepository.getKandidater(oldIdent).size)
        assertEquals(1, senOppfolgingRepository.getKandidater(newIdent).size)
    }
}
