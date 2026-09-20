package com.mikonoma.drivinglog.vehicle.domain

import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    /** All vehicles, in no particular order. */
    fun observeVehicles(): Flow<List<Vehicle>>

    /** The vehicle with its current odometer, or null when it does not exist. */
    fun observeVehicle(id: String): Flow<VehicleDetails?>

    /** At most [limit] events of the vehicle, newest first by instant (ties: the one added last comes first). */
    fun observeRecentEvents(vehicleId: String, limit: Int): Flow<List<VehicleEvent>>

    /** Every event of the vehicle, newest first by instant (ties: the one added last comes first). */
    fun observeLog(vehicleId: String): Flow<List<VehicleEvent>>

    /** Saves the vehicle and its initial odometer event together, or neither. Returns the new vehicle id. */
    suspend fun addVehicle(name: String, licensePlate: String?, unit: OdometerUnit, initialOdometer: Distance): String

    /**
     * Adds one distance entry to the log at [occurredAt] (in the zone it was entered in). [distance] must be above zero.
     * [loggedOdometer] is the count the user typed when logging by odometer, for the log row only. Returns the new event id.
     */
    suspend fun addDistanceEntry(vehicleId: String, occurredAt: ZonedMoment, distance: Distance, loggedOdometer: Distance?): String

    /** Changes only the name and plate. The log and the unit are never touched. */
    suspend fun updateVehicle(id: String, name: String, licensePlate: String?)
}
