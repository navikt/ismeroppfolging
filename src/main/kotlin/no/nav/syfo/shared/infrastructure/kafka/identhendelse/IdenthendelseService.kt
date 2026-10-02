package no.nav.syfo.shared.infrastructure.kafka.identhendelse

import no.nav.syfo.kartleggingssporsmal.application.IKartleggingssporsmalRepository
import no.nav.syfo.senoppfolging.application.ISenOppfolgingRepository
import no.nav.syfo.shared.infrastructure.kafka.identhendelse.kafka.KafkaIdenthendelseDTO
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class IdenthendelseService(
    private val senOppfolgingRepository: ISenOppfolgingRepository,
    private val kartleggingssporsmalRepository: IKartleggingssporsmalRepository,
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
                    log.info(
                        "Identhendelse: Updated personident for ${kandidaterWithOldIdent.size} SEN_OPPFOLGING_KANDIDAT " +
                            "rows, uuids=${kandidaterWithOldIdent.map { it.uuid }}"
                    )
                }

                val oppdateringer = inactiveIdenter.map { personident ->
                    kartleggingssporsmalRepository.updatePersonident(personident, activeIdent)
                }
                val kartleggingKandidatUuids = oppdateringer.flatMap { it.kandidatUuids }
                val kartleggingStoppunktUuids = oppdateringer.flatMap { it.stoppunktUuids }
                if (kartleggingKandidatUuids.isNotEmpty()) {
                    log.info(
                        "Identhendelse: Updated personident for ${kartleggingKandidatUuids.size} " +
                            "KARTLEGGINGSSPORSMAL_KANDIDAT rows, uuids=$kartleggingKandidatUuids"
                    )
                }
                if (kartleggingStoppunktUuids.isNotEmpty()) {
                    log.info(
                        "Identhendelse: Updated personident for ${kartleggingStoppunktUuids.size} " +
                            "KARTLEGGINGSSPORSMAL_STOPPUNKT rows, uuids=$kartleggingStoppunktUuids"
                    )
                }
            }
        }
    }
}
