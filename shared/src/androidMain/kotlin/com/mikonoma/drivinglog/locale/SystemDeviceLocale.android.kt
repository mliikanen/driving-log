package com.mikonoma.drivinglog.locale

import java.text.DateFormatSymbols
import java.text.DecimalFormatSymbols
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

/**
 * The Android device's locale. The system's 24-hour setting needs the application context, so the application supplies it
 * as [is24Hour] (for example `DateFormat.is24HourFormat(context)`), read on every call.
 */
class SystemDeviceLocale(private val is24Hour: () -> Boolean) : DeviceLocale {
    override val regionCode: String?
        get() = Locale.getDefault().country.ifEmpty { null }

    override fun numberSymbols(): NumberSymbols {
        val symbols = DecimalFormatSymbols.getInstance(Locale.getDefault())
        return NumberSymbols(symbols.decimalSeparator.toString(), symbols.groupingSeparator.toString())
    }

    override fun weekdayName(day: DayOfWeek): String =
        java.time.DayOfWeek.of(day.isoDayNumber).getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())

    override fun timeFormat(): TimeFormat {
        val markers = DateFormatSymbols.getInstance(Locale.getDefault()).amPmStrings
        return TimeFormat(is24Hour = is24Hour(), amMarker = markers[0], pmMarker = markers[1])
    }
}
