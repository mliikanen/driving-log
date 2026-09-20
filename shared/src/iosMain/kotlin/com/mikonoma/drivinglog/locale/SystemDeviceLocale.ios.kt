package com.mikonoma.drivinglog.locale

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.NSLocaleDecimalSeparator
import platform.Foundation.NSLocaleGroupingSeparator
import platform.Foundation.currentLocale

/** The iOS device's locale, calendar language and clock setting, read from Foundation on every call. */
class SystemDeviceLocale : DeviceLocale {
    override val regionCode: String?
        get() = (NSLocale.currentLocale.objectForKey(NSLocaleCountryCode) as? String)?.ifEmpty { null }

    override fun numberSymbols(): NumberSymbols {
        val locale = NSLocale.currentLocale
        return NumberSymbols(
            decimalSeparator = locale.objectForKey(NSLocaleDecimalSeparator) as? String ?: ".",
            groupingSeparator = locale.objectForKey(NSLocaleGroupingSeparator) as? String ?: ",",
        )
    }

    override fun weekdayName(day: DayOfWeek): String {
        // Foundation lists the weekday symbols starting with Sunday.
        val symbols = NSCalendar.currentCalendar.weekdaySymbols
        return symbols[day.isoDayNumber % 7] as String
    }

    override fun timeFormat(): TimeFormat {
        val formatter = NSDateFormatter().apply { locale = NSLocale.currentLocale }
        // The "j" template becomes the locale's preferred hour format, which follows the system 24-hour setting.
        val template = NSDateFormatter.dateFormatFromTemplate("j", 0u, NSLocale.currentLocale) ?: "HH"
        return TimeFormat(is24Hour = !template.contains('a'), amMarker = formatter.AMSymbol, pmMarker = formatter.PMSymbol)
    }
}
