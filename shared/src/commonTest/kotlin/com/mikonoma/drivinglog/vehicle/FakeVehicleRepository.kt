package com.mikonoma.drivinglog.vehicle

import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.EntrySource
import com.mikonoma.drivinglog.vehicle.domain.FuelType
import com.mikonoma.drivinglog.vehicle.domain.FuelUnit
import com.mikonoma.drivinglog.vehicle.domain.LogEntry
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
import com.mikonoma.drivinglog.vehicle.domain.currentOdometer
import com.mikonoma.drivinglog.vehicle.domain.inLogOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

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
    val photos: List<PendingPicture> = emptyList(),
    val capture: PendingCapture? = null,
)

data class AnchorCall(
    val vehicleId: String,
    val occurredAt: ZonedMoment,
    val reading: Distance,
    val tenthsIncluded: Boolean,
    val note: String? = null,
    val photos: List<PendingPicture> = emptyList(),
    val capture: PendingCapture? = null,
)

data class RefuelingCall(
    val vehicleId: String,
    val occurredAt: ZonedMoment,
    val amount: Volume,
    val unit: FuelUnit,
    val fuelType: FuelType,
    val filledUp: Boolean,
    val mileage: RefuelingMileage? = null,
    val tenthsIncluded: Boolean = false,
    val note: String? = null,
    val photos: List<PendingPicture> = emptyList(),
    val capture: PendingCapture? = null,
)

data class AddEventPhotoCall(val vehicleId: String, val eventId: String, val photo: PendingPicture)
data class RemoveEventPhotoCall(val vehicleId: String, val eventId: String, val pictureId: String)

data class AddCall(
    val name: String,
    val licensePlate: String?,
    val type: VehicleType,
    val unit: OdometerUnit,
    val initialOdometer: Distance,
    val picture: PendingPicture? = null,
    val color: Rgb = VehicleColors.default,
    val capture: PendingCapture? = null,
    val fuelType: VehicleFuelType = VehicleFuelType.PETROL,
)
data class UpdateCall(
    val id: String,
    val name: String,
    val licensePlate: String?,
    val type: VehicleType = VehicleType.CAR,
    val picture: PictureChange = PictureChange.Keep,
    val color: Rgb = VehicleColors.default,
    val fuelType: VehicleFuelType = VehicleFuelType.PETROL,
)

data class UpdateNoteCall(val vehicleId: String, val eventId: String, val note: String?)

/**
 * An in-memory repository for processor tests. Events are kept in the order they were added and returned newest first in
 * log order ([inLogOrder] reversed), as the real one returns them.
 */
class FakeVehicleRepository : VehicleRepository {
    private val vehicles = MutableStateFlow<List<Vehicle>>(emptyList())

    /** Each vehicle's events in the order they were added. */
    private val events = MutableStateFlow<Map<String, List<VehicleEvent>>>(emptyMap())
    private val lastLoggedVehicleId = MutableStateFlow<String?>(null)
    private val lastFuelUnit = MutableStateFlow<FuelUnit?>(null)
    private val lastFuelType = MutableStateFlow<FuelType?>(null)
    private var counter = 0

