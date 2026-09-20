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
import com.mikonoma.drivinglog.vehicle.domain.Vehicle
import com.mikonoma.drivinglog.vehicle.domain.VehicleDetails
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
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
) : VehicleRepository {

    private val vehicles get() = database.vehicleQueries
    private val events get() = database.vehicleEventQueries

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

    override suspend fun addVehicle(
        name: String,
        licensePlate: String?,
        unit: OdometerUnit,
        initialOdometer: Distance,
    ): String = withContext(dispatcher) {
        val instant = clock.now()
        val now = instant.toEpochMilliseconds()
        // The initial odometer event happens now, in the zone the device is in now.
        val zone = ZonedMoment.of(instant, deviceTimeZone.current()).zone
        val vehicleId = newId()
        val eventId = newId()
        // One transaction: the vehicle and its initial event are both saved, or neither.
        database.transaction {
            vehicles.insertVehicle(vehicleId, name, licensePlate, unit.code, now, now)
            events.insertEvent(eventId, vehicleId, INITIAL_ODOMETER, now, initialOdometer.meters, now, zone?.id, zone?.offsetSeconds?.toLong())
        }
        vehicleId
    }

    override suspend fun addDistanceEntry(
        vehicleId: String,
        occurredAt: ZonedMoment,
        distance: Distance,
        loggedOdometer: Distance?,
        tenthsIncluded: Boolean,
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
                    occurred_at = occurredAt.instant.toEpochMilliseconds(),
                    created_at = clock.now().toEpochMilliseconds(),
                    distance_meters = distance.meters,
                    logged_odometer_meters = loggedOdometer?.meters,
                    occurred_zone = zone?.id,
                    occurred_offset_seconds = zone?.offsetSeconds?.toLong(),
                )
                vehicles.updateLogDistanceTenths(if (tenthsIncluded) 1L else 0L, vehicleId)
            }
            eventId
        }
    }

    override suspend fun updateVehicle(id: String, name: String, licensePlate: String?) {
        withContext(dispatcher) {
            vehicles.updateVehicle(name, licensePlate, clock.now().toEpochMilliseconds(), id)
        }
    }

    private companion object {
        const val INITIAL_ODOMETER = "INITIAL_ODOMETER"
        const val DISTANCE = "DISTANCE"
    }

    private fun SelectVehicles.toDomain() = Vehicle(
        id = id,
        name = name,
        licensePlate = license_plate,
        odometerUnit = OdometerUnit.fromCode(odometer_unit),
        createdAt = Instant.fromEpochMilliseconds(created_at),
        logDistanceTenths = log_distance_tenths?.let { it != 0L },
    )

    private fun SelectVehicleDetails.toDomain() = VehicleDetails(
        vehicle = Vehicle(
            id = id,
            name = name,
            licensePlate = license_plate,
            odometerUnit = OdometerUnit.fromCode(odometer_unit),
            createdAt = Instant.fromEpochMilliseconds(created_at),
            logDistanceTenths = log_distance_tenths?.let { it != 0L },
        ),
        currentOdometer = current_odometer_meters?.let { Distance(it) },
    )

    private fun SelectRecentEvents.toDomain(): VehicleEvent? =
        eventOf(id, type, occurred_at, odometer_meters, distance_meters, logged_odometer_meters, occurred_zone, occurred_offset_seconds)

    private fun SelectLog.toDomain(): VehicleEvent? =
        eventOf(id, type, occurred_at, odometer_meters, distance_meters, logged_odometer_meters, occurred_zone, occurred_offset_seconds)

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
    ): VehicleEvent? {
        // Events from before time zones were stored have neither column and are shown in the device's zone.
        val zone = if (zoneId != null && offsetSeconds != null) EventZone(zoneId, offsetSeconds.toInt()) else null
        val moment = ZonedMoment(Instant.fromEpochMilliseconds(occurredAt), zone)
        return when (type) {
            INITIAL_ODOMETER -> VehicleEvent.InitialOdometer(
                id = id,
                occurredAt = moment,
                reading = Distance(requireNotNull(odometerMeters) { "Initial odometer event without a reading" }),
            )
            DISTANCE -> VehicleEvent.DistanceEntry(
                id = id,
                occurredAt = moment,
                distance = Distance(requireNotNull(distanceMeters) { "Distance event without a distance" }),
                loggedOdometer = loggedOdometerMeters?.let { Distance(it) },
            )
            else -> null
        }
    }
}
