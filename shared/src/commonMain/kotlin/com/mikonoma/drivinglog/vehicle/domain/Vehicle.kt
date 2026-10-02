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
    /** The id naming the vehicle's picture files, or null when it has no picture. */
    val pictureId: String? = null,
    /** The kind of vehicle. Every vehicle has one; a stored code this app does not know reads as [VehicleType.OTHER]. */
    val type: VehicleType,
    /** The vehicle's color. Every vehicle has one (the default is the application's main theme color). */
    val color: Rgb,
    /** The vehicle's own coarse fuel/engine category (`vehicle-fuel-type`). Every vehicle has one; a stored code
     * this app does not know, or a vehicle from before fuel types existed, reads as [VehicleFuelType.PETROL]. */
    val fuelType: VehicleFuelType = VehicleFuelType.PETROL,
)

/** A vehicle with its current odometer, which is derived from the log and never stored. */
data class VehicleDetails(
    val vehicle: Vehicle,
    val currentOdometer: Distance?,
)
