package com.mikonoma.drivinglog.vehicle.domain

import kotlin.time.Instant

data class Vehicle(
    val id: String,
    val name: String,
    val licensePlate: String?,
    val odometerUnit: OdometerUnit,
    val createdAt: Instant,
    /** The "include tenths" choice last used when logging a distance for this vehicle, or null when none was saved yet. */
    val logDistanceTenths: Boolean? = null,
)

/** A vehicle with its current odometer, which is derived from the log and never stored. */
data class VehicleDetails(
    val vehicle: Vehicle,
    val currentOdometer: Distance?,
)
