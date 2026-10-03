package com.mikonoma.drivinglog.vehicle.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.mikonoma.drivinglog.db.DrivingLogDatabase
import com.mikonoma.drivinglog.db.SelectEventById
import com.mikonoma.drivinglog.db.SelectLog
import com.mikonoma.drivinglog.db.SelectRecentEvents
import com.mikonoma.drivinglog.db.SelectVehicleDetails
import com.mikonoma.drivinglog.db.SelectVehicles
import com.mikonoma.drivinglog.util.undoOnFailure
import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.EventZone
import com.mikonoma.drivinglog.vehicle.domain.FuelType
import com.mikonoma.drivinglog.vehicle.domain.FuelUnit
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.PendingCapture
import com.mikonoma.drivinglog.vehicle.domain.PendingPicture
import com.mikonoma.drivinglog.vehicle.domain.PictureChange
import com.mikonoma.drivinglog.vehicle.domain.RefuelingMileage
import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.StoredCapture
import com.mikonoma.drivinglog.vehicle.domain.Vehicle
import com.mikonoma.drivinglog.vehicle.domain.VehicleColors
import com.mikonoma.drivinglog.vehicle.domain.VehicleDetails
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.VehicleFuelType
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.domain.VehicleType
import com.mikonoma.drivinglog.vehicle.domain.Volume
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import com.mikonoma.drivinglog.vehicle.domain.truncatedToMinute
import com.mikonoma.drivinglog.vehicle.ocr.CaptureStore
import com.mikonoma.drivinglog.vehicle.ocr.ScanResult
import com.mikonoma.drivinglog.vehicle.picture.PictureStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant

