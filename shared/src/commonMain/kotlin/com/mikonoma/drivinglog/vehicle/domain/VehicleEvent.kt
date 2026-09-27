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

    /**
     * A note the user typed while logging the event (`add-event-notes`), or null when it has none. Only [DistanceEntry]
     * and [OdometerAnchor] can carry one; [InitialOdometer] always reads null here, since it is created by the add-vehicle
     * flow, a different form entirely.
     */
    val note: String?

    /**
     * The ids of photos attached to the event (`add-event-pictures`), in the order they were attached; empty when it has
     * none. Only [DistanceEntry] and [OdometerAnchor] can carry any; [InitialOdometer] always reads empty here, for the
     * same reason it never carries a [note].
     */
    val photoIds: List<String>

    /** Written once, when the vehicle is added. It sets the odometer. */
    data class InitialOdometer(
        override val id: String,
        override val occurredAt: ZonedMoment,
        val reading: Distance,
    ) : VehicleEvent {
        override val odometer: Distance get() = reading
        override val note: String? get() = null
        override val photoIds: List<String> get() = emptyList()
    }

    /**
     * An odometer reading set after the vehicle was added: a new odometer count logged for a time when no odometer was known
     * (before the initial odometer, say). Like [InitialOdometer] it sets the odometer, so it is a baseline for the odometer
     * known at later times.
     */
    data class OdometerAnchor(
        override val id: String,
        override val occurredAt: ZonedMoment,
        val reading: Distance,
        /** A note the user typed while logging it, or null. See [DistanceEntry.note]. */
        override val note: String? = null,
        /** Photos attached while logging it, or later through the "Edit" action. See [DistanceEntry.photoIds]. */
        override val photoIds: List<String> = emptyList(),
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
        /** A note the user typed while logging it (`add-event-notes`), or null when none was added. Immutable once saved. */
        override val note: String? = null,
        /** Photos attached while logging it, or later through the "Edit" action (`add-event-pictures`), in attach order. */
        override val photoIds: List<String> = emptyList(),
    ) : VehicleEvent {
        override val odometer: Distance? get() = null
    }
}