    val addCalls = mutableListOf<AddCall>()
    val updateCalls = mutableListOf<UpdateCall>()
    val updateNoteCalls = mutableListOf<UpdateNoteCall>()
    val distanceCalls = mutableListOf<DistanceCall>()
    val anchorCalls = mutableListOf<AnchorCall>()
    val refuelingCalls = mutableListOf<RefuelingCall>()
    val addEventPhotoCalls = mutableListOf<AddEventPhotoCall>()
    val removeEventPhotoCalls = mutableListOf<RemoveEventPhotoCall>()
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
        fuelType: VehicleFuelType = VehicleFuelType.PETROL,
    ) {
        vehicles.value += Vehicle(id, name, plate, unit, Instant.fromEpochMilliseconds(createdAtMillis), logDistanceTenths, pictureId, type, color, fuelType)
    }

    /**
     * Replaces the vehicle's events; [newestFirst] must already be in newest-first order: events at the same instant are taken
     * to have been added in the reverse of the order given, so the log lists them exactly as given.
     */
    fun seedEvents(vehicleId: String, newestFirst: List<VehicleEvent>) {
        events.value += vehicleId to newestFirst.asReversed()
    }

    private fun appendEvents(vehicleId: String, added: List<VehicleEvent>) {
        events.value += vehicleId to (events.value[vehicleId].orEmpty() + added)
    }

    /** Applies [change] to every event of the entry [eventId] belongs to: its note and photos are the entry's. */
    private fun changeEntryOf(vehicleId: String, eventId: String, change: (VehicleEvent) -> VehicleEvent) {
        val added = events.value[vehicleId].orEmpty()
        val entryId = added.firstOrNull { it.id == eventId }?.entryId ?: return
        events.value += vehicleId to added.map { if (it.entryId == entryId) change(it) else it }
    }

    /** Changes a seeded vehicle's type, as another screen saving it would. */
    fun setColor(id: String, color: Rgb) {
        vehicles.value = vehicles.value.map { if (it.id == id) it.copy(color = color) else it }
    }

    fun setType(id: String, type: VehicleType) {
        vehicles.value = vehicles.value.map { if (it.id == id) it.copy(type = type) else it }
    }

    /** Changes a seeded vehicle's fuel type, as another screen saving it would. */
    fun setFuelType(id: String, fuelType: VehicleFuelType) {
        vehicles.value = vehicles.value.map { if (it.id == id) it.copy(fuelType = fuelType) else it }
    }

    /** Changes a seeded vehicle's picture id, as another screen saving it would. */
    fun setPicture(id: String, pictureId: String?) {
        vehicles.value = vehicles.value.map { if (it.id == id) it.copy(pictureId = pictureId) else it }
    }

    fun observeVehicleOdometer(vehicleId: String): Distance? = currentOdometer(logOf(vehicleId))

    /** Sets the remembered vehicle directly, as a database seeded from an earlier run would have it (no entry saved in this test). */
    fun seedLastLoggedVehicleId(id: String?) {
        lastLoggedVehicleId.value = id
    }

    /** Sets the remembered fuel unit/type directly, as a database seeded from an earlier refueling would have them. */
    fun seedLastFuelUnit(unit: FuelUnit?) {
        lastFuelUnit.value = unit
    }
    fun seedLastFuelType(type: FuelType?) {
        lastFuelType.value = type
    }

    /** The vehicle's events newest first, as [observeLog] returns them. */
    fun eventsOf(vehicleId: String): List<VehicleEvent> = logOf(vehicleId).asReversed()

    /** The vehicle's events in log order, oldest first. */
    private fun logOf(vehicleId: String, all: Map<String, List<VehicleEvent>> = events.value): List<VehicleEvent> = all[vehicleId].orEmpty().inLogOrder()

    override fun observeVehicles(): Flow<List<Vehicle>> = vehicles

    override fun observeVehicle(id: String): Flow<VehicleDetails?> = vehicles.map { list ->
        list.firstOrNull { it.id == id }?.let { vehicle ->
            // The current odometer is derived from the events in log order, like the real one.
            VehicleDetails(vehicle, currentOdometer(logOf(id)))
        }
    }

    override fun observeRecentEvents(vehicleId: String, limit: Int): Flow<List<VehicleEvent>> = events.map { logOf(vehicleId, it).asReversed().take(limit) }

    override fun observeLog(vehicleId: String): Flow<List<VehicleEvent>> = events.map { logOf(vehicleId, it).asReversed() }

    override fun observeEntry(vehicleId: String, eventId: String): Flow<LogEntry?> = events.map { all ->
        val log = logOf(vehicleId, all)
        val entryId = log.firstOrNull { it.id == eventId }?.entryId ?: return@map null
        val entryEvents = log.filter { it.entryId == entryId }
        val first = entryEvents.first()
        LogEntry(entryId, first.source, entryEvents, first.note, first.photoIds)
    }

    override fun observeLastLoggedVehicleId(): Flow<String?> = lastLoggedVehicleId

    override fun observeLastFuelUnit(): Flow<FuelUnit?> = lastFuelUnit

    override fun observeLastFuelType(): Flow<FuelType?> = lastFuelType

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
    ): String {
        addFailure?.let { throw it }
        addCalls += AddCall(name, licensePlate, type, unit, initialOdometer, picture, color, capture, fuelType)
        val id = "v${++counter}"
        seedVehicle(id, name, licensePlate, unit, type = type, color = color, fuelType = fuelType)
        seedEvents(
            id,
            listOf(
                VehicleEvent.OdometerReading(
                    "e$counter",
                    ZonedMoment(Instant.fromEpochMilliseconds(counter.toLong())),
                    initialOdometer,
                    EntrySource.VEHICLE_ADDED,
                ),
            ),
        )
        return id
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
        distanceFailure?.let { throw it }
        distanceCalls += DistanceCall(vehicleId, occurredAt, distance, loggedOdometer, tenthsIncluded, note, photos, capture)
        // The choice is remembered with the entry, like the real repository does.
        vehicles.value = vehicles.value.map { if (it.id == vehicleId) it.copy(logDistanceTenths = tenthsIncluded) else it }
        lastLoggedVehicleId.value = vehicleId
        val id = "d${++counter}"
        appendEvents(vehicleId, listOf(VehicleEvent.DistanceEntry(id, occurredAt, distance, loggedOdometer, note, photos.map(::fakePhotoId))))
        return id
    }

    override suspend fun addOdometerAnchor(
        vehicleId: String,
        occurredAt: ZonedMoment,
        reading: Distance,
        tenthsIncluded: Boolean,
        note: String?,
        photos: List<PendingPicture>,
        capture: PendingCapture?,
    ): String {
        distanceFailure?.let { throw it }
        anchorCalls += AnchorCall(vehicleId, occurredAt, reading, tenthsIncluded, note, photos, capture)
        vehicles.value = vehicles.value.map { if (it.id == vehicleId) it.copy(logDistanceTenths = tenthsIncluded) else it }
        lastLoggedVehicleId.value = vehicleId
        val id = "a${++counter}"
        appendEvents(vehicleId, listOf(VehicleEvent.OdometerReading(id, occurredAt, reading, EntrySource.LOG_FORM, note, photos.map(::fakePhotoId))))
        return id
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
        distanceFailure?.let { throw it }
        refuelingCalls += RefuelingCall(vehicleId, occurredAt, amount, unit, fuelType, filledUp, mileage, tenthsIncluded, note, photos, capture)
        if (mileage != null) vehicles.value = vehicles.value.map { if (it.id == vehicleId) it.copy(logDistanceTenths = tenthsIncluded) else it }
        lastLoggedVehicleId.value = vehicleId
        lastFuelUnit.value = unit
        lastFuelType.value = fuelType
        // One entry (atomic-log-events): the refueling and, with mileage, the event that mileage is, sharing the note and photos.
        val id = "r${++counter}"
        val photoIds = photos.map(::fakePhotoId)
        val mileageEvent = when (mileage) {
            is RefuelingMileage.Added -> VehicleEvent.DistanceEntry(
                "$id-mileage",
                occurredAt,
                mileage.distance,
                mileage.loggedOdometer,
                note,
                photoIds,
                entryId = id,
            )

            is RefuelingMileage.Anchor -> VehicleEvent.OdometerReading(
                "$id-mileage",
                occurredAt,
                mileage.reading,
                EntrySource.LOG_FORM,
                note,
                photoIds,
                entryId = id,
            )

            null -> null
        }
        appendEvents(vehicleId, listOfNotNull(mileageEvent, VehicleEvent.Refueling(id, occurredAt, amount, unit, fuelType, filledUp, note, photoIds)))
        return id
    }

    private fun fakePhotoId(photo: PendingPicture) = "photo-${photo.pendingId}"

    override suspend fun captureOf(eventId: String): StoredCapture? = null

    override suspend fun capturePhotoIds(): Set<String> = emptySet()

    override suspend fun updateVehicle(
        id: String,
        name: String,
        licensePlate: String?,
        type: VehicleType,
        color: Rgb,
        picture: PictureChange,
        fuelType: VehicleFuelType,
    ) {
        updateFailure?.let { throw it }
        updateCalls += UpdateCall(id, name, licensePlate, type, picture, color, fuelType)
        vehicles.value =
            vehicles.value.map { if (it.id == id) it.copy(name = name, licensePlate = licensePlate, type = type, color = color, fuelType = fuelType) else it }
    }

    override suspend fun updateEventNote(vehicleId: String, eventId: String, note: String?) {
        updateNoteCalls += UpdateNoteCall(vehicleId, eventId, note)
        changeEntryOf(vehicleId, eventId) { it.withNoteAndPhotos(note = note) }
    }

    override suspend fun addEventPhoto(vehicleId: String, eventId: String, photo: PendingPicture): String {
        addEventPhotoCalls += AddEventPhotoCall(vehicleId, eventId, photo)
        val photoId = fakePhotoId(photo)
        changeEntryOf(vehicleId, eventId) { it.withNoteAndPhotos(photoIds = it.photoIds + photoId) }
        return photoId
    }

    override suspend fun removeEventPhoto(vehicleId: String, eventId: String, pictureId: String) {
        removeEventPhotoCalls += RemoveEventPhotoCall(vehicleId, eventId, pictureId)
        changeEntryOf(vehicleId, eventId) { it.withNoteAndPhotos(photoIds = it.photoIds - pictureId) }
    }

    private fun VehicleEvent.withNoteAndPhotos(note: String? = this.note, photoIds: List<String> = this.photoIds): VehicleEvent = when (this) {
        is VehicleEvent.DistanceEntry -> copy(note = note, photoIds = photoIds)
        is VehicleEvent.OdometerReading -> copy(note = note, photoIds = photoIds)
        is VehicleEvent.Refueling -> copy(note = note, photoIds = photoIds)
    }
}

