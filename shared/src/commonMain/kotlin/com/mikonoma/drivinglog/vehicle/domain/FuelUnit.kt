package com.mikonoma.drivinglog.vehicle.domain

import kotlinx.serialization.Serializable

/**
 * The unit a refueling's fuel amount is entered and shown in: liters or gallons. [code] is what is stored and never
 * changes once released. Unlike a vehicle's odometer unit, this is never per-vehicle: it is a single, global
 * last-used preference (`add-refueling-logging`, "The fuel amount's unit remembers the last choice, independent of
 * the vehicle").
 */
@Serializable
enum class FuelUnit(val code: String, val abbreviation: String) {
    LITERS("LITERS", "L"),
    GALLONS("GALLONS", "gal"),
    ;

    /** Converts an amount counted in this unit's hundredths (its entry field's steps) to whole milliliters. */
    fun stepsToMilliliters(steps: Long): Long = when (this) {
        LITERS -> steps * ML_PER_LITER_STEP
        GALLONS -> (steps * ML_PER_GALLON_SCALED + HALF_STEPS_DENOM) / STEPS_DENOM
    }

    /** Converts whole milliliters to this unit's hundredths, rounding half up. */
    fun millilitersToSteps(milliliters: Long): Long = when (this) {
        LITERS -> (milliliters + ML_PER_LITER_STEP / 2) / ML_PER_LITER_STEP
        GALLONS -> (milliliters * STEPS_DENOM + ML_PER_GALLON_SCALED / 2) / ML_PER_GALLON_SCALED
    }

    companion object {

        // A liter step is a hundredth of a liter: 10 mL.
        private const val ML_PER_LITER_STEP = 10L

        // A US gallon is exactly 3.785411784 liters = 3,785,411,784 mL scaled by 1,000,000 - an exact integer, no rounding.
        private const val ML_PER_GALLON_SCALED = 3_785_411_784L

        // Steps are hundredths of a unit (x100); the gallon constant above is scaled by 1,000,000, so converting
        // steps <-> milliliters divides/multiplies by both scales together: 100 * 1,000,000.
        private const val STEPS_DENOM = 100_000_000L
        private const val HALF_STEPS_DENOM = STEPS_DENOM / 2

        fun fromCode(code: String): FuelUnit = entries.firstOrNull { it.code == code } ?: LITERS
    }
}
