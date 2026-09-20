package com.mikonoma.drivinglog.locale

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SystemDeviceLocaleTest {

    private val original = Locale.getDefault()

    @AfterTest
    fun restoreLocale() = Locale.setDefault(original)

    @Test
    fun followsEnglishUnitedStates() {
        Locale.setDefault(Locale.forLanguageTag("en-US"))
        val locale = SystemDeviceLocale()
        assertEquals("US", locale.regionCode)
        assertEquals(NumberSymbols(".", ","), locale.numberSymbols())
    }

    @Test
    fun followsFinnish() {
        Locale.setDefault(Locale.forLanguageTag("fi-FI"))
        val locale = SystemDeviceLocale()
        assertEquals("FI", locale.regionCode)
        assertEquals(",", locale.numberSymbols().decimalSeparator)
        assertEquals(" ", locale.numberSymbols().groupingSeparator)
    }

    @Test
    fun picksUpALocaleChangeWithoutANewInstance() {
        val locale = SystemDeviceLocale()
        Locale.setDefault(Locale.forLanguageTag("en-US"))
        assertEquals(".", locale.numberSymbols().decimalSeparator)
        Locale.setDefault(Locale.forLanguageTag("fi-FI"))
        assertEquals(",", locale.numberSymbols().decimalSeparator)
    }

    @Test
    fun regionIsNullWhenTheLocaleHasNone() {
        Locale.setDefault(Locale.forLanguageTag("fi"))
        assertNull(SystemDeviceLocale().regionCode)
    }
}
