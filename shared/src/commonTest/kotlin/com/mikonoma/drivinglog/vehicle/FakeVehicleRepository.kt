package com.mikonoma.drivinglog.vehicle

import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.Vehicle
import com.mikonoma.drivinglog.vehicle.domain.VehicleDetails
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone

/** A device time zone tests can set. */
class FixedDeviceTimeZone(var zone: TimeZone = TimeZone.UTC) : DeviceTimeZone {
    override fun current(): TimeZone = zone
}

data class DistanceCall(val vehicleId: String, val occurredAt: ZonedMoment, val distance: Distance, val loggedOdometer: Distance?)

data class AddCall(val name: String, val licensePlate: String?, val unit: OdometerUnit, val initialOdometer: Distance)
data class UpdateCall(val id: String, val name: String, val licensePlate: String?)

/** An in-memory repository for processor tests. Events are kept newest first, as the real one returns them. */
class FakeVehicleRepository : VehicleRepository {
    private val vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    private val events = MutableStateFlow<Map<String, List<VehicleEvent>>>(emptyMap())
    private var counter = 0

    val addCalls = mutableListOf<AddCall>()
    val updateCalls = mutableListOf<UpdateCall>()
    val distanceCalls = mutableListOf<DistanceCall>()
    var distanceFailure: Throwable? = null
    var addFailure: Throwable? = null
    var updateFailure: Throwable? = null

    fun seedVehicle(
        id: String,
        name: String,
        plate: String? = null,
        unit: OdometerUnit = OdometerUnit.KILOMETERS,
        createdAtMillis: Long = counter++.toLong(),
    ) {
        vehicles.value += Vehicle(id, name, plate, unit, Instant.fromEpochMilliseconds(createdAtMillis))
    }

    /** Replaces the vehicle's events; [newestFirst] must already be in newest-first order. */
    fun seedEvents(vehicleId: String, newestFirst: List<VehicleEvent>) {
        events.value += vehicleId to newestFirst
    }

    fun eventsOf(vehicleId: String): List<VehicleEvent> = events.value[vehicleId].orEmpty()

    override fun observeVehicles(): Flow<List<Vehicle>> = vehicles

    override fun observeVehicle(id: String): Flow<VehicleDetails?> =
        vehicles.map { list ->
            list.firstOrNull { it.id == id }?.let { vehicle ->
                val current = events.value[id].orEmpty().firstNotNullOfOrNull { it.odometer }
                VehicleDetails(vehicle, current)
            }
        }

    override fun observeRecentEvents(vehicleId: String, limit: Int): Flow<List<VehicleEvent>> =
        events.map { it[vehicleId].orEmpty().take(limit) }

    override fun observeLog(vehicleId: String): Flow<List<VehicleEvent>> = events.map { it[vehicleId].orEmpty() }

    override suspend fun addVehicle(name: String, licensePlate: String?, unit: OdometerUnit, initialOdometer: Distance): String {
        addFailure?.let { throw it }
        addCalls += AddCall(name, licensePlate, unit, initialOdometer)
        val id = "v${++counter}"
        seedVehicle(id, name, licensePlate, unit)
        seedEvents(id, listOf(VehicleEvent.InitialOdometer("e$counter", ZonedMoment(Instant.fromEpochMilliseconds(counter.toLong())), initialOdometer)))
        return id
    }

    override suspend fun addDistanceEntry(vehicleId: String, occurredAt: ZonedMoment, distance: Distance, loggedOdometer: Distance?): String {
        distanceFailure?.let { throw it }
        distanceCalls += DistanceCall(vehicleId, occurredAt, distance, loggedOdometer)
        val id = "d${++counter}"
        // Keep the log newest first by instant, the way the real repository returns it.
        val entry = VehicleEvent.DistanceEntry(id, occurredAt, distance, loggedOdometer)
        val updated = (listOf(entry) + eventsOf(vehicleId)).sortedByDescending { it.occurredAt.instant }
        seedEvents(vehicleId, updated)
        return id
    }

    override suspend fun updateVehicle(id: String, name: String, licensePlate: String?) {
        updateFailure?.let { throw it }
        updateCalls += UpdateCall(id, name, licensePlate)
        vehicles.value = vehicles.value.map { if (it.id == id) it.copy(name = name, licensePlate = licensePlate) else it }
    }
}

fun initialEvent(id: String, atMillis: Long, meters: Long) =
    VehicleEvent.InitialOdometer(id, ZonedMoment(Instant.fromEpochMilliseconds(atMillis)), Distance(meters))

fun distanceEvent(id: String, atMillis: Long, meters: Long, loggedOdometer: Long? = null) =
    VehicleEvent.DistanceEntry(id, ZonedMoment(Instant.fromEpochMilliseconds(atMillis)), Distance(meters), loggedOdometer?.let { Distance(it) })
