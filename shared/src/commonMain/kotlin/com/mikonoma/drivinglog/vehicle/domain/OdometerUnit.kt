package com.mikonoma.drivinglog.vehicle.domain

import kotlinx.serialization.Serializable

/**
 * How a vehicle's odometer counts. [code] is what gets stored, so it must never be a translated label
 * and must never change once released.
 */
@Serializable
enum class OdometerUnit(
    val code: String,
    val isMiles: Boolean,
    val hasTenths: Boolean,
) {
    KILOMETERS("KILOMETERS", isMiles = false, hasTenths = false),
    KILOMETERS_TENTHS("KILOMETERS_TENTHS", isMiles = false, hasTenths = true),
    MILES("MILES", isMiles = true, hasTenths = false),
    MILES_TENTHS("MILES_TENTHS", isMiles = true, hasTenths = true);

    /** Largest entry, counted in this unit's steps (whole units, or tenths): 7 whole digits. */
    val maxSteps: Long get() = if (hasTenths) 99_999_999L else 9_999_999L

    /** The shown suffix. */
    val abbreviation: String get() = if (isMiles) "mi" else "km"

    /** Converts an entry counted in this unit's steps to whole meters, using integer arithmetic only. */
    fun stepsToMeters(steps: Long): Long = when (this) {
        KILOMETERS -> steps * 1_000
        KILOMETERS_TENTHS -> steps * 100
        MILES -> (steps * METERS_PER_MILE_SCALED + 500) / 1_000
        MILES_TENTHS -> (steps * METERS_PER_MILE_SCALED + 5_000) / 10_000
    }

    /** Converts whole meters to this unit's steps, rounding half up. */
    fun metersToSteps(meters: Long): Long = when (this) {
        KILOMETERS -> (meters + 500) / 1_000
        KILOMETERS_TENTHS -> (meters + 50) / 100
        MILES -> (meters * 1_000 + HALF_MILE_SCALED) / METERS_PER_MILE_SCALED
        MILES_TENTHS -> (meters * 10_000 + HALF_MILE_SCALED) / METERS_PER_MILE_SCALED
    }

    companion object {
        /** A mile is 1609.344 m, so this is meters per mile times 1000. */
        private const val METERS_PER_MILE_SCALED = 1_609_344L
        private const val HALF_MILE_SCALED = METERS_PER_MILE_SCALED / 2

        fun fromCode(code: String): OdometerUnit =
            entries.firstOrNull { it.code == code } ?: error("Unknown odometer unit code: $code")
    }
}

private val MILES_DEFAULT_REGIONS = setOf("US", "GB", "LR", "MM")

/**
 * The unit preselected for a new vehicle. Stands in for the user-level unit preference until that exists:
 * miles in the regions that use them on the road, kilometers everywhere else (or when the region is unknown).
 */
fun defaultOdometerUnit(regionCode: String?): OdometerUnit =
    if (regionCode != null && regionCode.uppercase() in MILES_DEFAULT_REGIONS) OdometerUnit.MILES else OdometerUnit.KILOMETERS
