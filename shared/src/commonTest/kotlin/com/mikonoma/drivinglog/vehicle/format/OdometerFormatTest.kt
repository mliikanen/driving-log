package com.mikonoma.drivinglog.vehicle.format

import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class OdometerFormatTest {

    private val us = NumberSymbols.ENGLISH_US
    private val fi = NumberSymbols.FINNISH

    private fun km(meters: Long, unit: OdometerUnit, symbols: NumberSymbols) =
        formatOdometer(Distance(meters), unit, symbols)

    @Test
    fun englishKilometers() {
        assertEquals("45,200 km", km(45_200_000, OdometerUnit.KILOMETERS, us))
        assertEquals("0 km", km(0, OdometerUnit.KILOMETERS, us))
    }

    @Test
    fun englishKilometersWithTenths() {
        assertEquals("45,200.3 km", km(45_200_300, OdometerUnit.KILOMETERS_TENTHS, us))
        assertEquals("0.0 km", km(0, OdometerUnit.KILOMETERS_TENTHS, us))
    }

    @Test
    fun englishMiles() {
        assertEquals("45,200 mi", km(OdometerUnit.MILES.stepsToMeters(45_200), OdometerUnit.MILES, us))
    }

    @Test
    fun englishMilesWithTenths() {
        assertEquals("45,200.3 mi", km(OdometerUnit.MILES_TENTHS.stepsToMeters(452_003), OdometerUnit.MILES_TENTHS, us))
    }

    @Test
    fun finnishUsesACommaAndANoBreakSpace() {
        assertEquals("45 200,3 km", km(45_200_300, OdometerUnit.KILOMETERS_TENTHS, fi))
        assertEquals("0,0 km", km(0, OdometerUnit.KILOMETERS_TENTHS, fi))
        assertEquals("0,0 mi", km(0, OdometerUnit.MILES_TENTHS, fi))
    }

    @Test
    fun wholeUnitsRoundHalfUp() {
        assertEquals("124 km", km(123_500, OdometerUnit.KILOMETERS, us))
        assertEquals("123 km", km(123_499, OdometerUnit.KILOMETERS, us))
    }

    @Test
    fun tenthsRoundToTheNearestTenth() {
        assertEquals("123.5 km", km(123_450, OdometerUnit.KILOMETERS_TENTHS, us))
        assertEquals("123.4 km", km(123_449, OdometerUnit.KILOMETERS_TENTHS, us))
    }

    @Test
    fun smallNumbersHaveNoGrouping() {
        assertEquals("999 km", km(999_000, OdometerUnit.KILOMETERS, us))
        assertEquals("1,000 km", km(1_000_000, OdometerUnit.KILOMETERS, us))
        assertEquals("9,999,999.9 km", km(9_999_999_900, OdometerUnit.KILOMETERS_TENTHS, us))
    }

    @Test
    fun theSameMetersReadDifferentlyPerLocale() {
        val meters = 123_500L
        assertEquals("123.5 km", km(meters, OdometerUnit.KILOMETERS_TENTHS, us))
        assertEquals("123,5 km", km(meters, OdometerUnit.KILOMETERS_TENTHS, fi))
    }

    @Test
    fun stepsFormatWithoutASuffix() {
        assertEquals("12.3", formatSteps(123, hasTenths = true, symbols = us))
        assertEquals("12,3", formatSteps(123, hasTenths = true, symbols = fi))
        assertEquals("123", formatSteps(123, hasTenths = false, symbols = us))
    }

    @Test
    fun dateTimeIsFixedFormatInTheGivenZone() {
        val instant = Instant.fromEpochMilliseconds(1_768_912_800_000) // 2026-01-20T12:40:00Z
        assertEquals("2026-01-20 12:40", formatDateTime(instant, TimeZone.UTC))
        assertEquals("2026-01-20 14:40", formatDateTime(instant, TimeZone.of("Europe/Helsinki")))
    }
}