class SqlDelightVehicleRepository(
    private val database: DrivingLogDatabase,
    private val clock: Clock,
    private val newId: () -> String,
    private val dispatcher: CoroutineDispatcher,
    private val deviceTimeZone: DeviceTimeZone,
    private val pictures: PictureStore,
    /** A separate store instance from [pictures] (`add-event-pictures`), pointed at its own root: an event photo
     * never shares a directory with a vehicle's picture, though both draw ids from the same collision-free space. */
    private val eventPictures: PictureStore = pictures,
    /** Where the photos of accepted scans are kept (`odometer-ocr-capture`); only needed by a caller that saves one. */
    private val captures: CaptureStore? = null,
) : VehicleRepository {

    private val vehicles get() = database.vehicleQueries
    private val events get() = database.vehicleEventQueries
    private val appState get() = database.appStateQueries
    private val eventPhotos get() = database.eventPictureQueries
    private val eventCaptures get() = database.eventCaptureQueries

    override fun observeVehicles(): Flow<List<Vehicle>> = vehicles.selectVehicles().asFlow().mapToList(dispatcher).map { rows -> rows.map { it.toDomain() } }

    override fun observeVehicle(id: String): Flow<VehicleDetails?> =
        vehicles.selectVehicleDetails(id).asFlow().mapToOneOrNull(dispatcher).map { it?.toDomain() }

    override fun observeRecentEvents(vehicleId: String, limit: Int): Flow<List<VehicleEvent>> =
        events.selectRecentEvents(vehicleId, limit.toLong()).asFlow().mapToList(dispatcher)
            .map { rows -> rows.mapNotNull { it.toDomain() } }

    override fun observeLog(vehicleId: String): Flow<List<VehicleEvent>> = events.selectLog(vehicleId).asFlow().mapToList(dispatcher)
        .map { rows -> rows.mapNotNull { it.toDomain() } }

    override fun observeEvent(vehicleId: String, eventId: String): Flow<VehicleEvent?> =
        events.selectEventById(vehicleId, eventId).asFlow().mapToOneOrNull(dispatcher).map { it?.toDomain() }

    private suspend fun photoIdsOf(eventId: String): List<String> = withContext(dispatcher) { eventPhotos.selectPhotoIdsForEvent(eventId).executeAsList() }

    override fun observeLastLoggedVehicleId(): Flow<String?> = appState.selectAppState(LAST_LOGGED_VEHICLE_ID_KEY).asFlow().mapToOneOrNull(dispatcher)

    override fun observeLastFuelUnit(): Flow<FuelUnit?> =
        appState.selectAppState(LAST_FUEL_UNIT_KEY).asFlow().mapToOneOrNull(dispatcher).map { it?.let(FuelUnit::fromCode) }

    override fun observeLastFuelType(): Flow<FuelType?> =
        appState.selectAppState(LAST_FUEL_TYPE_KEY).asFlow().mapToOneOrNull(dispatcher).map { it?.let(FuelType::fromCode) }

    override suspend fun addVehicle(
        name: String,
        licensePlate: String?,
        type: VehicleType,
        color: Rgb,
        unit: OdometerUnit,
        initialOdometer: Distance,
        picture: PendingPicture?,
        capture: PendingCapture?,
        fuelType: VehicleFuelType,
    ): String = withContext(dispatcher) {
        val instant = clock.now()
        val now = instant.toEpochMilliseconds()
        // The initial odometer event happens now, in the zone the device is in now. Like every time a user defines it is to
        // the minute, so an entry logged in the same minute is not dated before it; created_at keeps the exact time.
        val occurredAt = instant.truncatedToMinute()
        val zone = ZonedMoment.of(occurredAt, deviceTimeZone.current()).zone
        val vehicleId = newId()
        val eventId = newId()
        val pictureId = picture?.let { promoted(it) }
        val promotedCapture = undoOnFailure(
            undo = {
                pictureId?.let { pictures.delete(it) }
            },
        ) {
            capture?.let { promotedCapture(it) to it.result.toJson() }
        }
        undoOnFailure(
            undo = {
                // Nothing was saved, so the files that were just moved into use belong to no vehicle.
                pictureId?.let { pictures.delete(it) }
                promotedCapture?.let { captures?.delete(it.first) }
            },
        ) {
            // One transaction: the vehicle, its initial event and the scan it came from (scan-initial-odometer) are all saved, or none.
            database.transaction {
                vehicles.insertVehicle(vehicleId, name, licensePlate, unit.code, now, now, pictureId, type.code, color.hex, fuelType.code)
                // The initial odometer event never carries a note (add-event-notes): it is created by this flow, not the log event form.
                events.insertEvent(
                    eventId,
                    vehicleId,
                    INITIAL_ODOMETER,
                    occurredAt.toEpochMilliseconds(),
                    initialOdometer.meters,
                    now,
                    zone?.id,
                    zone?.offsetSeconds?.toLong(),
                    null,
                )
                promotedCapture?.let { (photoId, detections) -> eventCaptures.insertEventCapture(eventId, photoId, detections) }
            }
        }
        vehicleId
    }

    override suspend fun addDistanceEntry(
        vehicleId: String,
        occurredAt: ZonedMoment,
        distance: Distance,
        loggedOdometer: Distance?,
        tenthsIncluded: Boolean,
        note: String?,
        photos: List<PendingPicture>,
        capture: PendingCapture?,
    ): String {
        require(distance.meters > 0) { "A distance entry must be above zero" }
        return withContext(dispatcher) {
            val eventId = newId()
            val zone = occurredAt.zone
            val photoIds = photos.map { promotedEventPhoto(it) }
            val promotedCapture = capture?.let { promotedCapture(it) to it.result.toJson() }
            undoOnFailure(
                undo = {
                    for (photoId in photoIds) eventPictures.delete(photoId)
                    promotedCapture?.let { captures?.delete(it.first) }
                },
            ) {
                // One transaction: the entry, the remembered tenths choice and the attached photos are all saved, or none.
                database.transaction {
                    events.insertDistanceEntry(
                        id = eventId,
                        vehicle_id = vehicleId,
                        occurred_at = occurredAt.instant.truncatedToMinute().toEpochMilliseconds(),
                        created_at = clock.now().toEpochMilliseconds(),
                        distance_meters = distance.meters,
                        logged_odometer_meters = loggedOdometer?.meters,
                        occurred_zone = zone?.id,
                        occurred_offset_seconds = zone?.offsetSeconds?.toLong(),
                        note = note,
                    )
                    insertEventPhotos(eventId, photoIds)
                    promotedCapture?.let { (photoId, detections) -> eventCaptures.insertEventCapture(eventId, photoId, detections) }
                    vehicles.updateLogDistanceTenths(if (tenthsIncluded) 1L else 0L, vehicleId)
                    appState.upsertAppState(LAST_LOGGED_VEHICLE_ID_KEY, vehicleId)
                }
            }
            eventId
        }
    }

    override suspend fun addOdometerAnchor(
        vehicleId: String,
        occurredAt: ZonedMoment,
        reading: Distance,
        tenthsIncluded: Boolean,
        note: String?,
        photos: List<PendingPicture>,
        capture: PendingCapture?,
    ): String = withContext(dispatcher) {
        val eventId = newId()
        val zone = occurredAt.zone
        val photoIds = photos.map { promotedEventPhoto(it) }
        val promotedCapture = capture?.let { promotedCapture(it) to it.result.toJson() }
        undoOnFailure(
            undo = {
                for (photoId in photoIds) eventPictures.delete(photoId)
                promotedCapture?.let { captures?.delete(it.first) }
            },
        ) {
            // One transaction: the anchor, the remembered tenths choice and the attached photos are all saved, or none.
            database.transaction {
                events.insertEvent(
                    eventId, vehicleId, ODOMETER_ANCHOR, occurredAt.instant.truncatedToMinute().toEpochMilliseconds(), reading.meters,
                    clock.now().toEpochMilliseconds(), zone?.id, zone?.offsetSeconds?.toLong(), note,
                )
                insertEventPhotos(eventId, photoIds)
                promotedCapture?.let { (photoId, detections) -> eventCaptures.insertEventCapture(eventId, photoId, detections) }
                vehicles.updateLogDistanceTenths(if (tenthsIncluded) 1L else 0L, vehicleId)
                appState.upsertAppState(LAST_LOGGED_VEHICLE_ID_KEY, vehicleId)
            }
        }
        eventId
    }

    override suspend fun addRefueling(
        vehicleId: String,
        occurredAt: ZonedMoment,
        amount: Volume,
        unit: FuelUnit,
        fuelType: FuelType,
        filledUp: Boolean,
        mileage: RefuelingMileage?,
        tenthsIncluded: Boolean,
        note: String?,
        photos: List<PendingPicture>,
        capture: PendingCapture?,
    ): String {
        require(amount.milliliters > 0) { "A refueling's fuel amount must be above zero" }
        return withContext(dispatcher) {
            val eventId = newId()
            val zone = occurredAt.zone
            val photoIds = photos.map { promotedEventPhoto(it) }
            val promotedCapture = capture?.let { promotedCapture(it) to it.result.toJson() }
            undoOnFailure(
                undo = {
                    for (photoId in photoIds) eventPictures.delete(photoId)
                    promotedCapture?.let { captures?.delete(it.first) }
                },
            ) {
                // One transaction: the refueling, its optional mileage's remembered tenths choice, the remembered fuel
                // unit/type and the attached photos are all saved, or none.
                database.transaction {
                    events.insertRefueling(
                        id = eventId,
                        vehicle_id = vehicleId,
                        occurred_at = occurredAt.instant.truncatedToMinute().toEpochMilliseconds(),
                        created_at = clock.now().toEpochMilliseconds(),
                        odometer_meters = (mileage as? RefuelingMileage.Anchor)?.reading?.meters,
                        distance_meters = (mileage as? RefuelingMileage.Added)?.distance?.meters,
                        logged_odometer_meters = (mileage as? RefuelingMileage.Added)?.loggedOdometer?.meters,
                        occurred_zone = zone?.id,
                        occurred_offset_seconds = zone?.offsetSeconds?.toLong(),
                        note = note,
                        fuel_amount_milliliters = amount.milliliters,
                        fuel_unit = unit.code,
                        fuel_type = fuelType.code,
                        filled_up = if (filledUp) 1L else 0L,
                    )
                    insertEventPhotos(eventId, photoIds)
                    promotedCapture?.let { (photoId, detections) -> eventCaptures.insertEventCapture(eventId, photoId, detections) }
                    if (mileage != null) vehicles.updateLogDistanceTenths(if (tenthsIncluded) 1L else 0L, vehicleId)
                    appState.upsertAppState(LAST_LOGGED_VEHICLE_ID_KEY, vehicleId)
                    appState.upsertAppState(LAST_FUEL_UNIT_KEY, unit.code)
                    appState.upsertAppState(LAST_FUEL_TYPE_KEY, fuelType.code)
                }
            }
            eventId
        }
    }

    override suspend fun updateVehicle(
        id: String,
        name: String,
        licensePlate: String?,
        type: VehicleType,
        color: Rgb,
        picture: PictureChange,
        fuelType: VehicleFuelType,
    ) {
        withContext(dispatcher) {
            val now = clock.now().toEpochMilliseconds()
            val newPictureId = (picture as? PictureChange.Replace)?.let { promoted(it.picture) }
            var oldPictureId: String? = null
            undoOnFailure(
                undo = {
                    newPictureId?.let { pictures.delete(it) }
                },
            ) {
                // One transaction: the name, the plate, the type, the color, the fuel type and the picture change together, or not at all.
                database.transaction {
                    vehicles.updateVehicle(name, licensePlate, now, id)
                    vehicles.updateVehicleType(type.code, now, id)
                    vehicles.updateVehicleColor(color.hex, now, id)
                    vehicles.updateVehicleFuelType(fuelType.code, now, id)
                    if (picture !is PictureChange.Keep) {
                        oldPictureId = vehicles.selectPictureId(id).executeAsOneOrNull()?.picture_id
                        vehicles.updateVehiclePicture(newPictureId, now, id)
                    }
                }
            }
            // Saved: the earlier picture is no longer used. A failure here only leaves files for the sweep.
            oldPictureId?.takeIf { it != newPictureId }?.let { runCatching { pictures.delete(it) } }
        }
    }

    override suspend fun updateEventNote(vehicleId: String, eventId: String, note: String?) {
        withContext(dispatcher) {
            events.updateEventNote(note, vehicleId, eventId)
        }
    }

    override suspend fun addEventPhoto(vehicleId: String, eventId: String, photo: PendingPicture): String = withContext(dispatcher) {
        val photoId = promotedEventPhoto(photo)
        undoOnFailure(
            undo = {
                eventPictures.delete(photoId)
            },
        ) {
            // touchEvent: see its own doc comment — event_picture alone does not make a live-observed event
            // (details screen, recent events, full log) notice this change.
            database.transaction {
                insertEventPhotos(eventId, listOf(photoId))
                events.touchEvent(eventId)
            }
        }
        photoId
    }

    override suspend fun removeEventPhoto(vehicleId: String, eventId: String, pictureId: String) {
        withContext(dispatcher) {
            // Only delete the files when that photo actually belonged to that event: a mismatched pair must not
            // delete a picture another event still references.
            if (pictureId in eventPhotos.selectPhotoIdsForEvent(eventId).executeAsList()) {
                database.transaction {
                    eventPhotos.deleteEventPicture(eventId, pictureId)
                    events.touchEvent(eventId)
                }
                eventPictures.delete(pictureId)
            }
        }
    }

    /** Moves a pending vehicle picture into use. The pending files are gone or incomplete when the save cannot go on. */
    private suspend fun promoted(picture: PendingPicture): String =
        pictures.promote(picture.pendingId) ?: error("The picture ${picture.pendingId} is no longer available")

    /** Moves a pending event photo into use, from the separate [eventPictures] store (`add-event-pictures`). */
    private suspend fun promotedEventPhoto(photo: PendingPicture): String =
        eventPictures.promote(photo.pendingId) ?: error("The photo ${photo.pendingId} is no longer available")

    /** Moves an accepted scan's photo into use (`odometer-ocr-capture`). */
    private suspend fun promotedCapture(capture: PendingCapture): String {
        val store = captures ?: error("No capture store to save the scan ${capture.pendingId} with")
        return store.promote(capture.pendingId) ?: error("The scan ${capture.pendingId} is no longer available")
    }

    override suspend fun captureOf(eventId: String): StoredCapture? = withContext(dispatcher) {
        eventCaptures.selectEventCapture(eventId).executeAsOneOrNull()?.let { StoredCapture(it.photo_id, ScanResult.fromJson(it.detections)) }
    }

    override suspend fun capturePhotoIds(): Set<String> = withContext(dispatcher) { eventCaptures.selectCapturePhotoIds().executeAsList().toSet() }

    /** Inserts one `event_picture` row per already-promoted [photoIds], appended after whatever the event already has. */
    private fun insertEventPhotos(eventId: String, photoIds: List<String>) {
        if (photoIds.isEmpty()) return
        val startPosition = eventPhotos.selectNextPosition(eventId).executeAsOne()
        val now = clock.now().toEpochMilliseconds()
        photoIds.forEachIndexed { index, photoId ->
            eventPhotos.insertEventPicture(photoId, eventId, startPosition + index, now)
        }
    }

    private companion object {
        const val INITIAL_ODOMETER = "INITIAL_ODOMETER"
        const val DISTANCE = "DISTANCE"
        const val ODOMETER_ANCHOR = "ODOMETER_ANCHOR"
        const val REFUELING = "REFUELING"

        /** The key `app_state` remembers the vehicle last logged for under. */
        const val LAST_LOGGED_VEHICLE_ID_KEY = "last_logged_vehicle_id"

        /** The keys `app_state` remembers a refueling's fuel unit/type under (`add-refueling-logging`): global,
         * independent of any vehicle, the same mechanism as [LAST_LOGGED_VEHICLE_ID_KEY]. */
        const val LAST_FUEL_UNIT_KEY = "last_fuel_unit"
        const val LAST_FUEL_TYPE_KEY = "last_fuel_type"
    }

    private fun SelectVehicles.toDomain() = Vehicle(
        id = id,
        name = name,
        licensePlate = license_plate,
        odometerUnit = OdometerUnit.fromCode(odometer_unit),
        createdAt = Instant.fromEpochMilliseconds(created_at),
        logDistanceTenths = log_distance_tenths?.let { it != 0L },
        pictureId = picture_id,
        type = VehicleType.fromCode(vehicle_type) ?: VehicleType.OTHER,
        color = Rgb.parse(vehicle_color) ?: VehicleColors.default,
        fuelType = VehicleFuelType.fromCode(vehicle_fuel_type) ?: VehicleFuelType.OTHER,
    )

    private fun SelectVehicleDetails.toDomain() = VehicleDetails(
        vehicle = Vehicle(
            id = id,
            name = name,
            licensePlate = license_plate,
            odometerUnit = OdometerUnit.fromCode(odometer_unit),
            createdAt = Instant.fromEpochMilliseconds(created_at),
            logDistanceTenths = log_distance_tenths?.let { it != 0L },
            pictureId = picture_id,
            type = VehicleType.fromCode(vehicle_type) ?: VehicleType.OTHER,
            color = Rgb.parse(vehicle_color) ?: VehicleColors.default,
            fuelType = VehicleFuelType.fromCode(vehicle_fuel_type) ?: VehicleFuelType.OTHER,
        ),
        currentOdometer = current_odometer_meters?.let { Distance(it) },
    )

    private suspend fun SelectRecentEvents.toDomain(): VehicleEvent? = eventOf(
        id, type, occurred_at, odometer_meters, distance_meters, logged_odometer_meters, occurred_zone,
        occurred_offset_seconds, note, fuel_amount_milliliters, fuel_unit, fuel_type, filled_up,
    )

    private suspend fun SelectLog.toDomain(): VehicleEvent? = eventOf(
        id, type, occurred_at, odometer_meters, distance_meters, logged_odometer_meters, occurred_zone,
        occurred_offset_seconds, note, fuel_amount_milliliters, fuel_unit, fuel_type, filled_up,
    )

    private suspend fun SelectEventById.toDomain(): VehicleEvent? = eventOf(
        id, type, occurred_at, odometer_meters, distance_meters, logged_odometer_meters, occurred_zone,
        occurred_offset_seconds, note, fuel_amount_milliliters, fuel_unit, fuel_type, filled_up,
    )

    /** Unknown types (from a newer app version, say) are skipped instead of crashing the screen. */
    private suspend fun eventOf(
        id: String,
        type: String,
        occurredAt: Long,
        odometerMeters: Long?,
        distanceMeters: Long?,
        loggedOdometerMeters: Long?,
        zoneId: String?,
        offsetSeconds: Long?,
        note: String?,
        fuelAmountMilliliters: Long? = null,
        fuelUnitCode: String? = null,
        fuelTypeCode: String? = null,
        filledUp: Long? = null,
    ): VehicleEvent? {
        // Events from before time zones were stored have neither column and are shown in the device's zone.
        val zone = if (zoneId != null && offsetSeconds != null) EventZone(zoneId, offsetSeconds.toInt()) else null
        val moment = ZonedMoment(Instant.fromEpochMilliseconds(occurredAt), zone)
        return when (type) {
            // The initial odometer event never has a note or a photo (add-event-notes, add-event-pictures): both
            // columns read null/empty for it, and are ignored here.
            INITIAL_ODOMETER -> VehicleEvent.InitialOdometer(
                id = id,
                occurredAt = moment,
                reading = Distance(requireNotNull(odometerMeters) { "Initial odometer event without a reading" }),
            )

            ODOMETER_ANCHOR -> VehicleEvent.OdometerAnchor(
                id = id,
                occurredAt = moment,
                reading = Distance(requireNotNull(odometerMeters) { "Odometer anchor event without a reading" }),
                note = note,
                photoIds = photoIdsOf(id),
            )

            DISTANCE -> VehicleEvent.DistanceEntry(
                id = id,
                occurredAt = moment,
                distance = Distance(requireNotNull(distanceMeters) { "Distance event without a distance" }),
                loggedOdometer = loggedOdometerMeters?.let { Distance(it) },
                note = note,
                photoIds = photoIdsOf(id),
            )

            // A refueling's mileage (add-refueling-logging) reuses the same odometer/distance columns an odometer
            // anchor/distance entry already do: odometer_meters set means it was logged by "New odometer",
            // distance_meters set means "Trip distance", and neither set means no mileage was given.
            REFUELING -> VehicleEvent.Refueling(
                id = id,
                occurredAt = moment,
                amount = Volume(requireNotNull(fuelAmountMilliliters) { "Refueling event without a fuel amount" }),
                unit = FuelUnit.fromCode(requireNotNull(fuelUnitCode) { "Refueling event without a fuel unit" }),
                fuelType = FuelType.fromCode(fuelTypeCode) ?: FuelType.OTHER,
                filledUp = requireNotNull(filledUp) { "Refueling event without a filled-up flag" } != 0L,
                mileage = when {
                    odometerMeters != null -> RefuelingMileage.Anchor(Distance(odometerMeters))
                    distanceMeters != null -> RefuelingMileage.Added(Distance(distanceMeters), loggedOdometerMeters?.let { Distance(it) })
                    else -> null
                },
                note = note,
                photoIds = photoIdsOf(id),
            )

            else -> null
        }
    }
}
