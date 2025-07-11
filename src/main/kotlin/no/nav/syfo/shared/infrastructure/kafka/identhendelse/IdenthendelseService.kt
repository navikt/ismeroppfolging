package no.nav.syfo.shared.infrastructure.kafka.identhendelse

import no.nav.syfo.senoppfolging.application.ISenOppfolgingRepository
import no.nav.syfo.shared.infrastructure.kafka.identhendelse.kafka.KafkaIdenthendelseDTO
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class IdenthendelseService(
    private val senOppfolgingRepository: ISenOppfolgingRepository,
) {

    private val log: Logger = LoggerFactory.getLogger(IdenthendelseService::class.java)

    fun handleIdenthendelse(identhendelse: KafkaIdenthendelseDTO) {
        if (identhendelse.folkeregisterIdenter.size > 1) {
            val activeIdent = identhendelse.getActivePersonident()
            if (activeIdent != null) {
                val inactiveIdenter = identhendelse.getInactivePersonidenter()
                val kandidaterWithOldIdent = inactiveIdenter.flatMap { personident ->
                    senOppfolgingRepository.getKandidater(personident)
                }

                if (kandidaterWithOldIdent.isNotEmpty()) {
                    senOppfolgingRepository.updateKandidatPersonident(kandidaterWithOldIdent, activeIdent)
                    log.info("Identhendelse: Updated ${kandidaterWithOldIdent.size} kandidater based on Identhendelse from PDL")
                }
            }
        }
    }
}
