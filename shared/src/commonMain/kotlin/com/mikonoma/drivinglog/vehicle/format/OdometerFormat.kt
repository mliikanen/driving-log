package com.mikonoma.drivinglog.vehicle.format

import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.locale.TimeFormat
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

/** Groups the digits of [value] in threes with [separator]. */
private fun group(value: Long, separator: String): String {
    val digits = value.toString()
    val first = digits.length % 3
    val parts = buildList {
        if (first > 0) add(digits.substring(0, first))
        for (i in first until digits.length step 3) add(digits.substring(i, i + 3))
    }
    return parts.joinToString(separator)
}

/**
 * Renders a value counted in steps, using the locale's separators. With tenths the last digit follows the
 * decimal separator and is always shown ("0.0", "12.3"). Built from integers, so nothing is rounded twice.
 */
fun formatSteps(steps: Long, hasTenths: Boolean, symbols: NumberSymbols): String =
    if (hasTenths) {
        group(steps / 10, symbols.groupingSeparator) + symbols.decimalSeparator + (steps % 10)
    } else {
        group(steps, symbols.groupingSeparator)
    }

/** A stored distance in the vehicle's unit, rounded half up to the unit's step, with the unit suffix. */
fun formatOdometer(distance: Distance, unit: OdometerUnit, symbols: NumberSymbols): String =
    formatSteps(unit.metersToSteps(distance.meters), unit.hasTenths, symbols) + " " + unit.abbreviation

private fun two(n: Int) = n.toString().padStart(2, '0')

/**
 * The time of day the way the system writes it: 24-hour `HH:mm`, or 12-hour `h:mm` and the device's AM or PM marker
 * (midnight is 12:00 AM and noon 12:00 PM).
 */
fun formatTimeOfDay(hour: Int, minute: Int, format: TimeFormat = TimeFormat()): String =
    if (format.is24Hour) {
        "${two(hour)}:${two(minute)}"
    } else {
        val hour12 = if (hour % 12 == 0) 12 else hour % 12
        "$hour12:${two(minute)} ${if (hour < 12) format.amMarker else format.pmMarker}"
    }

/** A date as yyyy-MM-dd, which is the same in every locale, followed by the time of day in [format]. */
fun formatLocalDateTime(t: LocalDateTime, format: TimeFormat = TimeFormat()): String =
    "${t.year}-${two(t.month.number)}-${two(t.day)} ${formatTimeOfDay(t.hour, t.minute, format)}"

/** An instant as a date and time in [zone]. */
fun formatDateTime(instant: Instant, zone: TimeZone = TimeZone.currentSystemDefault(), format: TimeFormat = TimeFormat()): String =
    formatLocalDateTime(instant.toLocalDateTime(zone), format)

/**
 * A moment as it was entered: the wall-clock time in the zone it was entered in (the stored offset applied, so it reads the
 * same whatever the device's zone is now), with that zone's id appended when it is not the device's current zone. A moment
 * from before zones were stored is shown in the device zone, with no zone id.
 */
fun formatMoment(moment: ZonedMoment, deviceZone: TimeZone, timeFormat: TimeFormat = TimeFormat()): String {
    val zone = moment.zone ?: return formatDateTime(moment.instant, deviceZone, timeFormat)
    val text = formatLocalDateTime(checkNotNull(moment.localDateTime), timeFormat)
    return if (zone.id == deviceZone.id) text else "$text (${zone.id})"
}
