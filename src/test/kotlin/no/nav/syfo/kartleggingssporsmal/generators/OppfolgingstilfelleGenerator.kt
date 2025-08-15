package no.nav.syfo.kartleggingssporsmal.generators

import no.nav.syfo.UserConstants
import no.nav.syfo.kartleggingssporsmal.domain.Oppfolgingstilfelle
import no.nav.syfo.shared.domain.Personident
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

fun createOppfolgingstilfelle(
    uuid: UUID = UUID.randomUUID(),
    personident: Personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
    tilfelleStart: LocalDate = LocalDate.now(),
    tilfelleEnd: LocalDate = LocalDate.now().plusDays(30),
    antallSykedager: Int? = ChronoUnit.DAYS.between(tilfelleStart, tilfelleEnd).toInt() + 1,
    isArbeidstakerAtTilfelleEnd: Boolean = true,
    virksomhetsnummerList: List<String> = listOf(UserConstants.VIRKSOMHETSNUMMER),
    dodsdato: LocalDate? = null,
) = Oppfolgingstilfelle(
    uuid = uuid,
    personident = personident,
    tilfelleGenerert = OffsetDateTime.now(),
    tilfelleBitReferanseUuid = UUID.randomUUID(),
    tilfelleStart = tilfelleStart,
    tilfelleEnd = tilfelleEnd,
    antallSykedager = antallSykedager,
    dodsdato = dodsdato,
    isArbeidstakerAtTilfelleEnd = isArbeidstakerAtTilfelleEnd,
    virksomhetsnummerList = virksomhetsnummerList,
)
