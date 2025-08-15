package no.nav.syfo.kartleggingssporsmal.application

import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt.Companion.KARTLEGGINGSSPORSMAL_STOPPUNKT_END_DAYS
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt.Companion.KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS
import no.nav.syfo.kartleggingssporsmal.domain.Oppfolgingstilfelle
import no.nav.syfo.shared.util.toLocalDateOslo
import org.slf4j.LoggerFactory
import java.util.*

class KartleggingssporsmalService(
    private val behandlendeEnhetClient: IBehandlendeEnhetClient,
    private val kartleggingssporsmalRepository: IKartleggingssporsmalRepository,
) {

    suspend fun processOppfolgingstilfelle(oppfolgingstilfelle: Oppfolgingstilfelle) {
        if (isRelevantForPlanlagtKandidat(oppfolgingstilfelle) && !isAlreadyKandidatInTilfelle(oppfolgingstilfelle)) {
            val kartleggingssporsmalStoppunkt = KartleggingssporsmalStoppunkt(
                personident = oppfolgingstilfelle.personident,
                tilfelleBitReferanseUuid = oppfolgingstilfelle.tilfelleBitReferanseUuid,
                tilfelleStart = oppfolgingstilfelle.tilfelleStart,
                tilfelleEnd = oppfolgingstilfelle.tilfelleEnd,
            )
            kartleggingssporsmalRepository.createStoppunkt(stoppunkt = kartleggingssporsmalStoppunkt)

            log.info(
                """
                Oppfolgingstilfelle with uuid: ${oppfolgingstilfelle.uuid} has generated a stoppunkt.
                Stoppunkt dato: ${kartleggingssporsmalStoppunkt.stoppunktAt}
                Tilfelle start: ${oppfolgingstilfelle.tilfelleStart}
                Tilfelle end: ${oppfolgingstilfelle.tilfelleEnd}
                Antall sykedager: ${oppfolgingstilfelle.antallSykedager}
                """.trimIndent()
            )
        } else {
            log.info("Oppfolgingstilfelle with uuid: ${oppfolgingstilfelle.uuid} is not relevant for kartleggingssporsmal")
        }
    }

    private suspend fun isRelevantForPlanlagtKandidat(
        oppfolgingstilfelle: Oppfolgingstilfelle,
    ): Boolean {
        val behandlendeEnhetDTO = behandlendeEnhetClient.getEnhet(
            callId = UUID.randomUUID().toString(),
            personident = oppfolgingstilfelle.personident,
        ) ?: throw IllegalStateException("Mangler enhet for person med oppfolgingstilfelle-uuid: ${oppfolgingstilfelle.uuid}")

        val enhet = behandlendeEnhetDTO.oppfolgingsenhetDTO?.enhet
            ?: behandlendeEnhetDTO.geografiskEnhet

        return oppfolgingstilfelleInsideStoppunktInterval(oppfolgingstilfelle) &&
            !oppfolgingstilfelle.isDod() &&
            !oppfolgingstilfelle.hasTilfelleWithEndMoreThanThirtyDaysAgo() &&
            isInPilot(enhet.enhetId)
    }

    private suspend fun isAlreadyKandidatInTilfelle(oppfolgingstilfelle: Oppfolgingstilfelle): Boolean {
        // TODO: Kan endringer i oppfølgingstilfelle føre til at man ble kandidat innefor en tidligere versjon av tilfellet,
        //  men at det tilfellet er annerledes nå når denne nye recorden kommer inn?
        val existingKandidat = kartleggingssporsmalRepository.getKandidat(oppfolgingstilfelle.personident)
        return existingKandidat != null &&
            oppfolgingstilfelle.datoInsideCurrentTilfelle(
                dato = existingKandidat.createdAt.toLocalDateOslo()
            )
    }

    private fun oppfolgingstilfelleInsideStoppunktInterval(oppfolgingstilfelle: Oppfolgingstilfelle): Boolean {
        // TODO: Hva med de som er før start-tidspunkt på intervallet, og så får en veldig lang sykmelding som strekker seg over intervallet?
        return oppfolgingstilfelle.durationInDays() >= KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS &&
            oppfolgingstilfelle.durationInDays() <= KARTLEGGINGSSPORSMAL_STOPPUNKT_END_DAYS
    }

    private fun isInPilot(enhetId: String) = enhetId in pilotkontorer

    companion object {
        private val log = LoggerFactory.getLogger(KartleggingssporsmalService::class.java)
        private const val PILOTKONTOR_TEST = "0314"
        private val pilotkontorer = listOf(PILOTKONTOR_TEST)
    }
}
