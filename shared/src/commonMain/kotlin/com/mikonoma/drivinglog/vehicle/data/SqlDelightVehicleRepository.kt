package com.mikonoma.drivinglog.vehicle.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.mikonoma.drivinglog.db.DrivingLogDatabase
import com.mikonoma.drivinglog.db.SelectLog
import com.mikonoma.drivinglog.db.SelectRecentEvents
import com.mikonoma.drivinglog.db.SelectVehicleDetails
import com.mikonoma.drivinglog.db.SelectVehicles
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.Vehicle
import com.mikonoma.drivinglog.vehicle.domain.VehicleDetails
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
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
        val now = clock.now().toEpochMilliseconds()
        val vehicleId = newId()
        val eventId = newId()
        // One transaction: the vehicle and its initial event are both saved, or neither.
        database.transaction {
            vehicles.insertVehicle(vehicleId, name, licensePlate, unit.code, now, now)
            events.insertEvent(eventId, vehicleId, INITIAL_ODOMETER, now, initialOdometer.meters, now)
        }
        vehicleId
    }

    override suspend fun updateVehicle(id: String, name: String, licensePlate: String?) {
        withContext(dispatcher) {
            vehicles.updateVehicle(name, licensePlate, clock.now().toEpochMilliseconds(), id)
        }
    }

    private companion object {
        const val INITIAL_ODOMETER = "INITIAL_ODOMETER"
    }

    private fun SelectVehicles.toDomain() = Vehicle(
        id = id,
        name = name,
        licensePlate = license_plate,
        odometerUnit = OdometerUnit.fromCode(odometer_unit),
        createdAt = Instant.fromEpochMilliseconds(created_at),
    )

    private fun SelectVehicleDetails.toDomain() = VehicleDetails(
        vehicle = Vehicle(
            id = id,
            name = name,
            licensePlate = license_plate,
            odometerUnit = OdometerUnit.fromCode(odometer_unit),
            createdAt = Instant.fromEpochMilliseconds(created_at),
        ),
        currentOdometer = current_odometer_meters?.let { Distance(it) },
    )

    private fun SelectRecentEvents.toDomain(): VehicleEvent? = eventOf(id, type, occurred_at, odometer_meters)

    private fun SelectLog.toDomain(): VehicleEvent? = eventOf(id, type, occurred_at, odometer_meters)

    /** Unknown types (from a newer app version, say) are skipped instead of crashing the screen. */
    private fun eventOf(id: String, type: String, occurredAt: Long, odometerMeters: Long?): VehicleEvent? =
        when (type) {
            INITIAL_ODOMETER -> VehicleEvent.InitialOdometer(
                id = id,
                occurredAt = Instant.fromEpochMilliseconds(occurredAt),
                reading = Distance(requireNotNull(odometerMeters) { "Initial odometer event without a reading" }),
            )
            else -> null
        }
}
