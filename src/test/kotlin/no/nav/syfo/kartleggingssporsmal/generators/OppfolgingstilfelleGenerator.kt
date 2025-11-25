package no.nav.syfo.kartleggingssporsmal.generators

import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt.Companion.KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS
import no.nav.syfo.kartleggingssporsmal.domain.Oppfolgingstilfelle
import no.nav.syfo.kartleggingssporsmal.domain.OppfolgingstilfelleDTO
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.oppfolgingstilfelle.OppfolgingstilfellePersonDTO
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.oppfolgingstilfelle.KafkaOppfolgingstilfellePersonDTO
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.util.nowUTC
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

fun createKafkaOppfolgingstilfellePersonDTO(
    personident: Personident = ARBEIDSTAKER_PERSONIDENT,
    tilfelleStart: LocalDate = LocalDate.now(),
    antallSykedager: Int? = KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS.toInt() + 1,
    tilfelleEnd: LocalDate = tilfelleStart.plusDays(
        antallSykedager?.toLong() ?: (KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS + 10)
    ),
    dodsdato: LocalDate? = null,
    arbeidstakerAtTilfelleEnd: Boolean = true,
    extraOppfolgingstilfeller: List<OppfolgingstilfelleDTO> = emptyList(),
): KafkaOppfolgingstilfellePersonDTO = KafkaOppfolgingstilfellePersonDTO(
    uuid = UUID.randomUUID().toString(),
    createdAt = nowUTC(),
    personIdentNumber = personident.value,
    oppfolgingstilfelleList = listOf(
        createOppfolgingstilfelleDTO(
            tilfelleStart = tilfelleStart,
            tilfelleEnd = tilfelleEnd,
            antallSykedager = antallSykedager,
            virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER),
            arbeidstakerAtTilfelleEnd = arbeidstakerAtTilfelleEnd,
        ),
    ) + extraOppfolgingstilfeller,
    referanseTilfelleBitUuid = UUID.randomUUID().toString(),
    dodsdato = dodsdato,
)

fun createOppfolgingstilfelleFromKafka(
    uuid: UUID = UUID.randomUUID(),
    personident: Personident = ARBEIDSTAKER_PERSONIDENT,
    tilfelleStart: LocalDate = LocalDate.now(),
    antallSykedager: Int? = KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS.toInt() + 1,
    tilfelleEnd: LocalDate = tilfelleStart.plusDays(
        if (antallSykedager != null) {
            antallSykedager.toLong() - 1 // Antall sykedager inkluderer +1 i sin lengde, så man vil legge til én dag for mye hvis man kun legger til antallSykedager
        } else {
            KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS + 10
        }
    ),
    isArbeidstakerAtTilfelleEnd: Boolean = true,
    virksomhetsnummerList: List<String> = listOf(VIRKSOMHETSNUMMER),
    dodsdato: LocalDate? = null,
) = Oppfolgingstilfelle.OppfolgingstilfelleFromKafka(
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

fun createOppfolgingstilfellePersonDTO(
    personident: String = ARBEIDSTAKER_PERSONIDENT.value,
    tilfelleStart: LocalDate = LocalDate.now(),
    antallSykedager: Int? = null,
    tilfelleEnd: LocalDate = tilfelleStart.plusDays(
        antallSykedager?.toLong() ?: (KARTLEGGINGSSPORSMAL_STOPPUNKT_START_DAYS + 10)
    ),
    isArbeidstakerAtTilfelleEnd: Boolean = true,
    virksomhetsnummerList: List<String> = listOf(VIRKSOMHETSNUMMER),
    dodsdato: LocalDate? = null,
    hasTilfelle: Boolean = true,
) = OppfolgingstilfellePersonDTO(
    personIdent = personident,
    dodsdato = dodsdato,
    oppfolgingstilfelleList = if (hasTilfelle) listOf(
        createOppfolgingstilfelleDTO(
            tilfelleStart = tilfelleStart,
            tilfelleEnd = tilfelleEnd,
            antallSykedager = antallSykedager,
            arbeidstakerAtTilfelleEnd = isArbeidstakerAtTilfelleEnd,
            virksomhetsnummerList = virksomhetsnummerList,
        )
    ) else emptyList(),
)

fun createOppfolgingstilfelleDTO(
    tilfelleStart: LocalDate,
    tilfelleEnd: LocalDate,
    antallSykedager: Int?,
    virksomhetsnummerList: List<String> = listOf(VIRKSOMHETSNUMMER),
    arbeidstakerAtTilfelleEnd: Boolean = true,
): OppfolgingstilfelleDTO = OppfolgingstilfelleDTO(
    arbeidstakerAtTilfelleEnd = arbeidstakerAtTilfelleEnd,
    start = tilfelleStart,
    end = tilfelleEnd,
    antallSykedager = antallSykedager,
    virksomhetsnummerList = virksomhetsnummerList,
)
