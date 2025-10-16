package no.nav.syfo.shared.util

import java.time.*
import java.time.temporal.ChronoUnit

val defaultZoneOffset: ZoneOffset = ZoneOffset.UTC
val osloTimeZone: ZoneId = ZoneId.of("Europe/Oslo")

const val DAYS_IN_WEEK = 7

fun nowUTC(): OffsetDateTime = OffsetDateTime.now(defaultZoneOffset)

fun OffsetDateTime.toLocalDateOslo(): LocalDate = this.atZoneSameInstant(osloTimeZone).toLocalDate()

fun OffsetDateTime.toLocalDateTimeOslo(): LocalDateTime = this.atZoneSameInstant(osloTimeZone).toLocalDateTime()

fun LocalDateTime.toOffsetDateTimeUTC(): OffsetDateTime = this.atZone(osloTimeZone).withZoneSameInstant(defaultZoneOffset).toOffsetDateTime()

fun OffsetDateTime.millisekundOpplosning(): OffsetDateTime = this.truncatedTo(ChronoUnit.MILLIS)

infix fun OffsetDateTime.isMoreThanDaysAgo(days: Long): Boolean = this.isBefore(OffsetDateTime.now().minusDays(days))

infix fun LocalDate.isMoreThanDaysAgo(days: Long): Boolean = this.isBefore(LocalDate.now().minusDays(days))

fun tomorrow(): LocalDate = LocalDate.now().plusDays(1)

infix fun LocalDate.isAfterOrEqual(other: LocalDate): Boolean = this.isAfter(other) || this == other

infix fun LocalDate.isBeforeOrEqual(other: LocalDate): Boolean = this.isBefore(other) || this == other
