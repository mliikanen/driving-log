package com.mikonoma.drivinglog.vehicle.domain

import kotlin.time.Instant

/** An entry in a vehicle's append-only log. */
sealed interface VehicleEvent {
    val id: String
    val occurredAt: Instant

    /** The odometer reading, when this kind of event carries one. */
    val odometer: Distance?

    /** Written once, when the vehicle is added. */
    data class InitialOdometer(
        override val id: String,
        override val occurredAt: Instant,
        val reading: Distance,
    ) : VehicleEvent {
        override val odometer: Distance get() = reading
    }
}
