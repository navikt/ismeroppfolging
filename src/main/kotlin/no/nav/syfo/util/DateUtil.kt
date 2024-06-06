package no.nav.syfo.util

import java.time.*

val defaultZoneOffset: ZoneOffset = ZoneOffset.UTC
val osloTimeZone: ZoneId = ZoneId.of("Europe/Oslo")

fun nowUTC(): OffsetDateTime = OffsetDateTime.now(defaultZoneOffset)

fun LocalDateTime.toOffsetDateTimeUTC(): OffsetDateTime = this.atZone(osloTimeZone).withZoneSameInstant(defaultZoneOffset).toOffsetDateTime()
