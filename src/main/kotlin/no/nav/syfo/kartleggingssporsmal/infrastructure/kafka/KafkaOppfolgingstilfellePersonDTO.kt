package no.nav.syfo.kartleggingssporsmal.infrastructure.kafka

import no.nav.syfo.kartleggingssporsmal.domain.Oppfolgingstilfelle
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.util.tomorrow
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class KafkaOppfolgingstilfellePersonDTO(
    val uuid: String,
    val createdAt: OffsetDateTime,
    val personIdentNumber: String,
    val oppfolgingstilfelleList: List<KafkaOppfolgingstilfelle>,
    val referanseTilfelleBitUuid: String,
    val referanseTilfelleBitInntruffet: OffsetDateTime,
    val dodsdato: LocalDate?,
)

data class KafkaOppfolgingstilfelle(
    val gradertAtTilfelleEnd: Boolean,
    val arbeidstakerAtTilfelleEnd: Boolean,
    val start: LocalDate,
    val end: LocalDate,
    val antallSykedager: Int?,
    val virksomhetsnummerList: List<String>,
)

fun KafkaOppfolgingstilfellePersonDTO.toLatestOppfolgingstilfelle(): Oppfolgingstilfelle? =
    this.oppfolgingstilfelleList
        .filter { isNotTilfelleInTheFuture(it) }
        .maxByOrNull { it.start }
        ?.let { this.toOppfolgingstilfelle(it) }

private fun KafkaOppfolgingstilfellePersonDTO.toOppfolgingstilfelle(
    latestOppfolgingsTilfelle: KafkaOppfolgingstilfelle,
) = Oppfolgingstilfelle(
    uuid = UUID.fromString(this.uuid),
    personident = Personident(this.personIdentNumber),
    tilfelleGenerert = this.createdAt,
    tilfelleBitReferanseUuid = UUID.fromString(this.referanseTilfelleBitUuid),
    tilfelleStart = latestOppfolgingsTilfelle.start,
    tilfelleEnd = latestOppfolgingsTilfelle.end,
    antallSykedager = latestOppfolgingsTilfelle.antallSykedager,
    dodsdato = this.dodsdato,
    isArbeidstakerAtTilfelleEnd = latestOppfolgingsTilfelle.arbeidstakerAtTilfelleEnd,
    virksomhetsnummerList = latestOppfolgingsTilfelle.virksomhetsnummerList,
)

private fun isNotTilfelleInTheFuture(tilfelle: KafkaOppfolgingstilfelle): Boolean {
    return tilfelle.start.isBefore(tomorrow())
}
