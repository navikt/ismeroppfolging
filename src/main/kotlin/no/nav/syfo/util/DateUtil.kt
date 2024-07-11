package no.nav.syfo.util

import java.time.*
import java.time.temporal.ChronoUnit

val defaultZoneOffset: ZoneOffset = ZoneOffset.UTC
val osloTimeZone: ZoneId = ZoneId.of("Europe/Oslo")

fun nowUTC(): OffsetDateTime = OffsetDateTime.now(defaultZoneOffset)

fun LocalDateTime.toOffsetDateTimeUTC(): OffsetDateTime = this.atZone(osloTimeZone).withZoneSameInstant(defaultZoneOffset).toOffsetDateTime()

fun OffsetDateTime.millisekundOpplosning(): OffsetDateTime = this.truncatedTo(ChronoUnit.MILLIS)
