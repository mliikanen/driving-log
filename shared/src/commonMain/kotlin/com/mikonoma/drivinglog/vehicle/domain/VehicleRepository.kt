package com.mikonoma.drivinglog.vehicle.domain

import kotlinx.coroutines.flow.Flow

// One repository per aggregate: one function per query or command it offers.
@Suppress("TooManyFunctions")
interface VehicleRepository {
    /** All vehicles, in no particular order. */
    fun observeVehicles(): Flow<List<Vehicle>>

    /** The vehicle with its current odometer, or null when it does not exist. */
    fun observeVehicle(id: String): Flow<VehicleDetails?>

    /** At most [limit] events of the vehicle, newest first: log order ([inLogOrder]) reversed. */
    fun observeRecentEvents(vehicleId: String, limit: Int): Flow<List<VehicleEvent>>

    /** Every event of the vehicle, newest first: log order ([inLogOrder]) reversed. */
    fun observeLog(vehicleId: String): Flow<List<VehicleEvent>>

    /**
     * The entry the event [eventId] belongs to, with all of its events, re-observed live (`atomic-log-events`): emits
     * again whenever the entry, its note or its photos change. Null when no such event exists for that vehicle.
     */
    fun observeEntry(vehicleId: String, eventId: String): Flow<LogEntry?>

    /**
     * The id of the vehicle last logged for: whichever of [addDistanceEntry], [addOdometerAnchor] or [addRefueling]
     * was called last, for any vehicle, on any route (the details screen or the Home screen). Null when nothing has
     * been logged since this memory existed (a new install, or a database from before it). This is a memory of its
     * own, stored apart from the events: it is not derived from their dates, so a backdated entry still counts as
     * the last one logged.
     */
    fun observeLastLoggedVehicleId(): Flow<String?>

    /** The fuel unit last chosen while logging a refueling, for any vehicle (`add-refueling-logging`): a single,
     * global preference, never derived from the log. Null when none has been chosen yet. */
    fun observeLastFuelUnit(): Flow<FuelUnit?>

    /** The fuel type last chosen while logging a refueling, for any vehicle (`add-refueling-logging`): the same kind
     * of global preference as [observeLastFuelUnit]. Null when none has been chosen yet. */
    fun observeLastFuelType(): Flow<FuelType?>

    /**
     * Saves the vehicle, its initial odometer (an odometer reading in an entry of its own, [EntrySource.VEHICLE_ADDED]) and its
     * [picture] (when it has one) together, or none of them: a failed save leaves no picture files behind. [capture] is the
     * accepted scan the initial odometer came from (`scan-initial-odometer`), or null when it was typed: stored with the initial
     * odometer in the same transaction. Returns the new vehicle id.
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
        /** The vehicle's coarse fuel/engine category (`vehicle-fuel-type`). Defaults to Petrol: a caller that does
         * not pass one (most existing callers besides the add form, which always passes its own choice) gets the
         * same vehicle a pre-`vehicle-fuel-type` caller would have saved. */
        fuelType: VehicleFuelType = VehicleFuelType.PETROL,
    ): String

    /**
     * Adds one entry holding one distance event to the log at [occurredAt] (in the zone it was entered in). [distance] must be above zero.
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
     * Adds an entry holding one odometer reading at [occurredAt]: a new odometer count logged where no odometer is
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
     * Adds a refueling to the log at [occurredAt] (`add-refueling-logging`): one entry holding a refueling event and,
     * when [mileage] is given, the event that mileage is (`atomic-log-events`) — a distance event for
     * [RefuelingMileage.Added], an odometer reading for [RefuelingMileage.Anchor] — at the same moment, all saved
     * together or not at all. [amount] and [unit] are stored together, [unit] never converted. With no [mileage] the
     * vehicle's odometer is untouched. [tenthsIncluded] is remembered for the vehicle in the same transaction, unless
     * [mileage] is null (there is then no unit choice to remember). [note] and [photos] are stored once, on the entry.
     * [capture] is stored under the event whose number it filled: the refueling for a fuel-amount scan, the mileage
     * event for an odometer or trip scan. Returns the refueling event's id.
     */
    suspend fun addRefueling(
        vehicleId: String,
        occurredAt: ZonedMoment,
        amount: Volume,
        unit: FuelUnit,
        fuelType: FuelType,
        filledUp: Boolean,
        mileage: RefuelingMileage? = null,
        tenthsIncluded: Boolean = false,
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
        /** The fuel type to store. Defaults to Petrol for a caller that does not pass one; the edit form always
         * passes the vehicle's current (possibly changed) fuel type explicitly, never relying on this default. */
        fuelType: VehicleFuelType = VehicleFuelType.PETROL,
    )

    /**
     * Changes only the note of the entry [eventId] belongs to (add-event-editing, `atomic-log-events`), shared by every
     * event of that entry: the log's one, narrowly-scoped exception to being append-only. Null clears it. No other field,
     * and no other entry, is touched.
     */
    suspend fun updateEventNote(vehicleId: String, eventId: String, note: String?)

    /**
     * Adds one photo to the photo strip of the entry an already-saved event belongs to (`add-event-pictures`, the details screen's "Edit"
     * action), appended after whatever photos it already has. Promotes [photo] from pending to a permanent id.
     * Returns the new photo's id.
     */
    suspend fun addEventPhoto(vehicleId: String, eventId: String, photo: PendingPicture): String

    /**
     * Removes one photo, by its id, from the entry an already-saved event belongs to (`add-event-pictures`, the details screen's "Edit"
     * action, after the removal is confirmed). Deletes the photo's files. Not finding it is not an error.
     */
    suspend fun removeEventPhoto(vehicleId: String, eventId: String, pictureId: String)

    /** The scan an event's number came from (`odometer-ocr-capture`), or null when it was typed. Only for reviewing a misdetection. */
    suspend fun captureOf(eventId: String): StoredCapture?

    /** Every scan photo in use, so the capture store's sweep keeps them. */
    suspend fun capturePhotoIds(): Set<String>
}
