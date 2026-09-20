package com.mikonoma.drivinglog.locale

import kotlinx.datetime.DayOfWeek

/** A device locale tests can set: region, number symbols, 12/24-hour setting and weekday names. */
class FakeDeviceLocale(
    override val regionCode: String? = "FI",
    var symbols: NumberSymbols = NumberSymbols.ENGLISH_US,
    var time: TimeFormat = TimeFormat(),
    var weekdays: Map<DayOfWeek, String> = emptyMap(),
) : DeviceLocale {
    override fun numberSymbols(): NumberSymbols = symbols
    override fun timeFormat(): TimeFormat = time
    override fun weekdayName(day: DayOfWeek): String = weekdays[day] ?: super.weekdayName(day)
}
