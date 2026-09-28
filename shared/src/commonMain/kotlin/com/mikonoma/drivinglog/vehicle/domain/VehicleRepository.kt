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
     * One event by id, re-observed live (add-event-details-view): emits again whenever the event changes. Null
     * when no such event exists for that vehicle (including once an event can be removed, though nothing can yet).
     */
    fun observeEvent(vehicleId: String, eventId: String): Flow<VehicleEvent?>

    /**
     * The id of the vehicle last logged for: whichever of [addDistanceEntry] or [addOdometerAnchor] was called last, for any vehicle, on any route
     * (the details screen or the Home screen). Null when nothing has been logged since this memory existed (a new install, or a database from before
     * it). This is a memory of its own, stored apart from the events: it is not derived from their dates, so a backdated entry still counts as the
     * last one logged.
     */
    fun observeLastLoggedVehicleId(): Flow<String?>

    /**
     * Saves the vehicle, its initial odometer event and its [picture] (when it has one) together, or none of them: a failed save
     * leaves no picture files behind. [capture] is the accepted scan the initial odometer came from (`scan-initial-odometer`), or null
     * when it was typed: stored with the initial odometer event in the same transaction. Returns the new vehicle id.
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
        capture: PendingCapture? = null,
    ): String

    /**
     * Adds one distance entry to the log at [occurredAt] (in the zone it was entered in). [distance] must be above zero.
     * [loggedOdometer] is the count the user typed when logging by odometer, for the log row only. [tenthsIncluded] is the tenths
     * choice used for the entry: it is remembered for the vehicle in the same transaction, so both are saved or neither.
     * [note] is the pending note typed on the log event form (`add-event-notes`), or null when none was added.
     * [photos] are the pending photos attached on the log event form (`add-event-pictures`, 0 to 5), promoted and saved
     * with the entry in the same transaction, in the order given, or none of them: a failed save leaves no photo files
     * behind. [capture] is the accepted scan the number came from (`odometer-ocr-capture`), or null when it was typed: saved
     * with the entry in the same transaction, like [photos]. Returns the new event id.
     */
    suspend fun addDistanceEntry(
        vehicleId: String,
        occurredAt: ZonedMoment,
        distance: Distance,
        loggedOdometer: Distance?,
        tenthsIncluded: Boolean,
        note: String? = null,
        photos: List<PendingPicture> = emptyList(),
        capture: PendingCapture? = null,
    ): String

    /**
     * Adds an odometer anchor at [occurredAt]: an odometer-setting event for a new odometer count logged where no odometer is
     * known, or one lower than the known odometer, confirmed by the user (`confirm-lower-odometer`). [tenthsIncluded] is
     * remembered for the vehicle in the same transaction, as for [addDistanceEntry]. [note] is the
     * pending note typed on the log event form (`add-event-notes`), or null when none was added. [photos] are the pending
     * photos attached on the log event form (`add-event-pictures`), and [capture] the accepted scan (`odometer-ocr-capture`),
     * both saved the same way as for [addDistanceEntry]. Returns the new event id.
     */
    suspend fun addOdometerAnchor(
        vehicleId: String,
        occurredAt: ZonedMoment,
        reading: Distance,
        tenthsIncluded: Boolean,
        note: String? = null,
        photos: List<PendingPicture> = emptyList(),
        capture: PendingCapture? = null,
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

    /**
     * Changes only the note of one event (add-event-editing), the log's one, narrowly-scoped exception to being
     * append-only. Null clears it. No other field of the event, and no other event, is touched.
     */
    suspend fun updateEventNote(vehicleId: String, eventId: String, note: String?)

    /**
     * Adds one photo to an already-saved event's photo strip (`add-event-pictures`, the details screen's "Edit"
     * action), appended after whatever photos it already has. Promotes [photo] from pending to a permanent id.
     * Returns the new photo's id.
     */
    suspend fun addEventPhoto(vehicleId: String, eventId: String, photo: PendingPicture): String

    /**
     * Removes one photo, by its id, from an already-saved event (`add-event-pictures`, the details screen's "Edit"
     * action, after the removal is confirmed). Deletes the photo's files. Not finding it is not an error.
     */
    suspend fun removeEventPhoto(vehicleId: String, eventId: String, pictureId: String)

    /** The scan an event's number came from (`odometer-ocr-capture`), or null when it was typed. Only for reviewing a misdetection. */
    suspend fun captureOf(eventId: String): StoredCapture?

    /** Every scan photo in use, so the capture store's sweep keeps them. */
    suspend fun capturePhotoIds(): Set<String>
}
