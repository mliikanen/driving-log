package com.mikonoma.drivinglog.vehicle.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class OdometerUnitTest {

    @Test
    fun milesAreTheDefaultInMileRegions() {
        for (region in listOf("US", "GB", "LR", "MM", "us")) {
            assertEquals(OdometerUnit.MILES, defaultOdometerUnit(region), region)
        }
    }

    @Test
    fun kilometersAreTheDefaultElsewhere() {
        for (region in listOf("FI", "DE", "FR", "JP", "")) {
            assertEquals(OdometerUnit.KILOMETERS, defaultOdometerUnit(region), region)
        }
    }

    @Test
    fun kilometersAreTheDefaultForUnknownRegion() {
        assertEquals(OdometerUnit.KILOMETERS, defaultOdometerUnit(null))
    }

    @Test
    fun codesRoundTrip() {
        for (unit in OdometerUnit.entries) assertEquals(unit, OdometerUnit.fromCode(unit.code))
    }

    @Test
    fun kilometerConversionsAreExact() {
        assertEquals(45_200_000, OdometerUnit.KILOMETERS.stepsToMeters(45_200))
        assertEquals(45_200_300, OdometerUnit.KILOMETERS_TENTHS.stepsToMeters(452_003))
    }

    @Test
    fun mileConversionsRoundToNearestMeter() {
        assertEquals(1_609, OdometerUnit.MILES.stepsToMeters(1))
        assertEquals(161, OdometerUnit.MILES_TENTHS.stepsToMeters(1))
        assertEquals(16_093, OdometerUnit.MILES.stepsToMeters(10))
    }

    @Test
    fun wholeMilesRoundTrip() {
        for (miles in (0L..20_000L) + 9_999_999L) {
            val meters = OdometerUnit.MILES.stepsToMeters(miles)
            assertEquals(miles, OdometerUnit.MILES.metersToSteps(meters), "miles=$miles")
        }
    }

    @Test
    fun tenthsOfMilesRoundTrip() {
        for (tenths in (0L..20_000L) + 99_999_999L) {
            val meters = OdometerUnit.MILES_TENTHS.stepsToMeters(tenths)
            assertEquals(tenths, OdometerUnit.MILES_TENTHS.metersToSteps(meters), "tenths=$tenths")
        }
    }

    @Test
    fun kilometerStepsRoundTrip() {
        for (steps in (0L..20_000L) + 9_999_999L) {
            assertEquals(steps, OdometerUnit.KILOMETERS.metersToSteps(OdometerUnit.KILOMETERS.stepsToMeters(steps)))
        }
        for (steps in (0L..20_000L) + 99_999_999L) {
            assertEquals(steps, OdometerUnit.KILOMETERS_TENTHS.metersToSteps(OdometerUnit.KILOMETERS_TENTHS.stepsToMeters(steps)))
        }
    }

    @Test
    fun wholeUnitsRoundHalfUpWhenShown() {
        assertEquals(124, OdometerUnit.KILOMETERS.metersToSteps(123_500))
        assertEquals(123, OdometerUnit.KILOMETERS.metersToSteps(123_499))
    }

    @Test
    fun maximumEntriesConvertWithoutOverflow() {
        for (unit in OdometerUnit.entries) {
            val meters = unit.stepsToMeters(unit.maxSteps)
            assertEquals(true, meters > 0, unit.name)
        }
    }
}
