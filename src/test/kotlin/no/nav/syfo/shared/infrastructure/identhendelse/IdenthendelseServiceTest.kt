package no.nav.syfo.shared.infrastructure.identhendelse

import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.kartleggingssporsmal.domain.Skjemavariant
import no.nav.syfo.kartleggingssporsmal.generators.createOppfolgingstilfelleFromKafka
import no.nav.syfo.kartleggingssporsmal.infrastructure.database.KartleggingssporsmalRepository
import no.nav.syfo.senoppfolging.domain.SenOppfolgingKandidat
import no.nav.syfo.shared.generators.generateKafkaIdenthendelseDTO
import no.nav.syfo.senoppfolging.infrastructure.database.repository.SenOppfolgingRepository
import no.nav.syfo.shared.infrastructure.database.getKartleggingssporsmalStoppunkt
import no.nav.syfo.shared.infrastructure.kafka.identhendelse.IdenthendelseService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class IdenthendelseServiceTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val senOppfolgingRepository = SenOppfolgingRepository(database)
    private val kartleggingssporsmalRepository = KartleggingssporsmalRepository(database)

    private val identhendelseService = IdenthendelseService(
        senOppfolgingRepository = senOppfolgingRepository,
        kartleggingssporsmalRepository = kartleggingssporsmalRepository,
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

    @Test
    fun `Skal oppdatere kartleggingssporsmal stoppunkt og kandidat nar person har fatt ny ident`() {
        val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO()
        val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!
        val oldIdent = kafkaIdenthendelseDTO.getInactivePersonidenter().first()
        val oppfolgingstilfelle = createOppfolgingstilfelleFromKafka(
            personident = oldIdent,
            tilfelleStart = LocalDate.now().minusDays(6 * 7),
            antallSykedager = 6 * 7 + 1,
        )
        val stoppunkt = KartleggingssporsmalStoppunkt.create(oppfolgingstilfelle)!!
        val kandidat = runBlocking {
            kartleggingssporsmalRepository.createStoppunkt(stoppunkt)
            val stoppunktId = database.getKartleggingssporsmalStoppunkt().first().id
            kartleggingssporsmalRepository.createKandidatAndMarkStoppunktAsProcessed(
                kandidat = KartleggingssporsmalKandidat.create(
                    personident = oldIdent,
                    skjemavariant = Skjemavariant.FLERVALG_V2,
                ),
                stoppunktId = stoppunktId,
            )
        }

        identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)

        assertEquals(newIdent, database.getKartleggingssporsmalStoppunkt().first().personident)
        runBlocking {
            assertEquals(newIdent, kartleggingssporsmalRepository.getKandidat(kandidat.uuid)!!.personident)
        }
    }
}
