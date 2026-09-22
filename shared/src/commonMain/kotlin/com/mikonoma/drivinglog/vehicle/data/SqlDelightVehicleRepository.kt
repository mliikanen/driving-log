package com.mikonoma.drivinglog.vehicle.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.mikonoma.drivinglog.db.DrivingLogDatabase
import com.mikonoma.drivinglog.db.SelectLog
import com.mikonoma.drivinglog.db.SelectRecentEvents
import com.mikonoma.drivinglog.db.SelectVehicleDetails
import com.mikonoma.drivinglog.db.SelectVehicles
import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.EventZone
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.PendingPicture
import com.mikonoma.drivinglog.vehicle.domain.PictureChange
import com.mikonoma.drivinglog.vehicle.domain.Vehicle
import com.mikonoma.drivinglog.vehicle.domain.VehicleDetails
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleColors
import com.mikonoma.drivinglog.vehicle.domain.VehicleType
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import com.mikonoma.drivinglog.vehicle.domain.truncatedToMinute
import com.mikonoma.drivinglog.vehicle.picture.VehiclePictureStore
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SqlDelightVehicleRepository(
    private val database: DrivingLogDatabase,
    private val clock: Clock,
    private val newId: () -> String,
    private val dispatcher: CoroutineDispatcher,
    private val deviceTimeZone: DeviceTimeZone,
    private val pictures: VehiclePictureStore,
) : VehicleRepository {

    private val vehicles get() = database.vehicleQueries
    private val events get() = database.vehicleEventQueries
    private val appState get() = database.appStateQueries

    override fun observeVehicles(): Flow<List<Vehicle>> =
        vehicles.selectVehicles().asFlow().mapToList(dispatcher).map { rows -> rows.map { it.toDomain() } }

    override fun observeVehicle(id: String): Flow<VehicleDetails?> =
        vehicles.selectVehicleDetails(id).asFlow().mapToOneOrNull(dispatcher).map { it?.toDomain() }

    override fun observeRecentEvents(vehicleId: String, limit: Int): Flow<List<VehicleEvent>> =
        events.selectRecentEvents(vehicleId, limit.toLong()).asFlow().mapToList(dispatcher)
            .map { rows -> rows.mapNotNull { it.toDomain() } }

    override fun observeLog(vehicleId: String): Flow<List<VehicleEvent>> =
        events.selectLog(vehicleId).asFlow().mapToList(dispatcher)
            .map { rows -> rows.mapNotNull { it.toDomain() } }

    override fun observeLastLoggedVehicleId(): Flow<String?> =
        appState.selectAppState(LAST_LOGGED_VEHICLE_ID_KEY).asFlow().mapToOneOrNull(dispatcher)

    override suspend fun addVehicle(
        name: String,
        licensePlate: String?,
        type: VehicleType,
        color: Rgb,
        unit: OdometerUnit,
        initialOdometer: Distance,
        picture: PendingPicture?,
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
        try {
            // One transaction: the vehicle and its initial event are both saved, or neither.
            database.transaction {
                vehicles.insertVehicle(vehicleId, name, licensePlate, unit.code, now, now, pictureId, type.code, color.hex)
                // The initial odometer event never carries a note (add-event-notes): it is created by this flow, not the log event form.
                events.insertEvent(eventId, vehicleId, INITIAL_ODOMETER, occurredAt.toEpochMilliseconds(), initialOdometer.meters, now, zone?.id, zone?.offsetSeconds?.toLong(), null)
            }
        } catch (throwable: Throwable) {
            // Nothing was saved, so the files that were just moved into use belong to no vehicle.
            pictureId?.let { pictures.delete(it) }
            throw throwable
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
    ): String {
        require(distance.meters > 0) { "A distance entry must be above zero" }
        return withContext(dispatcher) {
            val eventId = newId()
            val zone = occurredAt.zone
            // One transaction: the entry and the remembered tenths choice are both saved, or neither.
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
                vehicles.updateLogDistanceTenths(if (tenthsIncluded) 1L else 0L, vehicleId)
                appState.upsertAppState(LAST_LOGGED_VEHICLE_ID_KEY, vehicleId)
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
    ): String = withContext(dispatcher) {
        val eventId = newId()
        val zone = occurredAt.zone
        // One transaction: the anchor and the remembered tenths choice are both saved, or neither.
        database.transaction {
            events.insertEvent(
                eventId, vehicleId, ODOMETER_ANCHOR, occurredAt.instant.truncatedToMinute().toEpochMilliseconds(), reading.meters,
                clock.now().toEpochMilliseconds(), zone?.id, zone?.offsetSeconds?.toLong(), note,
            )
            vehicles.updateLogDistanceTenths(if (tenthsIncluded) 1L else 0L, vehicleId)
            appState.upsertAppState(LAST_LOGGED_VEHICLE_ID_KEY, vehicleId)
        }
        eventId
    }

    override suspend fun updateVehicle(id: String, name: String, licensePlate: String?, type: VehicleType, color: Rgb, picture: PictureChange) {
        withContext(dispatcher) {
            val now = clock.now().toEpochMilliseconds()
            val newPictureId = (picture as? PictureChange.Replace)?.let { promoted(it.picture) }
            var oldPictureId: String? = null
            try {
                // One transaction: the name, the plate, the type, the color and the picture change together, or not at all.
                database.transaction {
                    vehicles.updateVehicle(name, licensePlate, now, id)
                    vehicles.updateVehicleType(type.code, now, id)
                    vehicles.updateVehicleColor(color.hex, now, id)
                    if (picture !is PictureChange.Keep) {
                        oldPictureId = vehicles.selectPictureId(id).executeAsOneOrNull()?.picture_id
                        vehicles.updateVehiclePicture(newPictureId, now, id)
                    }
                }
            } catch (throwable: Throwable) {
                newPictureId?.let { pictures.delete(it) }
                throw throwable
            }
            // Saved: the earlier picture is no longer used. A failure here only leaves files for the sweep.
            oldPictureId?.takeIf { it != newPictureId }?.let { runCatching { pictures.delete(it) } }
        }
    }

    /** Moves a pending picture into use. The pending files are gone or incomplete when the save cannot go on. */
    private suspend fun promoted(picture: PendingPicture): String =
        pictures.promote(picture.pendingId) ?: error("The picture ${picture.pendingId} is no longer available")

    private companion object {
        const val INITIAL_ODOMETER = "INITIAL_ODOMETER"
        const val DISTANCE = "DISTANCE"
        const val ODOMETER_ANCHOR = "ODOMETER_ANCHOR"

        /** The key `app_state` remembers the vehicle last logged for under. */
        const val LAST_LOGGED_VEHICLE_ID_KEY = "last_logged_vehicle_id"
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
        ),
        currentOdometer = current_odometer_meters?.let { Distance(it) },
    )

    private fun SelectRecentEvents.toDomain(): VehicleEvent? =
        eventOf(id, type, occurred_at, odometer_meters, distance_meters, logged_odometer_meters, occurred_zone, occurred_offset_seconds, note)

    private fun SelectLog.toDomain(): VehicleEvent? =
        eventOf(id, type, occurred_at, odometer_meters, distance_meters, logged_odometer_meters, occurred_zone, occurred_offset_seconds, note)

    /** Unknown types (from a newer app version, say) are skipped instead of crashing the screen. */
    private fun eventOf(
        id: String,
        type: String,
        occurredAt: Long,
        odometerMeters: Long?,
        distanceMeters: Long?,
        loggedOdometerMeters: Long?,
        zoneId: String?,
        offsetSeconds: Long?,
        note: String?,
    ): VehicleEvent? {
        // Events from before time zones were stored have neither column and are shown in the device's zone.
        val zone = if (zoneId != null && offsetSeconds != null) EventZone(zoneId, offsetSeconds.toInt()) else null
        val moment = ZonedMoment(Instant.fromEpochMilliseconds(occurredAt), zone)
        return when (type) {
            // The initial odometer event never has a note (add-event-notes): the column reads null for it, and it is ignored here.
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
            )
            DISTANCE -> VehicleEvent.DistanceEntry(
                id = id,
                occurredAt = moment,
                distance = Distance(requireNotNull(distanceMeters) { "Distance event without a distance" }),
                loggedOdometer = loggedOdometerMeters?.let { Distance(it) },
                note = note,
            )
            else -> null
        }
    }
}
