package no.nav.syfo.jobbforventning.generators

import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.jobbforventning.infrastructure.kafka.KafkaOppfolgingstilfelle
import no.nav.syfo.jobbforventning.infrastructure.kafka.KafkaOppfolgingstilfellePersonDTO
import no.nav.syfo.shared.util.nowUTC
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

fun createKafkaOppfolgingstilfellePerson(
    personident: Personident = ARBEIDSTAKER_PERSONIDENT,
    tilfelleStart: LocalDate = LocalDate.now().minusDays(10),
    tilfelleEnd: LocalDate = LocalDate.now().plusDays(20),
    gradert: Boolean = false,
    dodsdato: LocalDate? = null,
    arbeidstakerAtTilfelleEnd: Boolean = true,
): KafkaOppfolgingstilfellePersonDTO = KafkaOppfolgingstilfellePersonDTO(
    uuid = UUID.randomUUID().toString(),
    createdAt = nowUTC(),
    personIdentNumber = personident.value,
    oppfolgingstilfelleList = listOf(
        KafkaOppfolgingstilfelle(
            arbeidstakerAtTilfelleEnd = arbeidstakerAtTilfelleEnd,
            start = tilfelleStart,
            end = tilfelleEnd,
            antallSykedager = ChronoUnit.DAYS.between(tilfelleStart, tilfelleEnd).toInt() + 1,
            virksomhetsnummerList = if (arbeidstakerAtTilfelleEnd) listOf(VIRKSOMHETSNUMMER) else emptyList(),
            gradertAtTilfelleEnd = gradert,
        ),
    ),
    referanseTilfelleBitUuid = UUID.randomUUID().toString(),
    referanseTilfelleBitInntruffet = nowUTC().minusDays(1),
    dodsdato = dodsdato,
)
