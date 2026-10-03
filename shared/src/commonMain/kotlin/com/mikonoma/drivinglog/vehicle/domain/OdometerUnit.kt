package com.mikonoma.drivinglog.vehicle.domain

import kotlinx.serialization.Serializable

/**
 * How a vehicle's odometer counts. [code] is what gets stored, so it must never be a translated label
 * and must never change once released.
 */
@Serializable
enum class OdometerUnit(val code: String, val isMiles: Boolean, val hasTenths: Boolean) {
    KILOMETERS("KILOMETERS", isMiles = false, hasTenths = false),
    KILOMETERS_TENTHS("KILOMETERS_TENTHS", isMiles = false, hasTenths = true),
    MILES("MILES", isMiles = true, hasTenths = false),
    MILES_TENTHS("MILES_TENTHS", isMiles = true, hasTenths = true),
    ;

    /** Largest entry, counted in this unit's steps (whole units, or tenths): 7 whole digits. */
    val maxSteps: Long get() = if (hasTenths) 99_999_999L else 9_999_999L

    /** The shown suffix. */
    val abbreviation: String get() = if (isMiles) "mi" else "km"

    /** Converts an entry counted in this unit's steps to whole meters, using integer arithmetic only. */
    fun stepsToMeters(steps: Long): Long = when (this) {
        KILOMETERS -> steps * METERS_PER_KILOMETER
        KILOMETERS_TENTHS -> steps * METERS_PER_TENTH_KILOMETER
        MILES -> (steps * METERS_PER_MILE_SCALED + MILE_SCALE / 2) / MILE_SCALE
        MILES_TENTHS -> (steps * METERS_PER_MILE_SCALED + TENTH_MILE_SCALE / 2) / TENTH_MILE_SCALE
    }

    /** Converts whole meters to this unit's steps, rounding half up. */
    fun metersToSteps(meters: Long): Long = when (this) {
        KILOMETERS -> (meters + METERS_PER_KILOMETER / 2) / METERS_PER_KILOMETER
        KILOMETERS_TENTHS -> (meters + METERS_PER_TENTH_KILOMETER / 2) / METERS_PER_TENTH_KILOMETER
        MILES -> (meters * MILE_SCALE + HALF_MILE_SCALED) / METERS_PER_MILE_SCALED
        MILES_TENTHS -> (meters * TENTH_MILE_SCALE + HALF_MILE_SCALED) / METERS_PER_MILE_SCALED
    }

    companion object {
        /** A mile is 1609.344 m, so this is meters per mile times 1000. */
        private const val METERS_PER_MILE_SCALED = 1_609_344L
        private const val HALF_MILE_SCALED = METERS_PER_MILE_SCALED / 2

        private const val METERS_PER_KILOMETER = 1_000L
        private const val METERS_PER_TENTH_KILOMETER = 100L

        /** The scale of [METERS_PER_MILE_SCALED] (x1000), and the same for tenths of a mile (x10 more). */
        private const val MILE_SCALE = 1_000L
        private const val TENTH_MILE_SCALE = 10_000L

        fun fromCode(code: String): OdometerUnit = entries.firstOrNull { it.code == code } ?: error("Unknown odometer unit code: $code")
    }
}

private val MILES_DEFAULT_REGIONS = setOf("US", "GB", "LR", "MM")

/**
 * The unit preselected for a new vehicle. Stands in for the user-level unit preference until that exists:
 * miles in the regions that use them on the road, kilometers everywhere else (or when the region is unknown).
 */
fun defaultOdometerUnit(regionCode: String?): OdometerUnit =
    if (regionCode != null && regionCode.uppercase() in MILES_DEFAULT_REGIONS) OdometerUnit.MILES else OdometerUnit.KILOMETERS
