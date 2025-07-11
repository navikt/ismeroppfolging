package no.nav.syfo.kartleggingssporsmal.generators

import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.KafkaOppfolgingstilfelle
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.KafkaOppfolgingstilfellePersonDTO
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
    extraOppfolgingstilfeller: List<KafkaOppfolgingstilfelle> = emptyList(),
): KafkaOppfolgingstilfellePersonDTO = KafkaOppfolgingstilfellePersonDTO(
    uuid = UUID.randomUUID().toString(),
    createdAt = nowUTC(),
    personIdentNumber = personident.value,
    oppfolgingstilfelleList = listOf(
        createKafkaOppfolgingstilfelle(
            tilfelleStart = tilfelleStart,
            tilfelleEnd = tilfelleEnd,
            gradert = gradert,
            virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER),
            arbeidstakerAtTilfelleEnd = arbeidstakerAtTilfelleEnd,
        ),
    ) + extraOppfolgingstilfeller,
    referanseTilfelleBitUuid = UUID.randomUUID().toString(),
    referanseTilfelleBitInntruffet = nowUTC().minusDays(1),
    dodsdato = dodsdato,
)

fun createKafkaOppfolgingstilfelle(
    tilfelleStart: LocalDate = LocalDate.now().minusDays(10),
    tilfelleEnd: LocalDate = LocalDate.now().plusDays(20),
    gradert: Boolean = false,
    virksomhetsnummerList: List<String> = listOf(VIRKSOMHETSNUMMER),
    arbeidstakerAtTilfelleEnd: Boolean = true,
): KafkaOppfolgingstilfelle = KafkaOppfolgingstilfelle(
    arbeidstakerAtTilfelleEnd = arbeidstakerAtTilfelleEnd,
    start = tilfelleStart,
    end = tilfelleEnd,
    antallSykedager = ChronoUnit.DAYS.between(tilfelleStart, tilfelleEnd).toInt() + 1,
    virksomhetsnummerList = virksomhetsnummerList,
    gradertAtTilfelleEnd = gradert,
)
