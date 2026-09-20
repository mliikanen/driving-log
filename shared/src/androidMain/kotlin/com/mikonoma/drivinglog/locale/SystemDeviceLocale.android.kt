package com.mikonoma.drivinglog.locale

import java.text.DecimalFormatSymbols
import java.util.Locale

actual class SystemDeviceLocale actual constructor() : DeviceLocale {
    override val regionCode: String?
        get() = Locale.getDefault().country.ifEmpty { null }

    override fun numberSymbols(): NumberSymbols {
        val symbols = DecimalFormatSymbols.getInstance(Locale.getDefault())
        return NumberSymbols(symbols.decimalSeparator.toString(), symbols.groupingSeparator.toString())
    }
}
