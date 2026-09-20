package com.mikonoma.drivinglog.locale

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.NSLocaleDecimalSeparator
import platform.Foundation.NSLocaleGroupingSeparator
import platform.Foundation.currentLocale

actual class SystemDeviceLocale actual constructor() : DeviceLocale {
    override val regionCode: String?
        get() = (NSLocale.currentLocale.objectForKey(NSLocaleCountryCode) as? String)?.ifEmpty { null }

    override fun numberSymbols(): NumberSymbols {
        val locale = NSLocale.currentLocale
        return NumberSymbols(
            decimalSeparator = locale.objectForKey(NSLocaleDecimalSeparator) as? String ?: ".",
            groupingSeparator = locale.objectForKey(NSLocaleGroupingSeparator) as? String ?: ",",
        )
    }
}
