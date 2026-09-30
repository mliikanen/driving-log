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

    /**
     * A refueling (`add-refueling-logging`): a fuel amount, a fuel type, whether the tank was filled up, and,
     * optionally, a mileage reading — the same "Trip distance"/"New odometer" choice [DistanceEntry]/[OdometerAnchor]
     * make, but never required. [mileage] is null when the refueling was saved with no mileage, in which case it
     * neither sets nor adds to the odometer. Every field but [mileage] is fixed once saved: the details screen's
     * "Edit" action changes only [note] and [photoIds], the same as it does for [DistanceEntry]/[OdometerAnchor].
     */
    data class Refueling(
        override val id: String,
        override val occurredAt: ZonedMoment,
        val amount: Volume,
        /** The unit [amount] was entered in: shown in that unit always, never converted — unlike a vehicle's own
         * odometer unit, this is a per-fill-up recording choice, not a fixed vehicle characteristic. */
        val unit: FuelUnit,
        val fuelType: FuelType,
        val filledUp: Boolean,
        val mileage: RefuelingMileage? = null,
        override val note: String? = null,
        override val photoIds: List<String> = emptyList(),
    ) : VehicleEvent {
        override val odometer: Distance? get() = (mileage as? RefuelingMileage.Anchor)?.reading
    }
}

/**
 * A refueling's optional mileage (`add-refueling-logging`): structurally the same two shapes
 * [VehicleEvent.DistanceEntry] and [VehicleEvent.OdometerAnchor] already are, since a refueling's mileage, when
 * given, is validated and saved exactly like theirs (`distance-logging`'s existing requirements, reused unchanged).
 */
sealed interface RefuelingMileage {
    /** Added to the odometer, like a [VehicleEvent.DistanceEntry]. [loggedOdometer] is the count typed, if logged by
     * odometer — for the row only, never used to derive an odometer. */
    data class Added(val distance: Distance, val loggedOdometer: Distance? = null) : RefuelingMileage

    /** Sets the odometer, like an [VehicleEvent.OdometerAnchor]. */
    data class Anchor(val reading: Distance) : RefuelingMileage
}
