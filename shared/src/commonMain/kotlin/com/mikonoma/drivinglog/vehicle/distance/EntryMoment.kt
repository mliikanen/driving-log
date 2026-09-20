package com.mikonoma.drivinglog.vehicle.distance

import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * The form holds a wall-clock date and time and a zone id; the instant is derived from them. A wall-clock time that does not
 * exist (a daylight saving gap) is resolved forward and one that occurs twice (an overlap) takes the earlier offset, as
 * kotlinx-datetime does; the stored offset records what was chosen.
 */
fun momentOf(local: LocalDateTime, zoneId: String): ZonedMoment {
    val zone = TimeZone.of(zoneId)
    return ZonedMoment.of(local.toInstant(zone), zone)
}

/** The wall-clock time [now] shows in [zone], to the minute: the default of a form opened at [now]. */
fun openedAt(now: Instant, zone: TimeZone): LocalDateTime {
    val t = now.toLocalDateTime(zone)
    return LocalDateTime(t.date, LocalTime(t.hour, t.minute))
}

/** The date a Material date picker reports (midnight UTC of the chosen day, in milliseconds) as a date. */
fun dateFromPicker(utcMidnightMillis: Long): LocalDate =
    Instant.fromEpochMilliseconds(utcMidnightMillis).toLocalDateTime(TimeZone.UTC).date

/** A date as the midnight UTC milliseconds the Material date picker takes as its selection. */
fun dateToPicker(date: LocalDate): Long = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

/** Replaces the date and the time of a wall-clock value with what the pickers report. */
fun withDate(local: LocalDateTime, date: LocalDate): LocalDateTime = LocalDateTime(date, local.time)

fun withTime(local: LocalDateTime, hour: Int, minute: Int): LocalDateTime =
    LocalDateTime(local.date, LocalTime(hour, minute))
