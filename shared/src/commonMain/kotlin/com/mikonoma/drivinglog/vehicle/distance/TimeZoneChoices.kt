package com.mikonoma.drivinglog.vehicle.distance

import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlin.math.abs
import kotlin.time.Instant

/** A zone the user can pick: its IANA id and its UTC offset at the moment the list was built for. */
data class ZoneChoice(val id: String, val offsetSeconds: Int)

private val CONTINENTS = listOf(
    "Africa", "America", "Antarctica", "Arctic", "Asia", "Atlantic", "Australia", "Europe", "Indian", "Pacific",
)

/** `Continent/City` (or `Continent/Region/City`), the readable names; UTC is added separately. */
private fun isReadableZoneId(id: String) = CONTINENTS.any { id.startsWith("$it/") }

/**
 * The zones to offer. [availableIds] is the raw list a platform knows (kotlinx-datetime's `TimeZone.availableZoneIds`), which
 * also holds legacy aliases and abbreviations (`Brazil/East`, `CET`, `Canada/Eastern`, `Etc/...`); only `Continent/City` names
 * and `UTC` are offered, plus the device's own zone whatever it looks like. Sorted by id, with the device zone first when
 * [query] is blank. [query] filters by a case-insensitive substring of the id, where a space matches an underscore.
 */
fun timeZoneChoices(availableIds: Collection<String>, deviceZoneId: String, at: Instant, query: String = ""): List<ZoneChoice> {
    val ids = (availableIds.filter { isReadableZoneId(it) || it == "UTC" } + deviceZoneId).distinct().sorted()
    val needle = query.trim().replace(' ', '_').lowercase()
    val matching = ids.filter { needle.isEmpty() || it.lowercase().contains(needle) }
    val ordered = if (needle.isEmpty() && deviceZoneId in matching) listOf(deviceZoneId) + (matching - deviceZoneId) else matching
    return ordered.mapNotNull { id ->
        // An id the platform cannot resolve is left out rather than crashing the list.
        runCatching { ZoneChoice(id, TimeZone.of(id).offsetAt(at).totalSeconds) }.getOrNull()
    }
}

/** An offset as "UTC+03:00", "UTC-04:00", "UTC+05:30" or "UTC". */
fun formatUtcOffset(offsetSeconds: Int): String {
    if (offsetSeconds == 0) return "UTC"
    val sign = if (offsetSeconds < 0) '-' else '+'
    val total = abs(offsetSeconds)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    return "UTC$sign${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}
