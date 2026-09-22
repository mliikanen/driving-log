package com.mikonoma.drivinglog.vehicle

import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleColors
import com.mikonoma.drivinglog.vehicle.domain.VehicleType
import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.PendingPicture
import com.mikonoma.drivinglog.vehicle.domain.PictureChange
import com.mikonoma.drivinglog.vehicle.domain.Vehicle
import com.mikonoma.drivinglog.vehicle.domain.VehicleDetails
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import com.mikonoma.drivinglog.vehicle.domain.currentOdometer
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone

/** A device time zone tests can set. */
class FixedDeviceTimeZone(var zone: TimeZone = TimeZone.UTC) : DeviceTimeZone {
    override fun current(): TimeZone = zone
}

data class DistanceCall(
    val vehicleId: String,
    val occurredAt: ZonedMoment,
    val distance: Distance,
    val loggedOdometer: Distance?,
    val tenthsIncluded: Boolean,
    val note: String? = null,
)

data class AnchorCall(
    val vehicleId: String,
    val occurredAt: ZonedMoment,
    val reading: Distance,
    val tenthsIncluded: Boolean,
    val note: String? = null,
)

data class AddCall(
    val name: String,
    val licensePlate: String?,
    val type: VehicleType,
    val unit: OdometerUnit,
    val initialOdometer: Distance,
    val picture: PendingPicture? = null,
    val color: Rgb = VehicleColors.default,
)
data class UpdateCall(
    val id: String,
    val name: String,
    val licensePlate: String?,
    val type: VehicleType = VehicleType.CAR,
    val picture: PictureChange = PictureChange.Keep,
    val color: Rgb = VehicleColors.default,
)

/** An in-memory repository for processor tests. Events are kept newest first, as the real one returns them. */
class FakeVehicleRepository : VehicleRepository {
    private val vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    private val events = MutableStateFlow<Map<String, List<VehicleEvent>>>(emptyMap())
    private val lastLoggedVehicleId = MutableStateFlow<String?>(null)
    private var counter = 0

    val addCalls = mutableListOf<AddCall>()
    val updateCalls = mutableListOf<UpdateCall>()
    val distanceCalls = mutableListOf<DistanceCall>()
    val anchorCalls = mutableListOf<AnchorCall>()
    var distanceFailure: Throwable? = null
    var addFailure: Throwable? = null
    var updateFailure: Throwable? = null

    fun seedVehicle(
        id: String,
        name: String,
        plate: String? = null,
        unit: OdometerUnit = OdometerUnit.KILOMETERS,
        createdAtMillis: Long = counter++.toLong(),
        logDistanceTenths: Boolean? = null,
        pictureId: String? = null,
        type: VehicleType = VehicleType.CAR,
        color: Rgb = VehicleColors.default,
    ) {
        vehicles.value += Vehicle(id, name, plate, unit, Instant.fromEpochMilliseconds(createdAtMillis), logDistanceTenths, pictureId, type, color)
    }

    /** Replaces the vehicle's events; [newestFirst] must already be in newest-first order. */
    fun seedEvents(vehicleId: String, newestFirst: List<VehicleEvent>) {
        events.value += vehicleId to newestFirst
    }

    /** Changes a seeded vehicle's type, as another screen saving it would. */
    fun setColor(id: String, color: Rgb) {
        vehicles.value = vehicles.value.map { if (it.id == id) it.copy(color = color) else it }
    }

    fun setType(id: String, type: VehicleType) {
        vehicles.value = vehicles.value.map { if (it.id == id) it.copy(type = type) else it }
    }

    /** Changes a seeded vehicle's picture id, as another screen saving it would. */
    fun setPicture(id: String, pictureId: String?) {
        vehicles.value = vehicles.value.map { if (it.id == id) it.copy(pictureId = pictureId) else it }
    }

    fun observeVehicleOdometer(vehicleId: String): Distance? = currentOdometer(eventsOf(vehicleId).asReversed())

    /** Sets the remembered vehicle directly, as a database seeded from an earlier run would have it (no entry saved in this test). */
    fun seedLastLoggedVehicleId(id: String?) { lastLoggedVehicleId.value = id }

    fun eventsOf(vehicleId: String): List<VehicleEvent> = events.value[vehicleId].orEmpty()

    override fun observeVehicles(): Flow<List<Vehicle>> = vehicles

