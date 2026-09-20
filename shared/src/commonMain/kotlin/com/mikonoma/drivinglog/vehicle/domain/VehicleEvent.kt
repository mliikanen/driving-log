package com.mikonoma.drivinglog.vehicle.domain

/** An entry in a vehicle's append-only log. */
sealed interface VehicleEvent {
    val id: String

    /** When the event happened, in the time zone it was entered in. Events are ordered by its instant. */
    val occurredAt: ZonedMoment

    /**
     * The odometer reading this event sets, or null for an event that does not set the odometer. Only odometer-setting
     * events are baselines for the known odometer; a distance entry is added on top of the latest baseline.
     */
    val odometer: Distance?

    /** Written once, when the vehicle is added. It sets the odometer. */
    data class InitialOdometer(
        override val id: String,
        override val occurredAt: ZonedMoment,
        val reading: Distance,
    ) : VehicleEvent {
        override val odometer: Distance get() = reading
    }

    /**
     * A distance driven, logged as a trip distance or as a new odometer count. [distance] is always what was added to the
     * odometer. [loggedOdometer] is the count the user typed when logging by odometer, kept for the log row only: it is
     * never used to derive an odometer.
     */
    data class DistanceEntry(
        override val id: String,
        override val occurredAt: ZonedMoment,
        val distance: Distance,
        val loggedOdometer: Distance? = null,
    ) : VehicleEvent {
        override val odometer: Distance? get() = null
    }
}
