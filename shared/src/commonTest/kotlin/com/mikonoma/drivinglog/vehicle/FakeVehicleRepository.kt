package com.mikonoma.drivinglog.vehicle

import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.Vehicle
import com.mikonoma.drivinglog.vehicle.domain.VehicleDetails
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

data class AddCall(val name: String, val licensePlate: String?, val unit: OdometerUnit, val initialOdometer: Distance)
data class UpdateCall(val id: String, val name: String, val licensePlate: String?)

/** An in-memory repository for processor tests. Events are kept newest first, as the real one returns them. */
class FakeVehicleRepository : VehicleRepository {
    private val vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    private val events = MutableStateFlow<Map<String, List<VehicleEvent>>>(emptyMap())
    private var counter = 0

    val addCalls = mutableListOf<AddCall>()
    val updateCalls = mutableListOf<UpdateCall>()
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
        seedEvents(id, listOf(VehicleEvent.InitialOdometer("e$counter", Instant.fromEpochMilliseconds(counter.toLong()), initialOdometer)))
        return id
    }

    override suspend fun updateVehicle(id: String, name: String, licensePlate: String?) {
        updateFailure?.let { throw it }
        updateCalls += UpdateCall(id, name, licensePlate)
        vehicles.value = vehicles.value.map { if (it.id == id) it.copy(name = name, licensePlate = licensePlate) else it }
    }
}

fun initialEvent(id: String, atMillis: Long, meters: Long) =
    VehicleEvent.InitialOdometer(id, Instant.fromEpochMilliseconds(atMillis), Distance(meters))
