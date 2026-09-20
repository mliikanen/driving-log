package com.mikonoma.drivinglog.locale

import kotlinx.datetime.DayOfWeek

/** The separators a locale draws numbers with. Digits are always 0 to 9. */
data class NumberSymbols(val decimalSeparator: String, val groupingSeparator: String) {
    companion object {
        val ENGLISH_US = NumberSymbols(decimalSeparator = ".", groupingSeparator = ",")

        /** Finnish groups with a no-break space. */
        val FINNISH = NumberSymbols(decimalSeparator = ",", groupingSeparator = " ")
    }
}

/** How the system writes the time of day: 24-hour, or 12-hour with these AM and PM markers. */
data class TimeFormat(val is24Hour: Boolean = true, val amMarker: String = "AM", val pmMarker: String = "PM")

/**
 * What the app needs to know about the device: its locale, calendar language and clock setting. Implementations read the
 * platform on every call so a change of language or of the 12/24-hour setting is picked up without restarting. The defaults
 * (English day names, a 24-hour clock) keep test fakes small.
 */
interface DeviceLocale {
    /** The ISO 3166 region code such as "US", or null when unknown. */
    val regionCode: String?

    fun numberSymbols(): NumberSymbols

    /** The full name of the day of the week in the device's language, for example "Sunday" or "sunnuntai". */
    fun weekdayName(day: DayOfWeek): String = day.name.lowercase().replaceFirstChar { it.uppercase() }

    /** The system's 12-hour or 24-hour setting, with the device's AM and PM markers. */
    fun timeFormat(): TimeFormat = TimeFormat()
}
