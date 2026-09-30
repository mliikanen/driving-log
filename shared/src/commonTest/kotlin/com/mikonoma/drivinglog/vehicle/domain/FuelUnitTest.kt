package com.mikonoma.drivinglog.vehicle.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class FuelUnitTest {

    @Test
    fun codesRoundTrip() {
        for (unit in FuelUnit.entries) assertEquals(unit, FuelUnit.fromCode(unit.code))
    }

    @Test
    fun unknownCodeDefaultsToLiters() {
        assertEquals(FuelUnit.LITERS, FuelUnit.fromCode("SOMETHING_ELSE"))
    }

    @Test
    fun literStepsAreExact() {
        // 100 steps = 1.00 L = 1000 mL.
        assertEquals(1000, FuelUnit.LITERS.stepsToMilliliters(100))
        // 4230 steps = 42.30 L = 42,300 mL.
        assertEquals(42_300, FuelUnit.LITERS.stepsToMilliliters(4230))
    }

    @Test
    fun aUsGallonIsExact() {
        // 1 gallon = 100 steps, exactly 3,785,411,784 nanoliters -> 3785 mL rounded to the nearest whole mL (.411784 rounds down).
        assertEquals(3785, FuelUnit.GALLONS.stepsToMilliliters(100))
        assertEquals(37_854, FuelUnit.GALLONS.stepsToMilliliters(1000))
    }

    @Test
    fun literStepsRoundTrip() {
        for (steps in listOf(0L, 1L, 100L, 4230L, 999_999L)) {
            assertEquals(steps, FuelUnit.LITERS.millilitersToSteps(FuelUnit.LITERS.stepsToMilliliters(steps)), "steps=$steps")
        }
    }

    @Test
    fun gallonStepsRoundTripWithinRounding() {
        for (steps in listOf(0L, 1L, 100L, 1000L, 999_999L)) {
            val roundTripped = FuelUnit.GALLONS.millilitersToSteps(FuelUnit.GALLONS.stepsToMilliliters(steps))
            // The conversion is not perfectly invertible (3.785411784 has no exact hundredths inverse), but must
            // never drift by more than one hundredth.
            kotlin.test.assertTrue(kotlin.math.abs(roundTripped - steps) <= 1, "steps=$steps roundTripped=$roundTripped")
        }
    }

    @Test
    fun zeroConvertsToZero() {
        for (unit in FuelUnit.entries) {
            assertEquals(0, unit.stepsToMilliliters(0))
            assertEquals(0, unit.millilitersToSteps(0))
        }
    }

    @Test
    fun maximumStepsConvertWithoutOverflow() {
        for (unit in FuelUnit.entries) {
            val milliliters = unit.stepsToMilliliters(999_999L)
            assertEquals(true, milliliters > 0, unit.name)
        }
    }
}
