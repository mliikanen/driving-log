package com.mikonoma.drivinglog.locale

/** The separators a locale draws numbers with. Digits are always 0 to 9. */
data class NumberSymbols(val decimalSeparator: String, val groupingSeparator: String) {
    companion object {
        val ENGLISH_US = NumberSymbols(decimalSeparator = ".", groupingSeparator = ",")

        /** Finnish groups with a no-break space. */
        val FINNISH = NumberSymbols(decimalSeparator = ",", groupingSeparator = " ")
    }
}

/**
 * What the app needs to know about the device's current locale. Implementations read the locale on every
 * call so a change of locale is picked up without restarting.
 */
interface DeviceLocale {
    /** The ISO 3166 region code such as "US", or null when unknown. */
    val regionCode: String?

    fun numberSymbols(): NumberSymbols
}

/** The device's real locale. */
expect class SystemDeviceLocale() : DeviceLocale