fun initialEvent(id: String, atMillis: Long, meters: Long) =
    VehicleEvent.OdometerReading(id, ZonedMoment(Instant.fromEpochMilliseconds(atMillis)), Distance(meters), EntrySource.VEHICLE_ADDED)

fun distanceEvent(id: String, atMillis: Long, meters: Long, loggedOdometer: Long? = null, entryId: String = id) = VehicleEvent.DistanceEntry(
    id,
    ZonedMoment(Instant.fromEpochMilliseconds(atMillis)),
    Distance(meters),
    loggedOdometer?.let {
        Distance(it)
    },
    entryId = entryId,
)

fun anchorEvent(id: String, atMillis: Long, meters: Long, entryId: String = id) =
    VehicleEvent.OdometerReading(id, ZonedMoment(Instant.fromEpochMilliseconds(atMillis)), Distance(meters), entryId = entryId)

fun refuelingEvent(
    id: String,
    atMillis: Long,
    milliliters: Long,
    unit: FuelUnit = FuelUnit.LITERS,
    fuelType: FuelType = FuelType.REGULAR_PETROL,
    filledUp: Boolean = true,
    entryId: String = id,
) = VehicleEvent.Refueling(id, ZonedMoment(Instant.fromEpochMilliseconds(atMillis)), Volume(milliliters), unit, fuelType, filledUp, entryId = entryId)
