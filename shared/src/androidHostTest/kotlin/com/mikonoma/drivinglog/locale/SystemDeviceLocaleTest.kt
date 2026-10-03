package com.mikonoma.drivinglog.locale

import kotlinx.datetime.DayOfWeek
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
        val locale = SystemDeviceLocale { true }
        assertEquals("US", locale.regionCode)
        assertEquals(NumberSymbols(".", ","), locale.numberSymbols())
    }

    @Test
    fun followsFinnish() {
        Locale.setDefault(Locale.forLanguageTag("fi-FI"))
        val locale = SystemDeviceLocale { true }
        assertEquals("FI", locale.regionCode)
        assertEquals(",", locale.numberSymbols().decimalSeparator)
        assertEquals(" ", locale.numberSymbols().groupingSeparator)
    }

    @Test
    fun picksUpALocaleChangeWithoutANewInstance() {
        val locale = SystemDeviceLocale { true }
        Locale.setDefault(Locale.forLanguageTag("en-US"))
        assertEquals(".", locale.numberSymbols().decimalSeparator)
        Locale.setDefault(Locale.forLanguageTag("fi-FI"))
        assertEquals(",", locale.numberSymbols().decimalSeparator)
    }

    @Test
    fun regionIsNullWhenTheLocaleHasNone() {
        Locale.setDefault(Locale.forLanguageTag("fi"))
        assertNull(SystemDeviceLocale { true }.regionCode)
    }

    @Test
    fun weekdayNamesFollowTheDeviceLanguage() {
        Locale.setDefault(Locale.forLanguageTag("en-US"))
        val english = SystemDeviceLocale { true }
        assertEquals("Sunday", english.weekdayName(DayOfWeek.SUNDAY))
        assertEquals("Monday", english.weekdayName(DayOfWeek.MONDAY))
        assertEquals("Saturday", english.weekdayName(DayOfWeek.SATURDAY))

        Locale.setDefault(Locale.forLanguageTag("fi-FI"))
        val finnish = SystemDeviceLocale { true }
        assertEquals("sunnuntai", finnish.weekdayName(DayOfWeek.SUNDAY))
        assertEquals("maanantai", finnish.weekdayName(DayOfWeek.MONDAY))
    }

    @Test
    fun everyDayOfTheWeekHasADistinctName() {
        Locale.setDefault(Locale.forLanguageTag("en-US"))
        val names = DayOfWeek.entries.map { SystemDeviceLocale { true }.weekdayName(it) }
        assertEquals(7, names.toSet().size)
    }

    @Test
    fun theTimeFormatFollowsTheSuppliedSystemSettingOnEveryCall() {
        Locale.setDefault(Locale.forLanguageTag("en-US"))
        var use24 = true
        val locale = SystemDeviceLocale { use24 }

        assertEquals(true, locale.timeFormat().is24Hour)
        use24 = false
        assertEquals(false, locale.timeFormat().is24Hour)
    }

    @Test
    fun theAmAndPmMarkersFollowTheDeviceLanguage() {
        Locale.setDefault(Locale.forLanguageTag("en-US"))
        assertEquals("AM", SystemDeviceLocale { false }.timeFormat().amMarker)
        assertEquals("PM", SystemDeviceLocale { false }.timeFormat().pmMarker)

        Locale.setDefault(Locale.forLanguageTag("fi-FI"))
        val finnish = SystemDeviceLocale { false }.timeFormat()
        assertEquals("ap.", finnish.amMarker)
        assertEquals("ip.", finnish.pmMarker)
    }
}