    override fun observeVehicle(id: String): Flow<VehicleDetails?> =
        vehicles.map { list ->
            list.firstOrNull { it.id == id }?.let { vehicle ->
                // Events are kept newest first; the current odometer is derived from them oldest first, like the real one.
                VehicleDetails(vehicle, currentOdometer(events.value[id].orEmpty().asReversed()))
            }
        }

    override fun observeRecentEvents(vehicleId: String, limit: Int): Flow<List<VehicleEvent>> =
        events.map { it[vehicleId].orEmpty().take(limit) }

    override fun observeLog(vehicleId: String): Flow<List<VehicleEvent>> = events.map { it[vehicleId].orEmpty() }

    override fun observeLastLoggedVehicleId(): Flow<String?> = lastLoggedVehicleId

    override suspend fun addVehicle(
        name: String,
        licensePlate: String?,
        type: VehicleType,
        color: Rgb,
        unit: OdometerUnit,
        initialOdometer: Distance,
        picture: PendingPicture?,
    ): String {
        addFailure?.let { throw it }
        addCalls += AddCall(name, licensePlate, type, unit, initialOdometer, picture, color)
        val id = "v${++counter}"
        seedVehicle(id, name, licensePlate, unit, type = type, color = color)
        seedEvents(id, listOf(VehicleEvent.InitialOdometer("e$counter", ZonedMoment(Instant.fromEpochMilliseconds(counter.toLong())), initialOdometer)))
        return id
    }

    override suspend fun addDistanceEntry(
        vehicleId: String,
        occurredAt: ZonedMoment,
        distance: Distance,
        loggedOdometer: Distance?,
        tenthsIncluded: Boolean,
        note: String?,
    ): String {
        distanceFailure?.let { throw it }
        distanceCalls += DistanceCall(vehicleId, occurredAt, distance, loggedOdometer, tenthsIncluded, note)
        // The choice is remembered with the entry, like the real repository does.
        vehicles.value = vehicles.value.map { if (it.id == vehicleId) it.copy(logDistanceTenths = tenthsIncluded) else it }
        lastLoggedVehicleId.value = vehicleId
        val id = "d${++counter}"
        // Keep the log newest first by instant, the way the real repository returns it.
        val entry = VehicleEvent.DistanceEntry(id, occurredAt, distance, loggedOdometer, note)
        val updated = (listOf(entry) + eventsOf(vehicleId)).sortedByDescending { it.occurredAt.instant }
        seedEvents(vehicleId, updated)
        return id
    }

    override suspend fun addOdometerAnchor(
        vehicleId: String,
        occurredAt: ZonedMoment,
        reading: Distance,
        tenthsIncluded: Boolean,
        note: String?,
    ): String {
        distanceFailure?.let { throw it }
        anchorCalls += AnchorCall(vehicleId, occurredAt, reading, tenthsIncluded, note)
        vehicles.value = vehicles.value.map { if (it.id == vehicleId) it.copy(logDistanceTenths = tenthsIncluded) else it }
        lastLoggedVehicleId.value = vehicleId
        val id = "a${++counter}"
        val anchor = VehicleEvent.OdometerAnchor(id, occurredAt, reading, note)
        seedEvents(vehicleId, (listOf(anchor) + eventsOf(vehicleId)).sortedByDescending { it.occurredAt.instant })
        return id
    }

    override suspend fun updateVehicle(id: String, name: String, licensePlate: String?, type: VehicleType, color: Rgb, picture: PictureChange) {
        updateFailure?.let { throw it }
        updateCalls += UpdateCall(id, name, licensePlate, type, picture, color)
        vehicles.value = vehicles.value.map { if (it.id == id) it.copy(name = name, licensePlate = licensePlate, type = type, color = color) else it }
    }
}

fun initialEvent(id: String, atMillis: Long, meters: Long) =
    VehicleEvent.InitialOdometer(id, ZonedMoment(Instant.fromEpochMilliseconds(atMillis)), Distance(meters))

fun distanceEvent(id: String, atMillis: Long, meters: Long, loggedOdometer: Long? = null) =
    VehicleEvent.DistanceEntry(id, ZonedMoment(Instant.fromEpochMilliseconds(atMillis)), Distance(meters), loggedOdometer?.let { Distance(it) })

fun anchorEvent(id: String, atMillis: Long, meters: Long) =
    VehicleEvent.OdometerAnchor(id, ZonedMoment(Instant.fromEpochMilliseconds(atMillis)), Distance(meters))
