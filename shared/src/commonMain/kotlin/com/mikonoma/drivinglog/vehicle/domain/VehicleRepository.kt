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

    /**
     * The id of the vehicle last logged for: whichever of [addDistanceEntry] or [addOdometerAnchor] was called last, for any vehicle, on any route
     * (the details screen or the Home screen). Null when nothing has been logged since this memory existed (a new install, or a database from before
     * it). This is a memory of its own, stored apart from the events: it is not derived from their dates, so a backdated entry still counts as the
     * last one logged.
     */
    fun observeLastLoggedVehicleId(): Flow<String?>

    /**
     * Saves the vehicle, its initial odometer event and its [picture] (when it has one) together, or none of them: a failed save
     * leaves no picture files behind. Returns the new vehicle id.
     */
    suspend fun addVehicle(
        name: String,
        licensePlate: String?,
        /** A new vehicle always has a type: the required-type rule is in the form, and this signature cannot save one without. */
        type: VehicleType,
        /** A new vehicle always has a color: the form starts with the default, and this signature cannot save one without. */
        color: Rgb,
        unit: OdometerUnit,
        initialOdometer: Distance,
        picture: PendingPicture? = null,
    ): String

    /**
     * Adds one distance entry to the log at [occurredAt] (in the zone it was entered in). [distance] must be above zero.
     * [loggedOdometer] is the count the user typed when logging by odometer, for the log row only. [tenthsIncluded] is the tenths
     * choice used for the entry: it is remembered for the vehicle in the same transaction, so both are saved or neither.
     * Returns the new event id.
     */
    suspend fun addDistanceEntry(
        vehicleId: String,
        occurredAt: ZonedMoment,
        distance: Distance,
        loggedOdometer: Distance?,
        tenthsIncluded: Boolean,
    ): String

    /**
     * Adds an odometer anchor at [occurredAt]: an odometer-setting event for a new odometer count logged where no odometer is
     * known. [tenthsIncluded] is remembered for the vehicle in the same transaction, as for [addDistanceEntry].
     * Returns the new event id.
     */
    suspend fun addOdometerAnchor(
        vehicleId: String,
        occurredAt: ZonedMoment,
        reading: Distance,
        tenthsIncluded: Boolean,
    ): String

    /**
     * Changes only the name, the plate, the type, the color and the picture. The log and the unit are never touched. The changes are applied together
     * or not at all; the files of a replaced or removed picture are deleted once the change is saved, and a failed save leaves
     * the vehicle's picture in use and no new files behind.
     */
    suspend fun updateVehicle(
        id: String,
        name: String,
        licensePlate: String?,
        /** The type to store. Every vehicle has one, so there is no way to save an edit without it. */
        type: VehicleType,
        /** The color to store. Every vehicle has one, so there is no way to save an edit without it. */
        color: Rgb,
        picture: PictureChange = PictureChange.Keep,
    )
}
