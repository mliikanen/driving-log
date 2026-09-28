package com.mikonoma.drivinglog.vehicle.data

import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleColors
import com.mikonoma.drivinglog.vehicle.domain.VehicleType
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.mikonoma.drivinglog.db.DrivingLogDatabase
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.EventZone
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.PendingPicture
import com.mikonoma.drivinglog.vehicle.domain.PictureChange
import com.mikonoma.drivinglog.vehicle.picture.FakePictureStore
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import com.mikonoma.drivinglog.vehicle.domain.currentOdometer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone

/** The default is on a whole minute, where a vehicle's initial event is dated exactly (see the minute precision tests). */
class FakeClock(var current: Instant = Instant.fromEpochMilliseconds(60_000)) : Clock {
    override fun now(): Instant = current
}

@OptIn(ExperimentalCoroutinesApi::class)
class SqlDelightVehicleRepositoryTest {

    private val driver: SqlDriver = createTestDriver()
    private val database = DrivingLogDatabase(driver)
    private val clock = FakeClock()
    private var idCounter = 0
    private val deviceTimeZone = com.mikonoma.drivinglog.vehicle.FixedDeviceTimeZone()
    private val pictureStore = FakePictureStore()
    private val repository = SqlDelightVehicleRepository(
        database = database,
        clock = clock,
        newId = { "id-${++idCounter}" },
        dispatcher = UnconfinedTestDispatcher(),
        deviceTimeZone = deviceTimeZone,
        pictures = pictureStore,
    )

    @AfterTest
    fun close() = driver.close()

    private fun insertEvent(id: String, vehicleId: String, type: String, at: Long, meters: Long?) =
        database.vehicleEventQueries.insertEvent(id, vehicleId, type, at, meters, at, null, null, null)

    private suspend fun addFamilyCar(unit: OdometerUnit = OdometerUnit.KILOMETERS, meters: Long = 45_200_000) =
        repository.addVehicle("Family car", "ABC-123", VehicleType.CAR, VehicleColors.default, unit, Distance(meters))

    @Test
    fun addCreatesTheVehicleAndOneInitialEvent() = runTest {
        val id = addFamilyCar()

        val vehicle = repository.observeVehicles().first().single()
        assertEquals(id, vehicle.id)
        assertEquals("Family car", vehicle.name)
        assertEquals("ABC-123", vehicle.licensePlate)

        val log = repository.observeLog(id).first()
        val event = log.single() as VehicleEvent.InitialOdometer
        assertEquals(Distance(45_200_000), event.reading)
        assertEquals(clock.current, event.occurredAt.instant)
    }

    // ---- Minute precision: timestamps a user defines have no seconds

    @Test
    fun theInitialEventIsDatedToTheMinuteAndTheCreatedTimeIsExact() = runTest {
        clock.current = Instant.parse("2026-09-20T12:00:40.123Z")
        val id = addFamilyCar()

        val event = repository.observeLog(id).first().single()
        assertEquals(Instant.parse("2026-09-20T12:00:00Z"), event.occurredAt.instant)
        assertEquals(clock.current, repository.observeVehicles().first().single().createdAt)
    }

    @Test
    fun aDistanceEntryAndAnAnchorAreStoredToTheMinuteWhateverTheCallerPasses() = runTest {
        val id = vehicleAtNoon()
        val entry = addEntry(id, ZonedMoment.of(noon + 90.minutes + 45.seconds + 7.milliseconds, helsinki), Distance(1_000), null)
        val anchor = repository.addOdometerAnchor(id, ZonedMoment.of(noon - 1.hours + 59.seconds, helsinki), Distance(44_000_000), false)

        val log = repository.observeLog(id).first().associateBy { it.id }
        assertEquals(noon + 90.minutes, log.getValue(entry).occurredAt.instant)
        assertEquals(noon - 1.hours, log.getValue(anchor).occurredAt.instant)
        // The zone it was entered in is kept.
        assertEquals("Europe/Helsinki", log.getValue(entry).occurredAt.zone?.id)
    }

    @Test
    fun anEntryInTheMinuteTheVehicleWasAddedCounts() = runTest {
        clock.current = Instant.parse("2026-09-20T12:00:40Z")
        val id = addFamilyCar()
        // The form offers the time it was opened at, to the minute: 12:00.
        addEntry(id, ZonedMoment.of(Instant.parse("2026-09-20T12:00:00Z"), helsinki), Distance(30_000), null)

        assertCurrentOdometer(id, 45_230_000)
    }

    @Test
    fun addWithoutAPlateStoresNone() = runTest {
        repository.addVehicle("Van", null, VehicleType.CAR, VehicleColors.default, OdometerUnit.MILES, Distance.ZERO)
        assertNull(repository.observeVehicles().first().single().licensePlate)
    }

    @Test
    fun defaultOdometerOfZeroIsLogged() = runTest {
        val id = repository.addVehicle("Van", null, VehicleType.CAR, VehicleColors.default, OdometerUnit.MILES, Distance.ZERO)
        assertEquals(Distance.ZERO, repository.observeVehicle(id).first()?.currentOdometer)
        assertEquals(Distance.ZERO, (repository.observeLog(id).first().single() as VehicleEvent.InitialOdometer).reading)
    }

    @Test
    fun theUnitIsStoredAndReturnedForEveryUnit() = runTest {
        for (unit in OdometerUnit.entries) {
            val id = repository.addVehicle(unit.name, null, VehicleType.CAR, VehicleColors.default, unit, Distance.ZERO)
            assertEquals(unit, repository.observeVehicle(id).first()?.vehicle?.odometerUnit, unit.name)
        }
    }

    @Test
    fun aTenthsReadingIsKeptExactlyInMeters() = runTest {
        val id = addFamilyCar(OdometerUnit.KILOMETERS_TENTHS, 45_200_300)
        assertEquals(Distance(45_200_300), repository.observeVehicle(id).first()?.currentOdometer)
    }

    @Test
    fun aFailedAddCreatesNothing() = runTest {
        // Make the event insert fail: its id ("id-2") is already taken by another vehicle's event.
        database.vehicleQueries.insertVehicle("other", "Other", null, "KILOMETERS", 1, 1, null, "CAR", "203A43")
        insertEvent("id-2", "other", "INITIAL_ODOMETER", 1, 0)

        assertFails { addFamilyCar() }

        assertEquals(listOf("other"), repository.observeVehicles().first().map { it.id })
        assertEquals(1, repository.observeLog("other").first().size)
        assertEquals(emptyList(), repository.observeLog("id-1").first())
    }

    @Test
    fun theCurrentOdometerComesFromANewerEvent() = runTest {
        val id = addFamilyCar()
        insertEvent("later", id, "INITIAL_ODOMETER", clock.current.toEpochMilliseconds() + 10, 45_900_000)
        assertEquals(Distance(45_900_000), repository.observeVehicle(id).first()?.currentOdometer)
    }

    @Test
    fun aNewerEventWithoutAReadingDoesNotHideTheOdometer() = runTest {
        val id = addFamilyCar()
        insertEvent("note", id, "INITIAL_ODOMETER", clock.current.toEpochMilliseconds() + 10, null)
        // Rows with no reading are only a storage possibility for future event types; the derived odometer skips them.
        assertEquals(Distance(45_200_000), repository.observeVehicle(id).first()?.currentOdometer)
    }

    @Test
    fun aMissingVehicleIsNull() = runTest {
        assertNull(repository.observeVehicle("nope").first())
    }

    @Test
    fun theFiveNewestOfSevenEventsAreRecent() = runTest {
        val id = addFamilyCar()
        val start = clock.current.toEpochMilliseconds()
        for (i in 1..6) insertEvent("e$i", id, "INITIAL_ODOMETER", start + i, 45_200_000L + i * 1_000)

        val recent = repository.observeRecentEvents(id, 5).first()
        assertEquals(listOf("e6", "e5", "e4", "e3", "e2"), recent.map { it.id })
        assertEquals(7, repository.observeLog(id).first().size)
    }

    @Test
    fun theFullLogIsNewestFirst() = runTest {
        val id = addFamilyCar()
        val start = clock.current.toEpochMilliseconds()
        insertEvent("older", id, "INITIAL_ODOMETER", start - 5, 1)
        insertEvent("newer", id, "INITIAL_ODOMETER", start + 5, 2)

        assertEquals(listOf("newer", "id-2", "older"), repository.observeLog(id).first().map { it.id })
    }

    @Test
    fun eventsWithTheSameTimeComeLastAddedFirst() = runTest {
        val id = addFamilyCar()
        val at = clock.current.toEpochMilliseconds()
        insertEvent("first-added", id, "INITIAL_ODOMETER", at, 1)
        insertEvent("second-added", id, "INITIAL_ODOMETER", at, 2)

        assertEquals(
            listOf("second-added", "first-added", "id-2"),
            repository.observeRecentEvents(id, 5).first().map { it.id },
        )
        assertEquals(Distance(2), repository.observeVehicle(id).first()?.currentOdometer)
    }

    @Test
    fun editChangesNameAndPlateAndLeavesTheLogAndUnitUntouched() = runTest {
        val id = addFamilyCar(OdometerUnit.MILES_TENTHS, 1_609_344)
        val logBefore = repository.observeLog(id).first()
        clock.current = Instant.fromEpochMilliseconds(5_000)

        repository.updateVehicle(id, "Estate car", "XYZ-789", VehicleType.CAR, VehicleColors.default)

        val vehicle = repository.observeVehicle(id).first()!!
        assertEquals("Estate car", vehicle.vehicle.name)
        assertEquals("XYZ-789", vehicle.vehicle.licensePlate)
        assertEquals(OdometerUnit.MILES_TENTHS, vehicle.vehicle.odometerUnit)
        assertEquals(Distance(1_609_344), vehicle.currentOdometer)
        assertEquals(logBefore, repository.observeLog(id).first())
    }

    @Test
    fun theEditedPlateCanBeClearedToNone() = runTest {
        val id = addFamilyCar()
        repository.updateVehicle(id, "Family car", null, VehicleType.CAR, VehicleColors.default)
        assertNull(repository.observeVehicle(id).first()?.vehicle?.licensePlate)
    }

    @Test
    fun anUnknownEventTypeIsSkipped() = runTest {
        val id = addFamilyCar()
        insertEvent("future", id, "REFUELING", clock.current.toEpochMilliseconds() + 1, 46_000_000)

        assertEquals(listOf("id-2"), repository.observeLog(id).first().map { it.id })
    }

    @Test
    fun theVehicleListUpdatesWhenAVehicleIsAdded() = runTest {
        val emissions = mutableListOf<Int>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeVehicles().collect { emissions += it.size }
        }
        assertEquals(listOf(0), emissions)

        addFamilyCar()
        assertEquals(listOf(0, 1), emissions)
    }

    @Test
    fun theVehicleDetailsUpdateWhenTheVehicleIsEdited() = runTest {
        val id = addFamilyCar()
        val names = mutableListOf<String?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeVehicle(id).collect { names += it?.vehicle?.name }
        }
        repository.updateVehicle(id, "Estate car", null, VehicleType.CAR, VehicleColors.default)
        assertEquals(listOf<String?>("Family car", "Estate car"), names)
    }

    @Test
    fun editingAVehicleKeepsItsUnitItsOdometerAndItsLog() = runTest {
        val id = addFamilyCar(OdometerUnit.MILES_TENTHS, 45_200_300)
        val before = repository.observeVehicle(id).first()
        val eventsBefore = repository.observeLog(id).first()

        repository.updateVehicle(id, "Estate car", "XYZ-789", VehicleType.VAN, VehicleColors.default)

        val after = repository.observeVehicle(id).first()
        assertEquals(OdometerUnit.MILES_TENTHS, after?.vehicle?.odometerUnit)
        assertEquals(before?.currentOdometer, after?.currentOdometer)
        assertEquals(eventsBefore, repository.observeLog(id).first())
    }

    // ---- Distance entries, time zones and the derived odometer

    private val noon = Instant.parse("2026-09-20T12:00:00Z")
    private val helsinki = TimeZone.of("Europe/Helsinki")
    private val newYork = TimeZone.of("America/New_York")

    private fun at(delta: kotlin.time.Duration, zone: TimeZone = TimeZone.UTC) = ZonedMoment.of(noon + delta, zone)

    /** A vehicle whose initial odometer (45 200 km) happens at noon UTC. */
    private suspend fun vehicleAtNoon(): String {
        clock.current = noon
        return repository.addVehicle("Family car", null, VehicleType.CAR, VehicleColors.default, OdometerUnit.KILOMETERS, Distance(45_200_000))
    }

    /** Adds a distance entry; the tenths choice only matters to the tests about it. */
    private suspend fun addEntry(
        vehicleId: String,
        occurredAt: ZonedMoment,
        distance: Distance,
        loggedOdometer: Distance?,
        tenthsIncluded: Boolean = false,
    ) = repository.addDistanceEntry(vehicleId, occurredAt, distance, loggedOdometer, tenthsIncluded)

    private suspend fun assertCurrentOdometer(id: String, meters: Long) {
        val fromSql = repository.observeVehicle(id).first()?.currentOdometer
        assertEquals(Distance(meters), fromSql)
        // The SQL derivation and the pure function must agree on the same log.
        assertEquals(currentOdometer(repository.observeLog(id).first().reversed()), fromSql)
    }

    @Test
    fun theSchemaIsVersionTen() {
        assertEquals(10L, DrivingLogDatabase.Schema.version)
    }

    @Test
    fun theInitialEventTakesTheDeviceZoneAtTheTimeOfAdding() = runTest {
        deviceTimeZone.zone = helsinki
        val id = vehicleAtNoon()

        val event = repository.observeLog(id).first().single()

        assertEquals(EventZone("Europe/Helsinki", 3 * 3600), event.occurredAt.zone)
        assertEquals(noon, event.occurredAt.instant)
    }

    @Test
    fun theInitialEventFollowsTheDeviceZoneOfTheDayItIsAdded() = runTest {
        deviceTimeZone.zone = newYork
        val id = vehicleAtNoon()
        assertEquals(EventZone("America/New_York", -4 * 3600), repository.observeLog(id).first().single().occurredAt.zone)
    }

    @Test
    fun aDistanceEntryIsAddedAndReadBackWithItsZone() = runTest {
        val id = vehicleAtNoon()
        val moment = at(2.hours, newYork)

        val entryId = addEntry(id, moment, Distance(30_000), null)

        val entry = repository.observeLog(id).first().first() as VehicleEvent.DistanceEntry
        assertEquals(entryId, entry.id)
        assertEquals(Distance(30_000), entry.distance)
        assertEquals(null, entry.loggedOdometer)
        assertEquals(moment, entry.occurredAt)
        assertEquals(EventZone("America/New_York", -4 * 3600), entry.occurredAt.zone)
    }

    @Test
    fun aDistanceEntryWithANoteIsReadBackWithIt() = runTest {
        val id = vehicleAtNoon()

        repository.addDistanceEntry(id, at(1.hours), Distance(30_000), loggedOdometer = null, tenthsIncluded = false, note = "borrowed to Sam")

        val entry = repository.observeLog(id).first().first() as VehicleEvent.DistanceEntry
        assertEquals("borrowed to Sam", entry.note)
    }

    @Test
    fun aDistanceEntryWithoutANoteReadsBackNull() = runTest {
        val id = vehicleAtNoon()

        addEntry(id, at(1.hours), Distance(30_000), null)

        val entry = repository.observeLog(id).first().first() as VehicleEvent.DistanceEntry
        assertNull(entry.note)
    }

    @Test
    fun aDistanceEntryWithPhotosIsReadBackWithThemInAttachOrder() = runTest {
        val id = vehicleAtNoon()
        val first = PendingPicture(pictureStore.addPending())
        val second = PendingPicture(pictureStore.addPending())

        repository.addDistanceEntry(id, at(1.hours), Distance(30_000), null, tenthsIncluded = false, photos = listOf(first, second))

        val entry = repository.observeLog(id).first().first() as VehicleEvent.DistanceEntry
        assertEquals(2, entry.photoIds.size)
        assertTrue(entry.photoIds[0] in pictureStore.pictures)
        assertTrue(entry.photoIds[1] in pictureStore.pictures)
        assertNotEquals(entry.photoIds[0], entry.photoIds[1])
    }

    @Test
    fun aDistanceEntryWithoutPhotosReadsBackEmpty() = runTest {
        val id = vehicleAtNoon()

        addEntry(id, at(1.hours), Distance(30_000), null)

        val entry = repository.observeLog(id).first().first() as VehicleEvent.DistanceEntry
        assertTrue(entry.photoIds.isEmpty())
    }

    @Test
    fun aFailingDiskFailsTheDistanceEntrySaveAndSavesNothing() = runTest {
        val id = vehicleAtNoon()
        val pending = pictureStore.addPending()
        pictureStore.promoteFailure = IllegalStateException("disk full")

        assertFails {
            repository.addDistanceEntry(id, at(1.hours), Distance(30_000), null, tenthsIncluded = false, photos = listOf(PendingPicture(pending)))
        }

        assertEquals(1, repository.observeLog(id).first().size) // only the initial odometer event
    }

    @Test
    fun eventPhotosUseTheSeparateEventPictureStoreNotTheVehicleOne() = runTest {
        // A repository with two distinct store instances, as AppGraph.kt wires them (add-event-pictures).
        val eventPictureStore = FakePictureStore()
        val repositoryWithSeparateStores = SqlDelightVehicleRepository(
            database = database,
            clock = clock,
            newId = { "id-${++idCounter}" },
            dispatcher = UnconfinedTestDispatcher(),
            deviceTimeZone = deviceTimeZone,
            pictures = pictureStore,
            eventPictures = eventPictureStore,
        )
        val id = repositoryWithSeparateStores.addVehicle(
            "Family car", null, VehicleType.CAR, VehicleColors.default, OdometerUnit.KILOMETERS, Distance(45_200_000),
        )
        val eventPhoto = PendingPicture(eventPictureStore.addPending())

        val eventId = repositoryWithSeparateStores.addDistanceEntry(
            id, ZonedMoment.of(clock.current, TimeZone.UTC), Distance(30_000), null, tenthsIncluded = false, photos = listOf(eventPhoto),
        )

        val entry = repositoryWithSeparateStores.observeLog(id).first().first { it.id == eventId } as VehicleEvent.DistanceEntry
        val photoId = entry.photoIds.single()
        assertTrue(photoId in eventPictureStore.pictures)
        assertTrue(photoId !in pictureStore.pictures)
        // Not derived from the owning event's or vehicle's own id — a fresh, independent id (design.md).
        assertFalse(photoId.contains(eventId))
        assertFalse(photoId.contains(id))
    }

    // ---- The scan an entry's number came from (add-odometer-ocr-capture)

    private val captureStore = com.mikonoma.drivinglog.vehicle.ocr.FakeCaptureStore()
    private val repositoryWithCaptures = SqlDelightVehicleRepository(
        database = database,
        clock = clock,
        newId = { "id-${++idCounter}" },
        dispatcher = UnconfinedTestDispatcher(),
        deviceTimeZone = deviceTimeZone,
        pictures = pictureStore,
        captures = captureStore,
    )

    private fun scan(accepted: Int = 0) = com.mikonoma.drivinglog.vehicle.ocr.ScanResult(
        1280, 720,
        listOf(
            com.mikonoma.drivinglog.vehicle.ocr.Detection(
                "71140km", "71140", com.mikonoma.drivinglog.vehicle.ocr.TextBox(529, 412, 619, 433),
                com.mikonoma.drivinglog.vehicle.ocr.ReadingKind.ODOMETER, com.mikonoma.drivinglog.vehicle.ocr.DetectionBasis.LABEL, "ODO",
            ),
            com.mikonoma.drivinglog.vehicle.ocr.Detection(
                "917", "917", com.mikonoma.drivinglog.vehicle.ocr.TextBox(445, 246, 481, 264),
                com.mikonoma.drivinglog.vehicle.ocr.ReadingKind.TRIP, com.mikonoma.drivinglog.vehicle.ocr.DetectionBasis.MAGNITUDE,
            ),
        ),
        accepted,
    )

    @Test
    fun aScannedEntryIsSavedWithItsPhotoAndEveryDetection() = runTest {
        val id = addFamilyCar()
        val capture = com.mikonoma.drivinglog.vehicle.domain.PendingCapture(captureStore.addPending(), scan())

        val eventId = repositoryWithCaptures.addDistanceEntry(
            id, ZonedMoment.of(clock.current, TimeZone.UTC), Distance(30_000), null, tenthsIncluded = false, capture = capture,
        )

        val stored = assertNotNull(repositoryWithCaptures.captureOf(eventId))
        assertEquals(scan(), stored.result)
        assertTrue(stored.photoId in captureStore.photos)
        assertTrue(captureStore.pending.isEmpty())
        assertEquals(setOf(stored.photoId), repositoryWithCaptures.capturePhotoIds())
    }

    @Test
    fun aScannedOdometerAnchorIsSavedWithItsScan() = runTest {
        val id = addFamilyCar()
        val capture = com.mikonoma.drivinglog.vehicle.domain.PendingCapture(captureStore.addPending(), scan())

        val eventId = repositoryWithCaptures.addOdometerAnchor(
            id, ZonedMoment.of(clock.current, TimeZone.UTC), Distance(71_140_000), tenthsIncluded = false, capture = capture,
        )

        assertEquals("71140", assertNotNull(repositoryWithCaptures.captureOf(eventId)).result.accepted.value)
    }

    // ---- The scan a new vehicle's initial odometer came from (scan-initial-odometer)

    @Test
    fun aScannedInitialOdometerIsSavedWithTheInitialEvent() = runTest {
        val capture = com.mikonoma.drivinglog.vehicle.domain.PendingCapture(captureStore.addPending(), scan())

        val id = repositoryWithCaptures.addVehicle(
            "Family car", null, VehicleType.CAR, VehicleColors.default, OdometerUnit.KILOMETERS, Distance(71_140_000), capture = capture,
        )

        val initial = repositoryWithCaptures.observeLog(id).first().single() as VehicleEvent.InitialOdometer
        val stored = assertNotNull(repositoryWithCaptures.captureOf(initial.id))
        assertEquals("71140", stored.result.accepted.value)
        assertTrue(stored.photoId in captureStore.photos)
        assertTrue(captureStore.pending.isEmpty())
    }

    @Test
    fun aTypedInitialOdometerKeepsNoScan() = runTest {
        val id = repositoryWithCaptures.addVehicle("Family car", null, VehicleType.CAR, VehicleColors.default, OdometerUnit.KILOMETERS, Distance(71_140_000))

        val initial = repositoryWithCaptures.observeLog(id).first().single()
        assertNull(repositoryWithCaptures.captureOf(initial.id))
    }

    @Test
    fun aFailedVehicleSaveLeavesNoScanPhotoBehind() = runTest {
        val capture = com.mikonoma.drivinglog.vehicle.domain.PendingCapture(captureStore.addPending(), scan())
        // The same vehicle id twice: the second insert fails inside the transaction.
        idCounter = 0
        repositoryWithCaptures.addVehicle("First", null, VehicleType.CAR, VehicleColors.default, OdometerUnit.KILOMETERS, Distance(1_000_000))
        idCounter = 0

        assertFails {
            repositoryWithCaptures.addVehicle("Second", null, VehicleType.CAR, VehicleColors.default, OdometerUnit.KILOMETERS, Distance(71_140_000), capture = capture)
        }

        assertTrue(captureStore.photos.isEmpty())
    }

    /** A capture outlives its event: removing the event neither removes the capture nor its photo (the sweep keeps it). */
    @Test
    fun aScanIsKeptWhenItsEventIsRemoved() = runTest {
        val id = addFamilyCar()
        val capture = com.mikonoma.drivinglog.vehicle.domain.PendingCapture(captureStore.addPending(), scan())
        val eventId = repositoryWithCaptures.addDistanceEntry(
            id, ZonedMoment.of(clock.current, TimeZone.UTC), Distance(30_000), null, tenthsIncluded = false, capture = capture,
        )
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)

        // No event can be removed from the app yet; a later change that adds it must not reach the capture.
        driver.execute(null, "DELETE FROM vehicle_event WHERE id = '$eventId'", 0)
        com.mikonoma.drivinglog.vehicle.ocr.sweepCaptures(repositoryWithCaptures, captureStore)

        val stored = assertNotNull(repositoryWithCaptures.captureOf(eventId))
        assertTrue(stored.photoId in captureStore.photos)
    }

    @Test
    fun aTypedEntryHasNoScan() = runTest {
        val id = addFamilyCar()

        val eventId = repositoryWithCaptures.addDistanceEntry(id, ZonedMoment.of(clock.current, TimeZone.UTC), Distance(30_000), null, tenthsIncluded = false)

        assertNull(repositoryWithCaptures.captureOf(eventId))
        assertEquals(emptySet(), repositoryWithCaptures.capturePhotoIds())
    }

    @Test
    fun aScanWhosePhotoIsGoneSavesNothing() = runTest {
        val id = addFamilyCar()
        val capture = com.mikonoma.drivinglog.vehicle.domain.PendingCapture("capture-pending-gone", scan())

        assertFails {
            repositoryWithCaptures.addDistanceEntry(id, ZonedMoment.of(clock.current, TimeZone.UTC), Distance(30_000), null, tenthsIncluded = false, capture = capture)
        }

        assertEquals(1, repositoryWithCaptures.observeLog(id).first().size)
    }

    @Test
    fun aFailedSaveLeavesNoScanPhotoBehind() = runTest {
        val id = addFamilyCar()
        val capture = com.mikonoma.drivinglog.vehicle.domain.PendingCapture(captureStore.addPending(), scan())
        // An entry of zero is refused before anything is written; an unknown vehicle fails inside the transaction instead.
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)

        assertFails {
            repositoryWithCaptures.addDistanceEntry("no-such-vehicle", ZonedMoment.of(clock.current, TimeZone.UTC), Distance(30_000), null, tenthsIncluded = false, capture = capture)
        }

        assertTrue(captureStore.photos.isEmpty())
    }

    @Test
    fun anEntryLoggedByOdometerKeepsTheTypedCount() = runTest {
        val id = vehicleAtNoon()

        addEntry(id, at(1.hours), Distance(50_000), loggedOdometer = Distance(45_250_000))

        val entry = repository.observeLog(id).first().first() as VehicleEvent.DistanceEntry
        assertEquals(Distance(50_000), entry.distance)
        assertEquals(Distance(45_250_000), entry.loggedOdometer)
        // The typed count is provenance only: the odometer is still the baseline plus the distance.
        assertCurrentOdometer(id, 45_250_000)
    }

    @Test
    fun aZoneTheDeviceDoesNotKnowIsStoredAndReadBackExactly() = runTest {
        val id = vehicleAtNoon()
        val moment = ZonedMoment(noon + 1.hours, EventZone("Mars/Olympus_Mons", 5 * 3600 + 1800))

        addEntry(id, moment, Distance(1_000), null)

        assertEquals(moment, repository.observeLog(id).first().first().occurredAt)
    }

    @Test
    fun aMomentWithoutAZoneIsStoredWithoutOne() = runTest {
        val id = vehicleAtNoon()
        addEntry(id, ZonedMoment(noon + 1.hours), Distance(1_000), null)
        assertNull(repository.observeLog(id).first().first().occurredAt.zone)
    }

    @Test
    fun aLegacyEventWithoutAZoneReadsBackWithoutOne() = runTest {
        val id = vehicleAtNoon()
        insertEvent("legacy", id, "INITIAL_ODOMETER", noon.toEpochMilliseconds() - 1, 5)
        assertNull(repository.observeLog(id).first().last().occurredAt.zone)
    }

    @Test
    fun aDistanceEntryMustBeAboveZero() = runTest {
        val id = vehicleAtNoon()
        assertFails { addEntry(id, at(1.hours), Distance.ZERO, null) }
        assertEquals(1, repository.observeLog(id).first().size)
    }

    @Test
    fun entriesAreOrderedByInstantWhateverTheZoneOrTheOrderTheyWereAdded() = runTest {
        val id = vehicleAtNoon()
        // 15:00 Helsinki is 12:00 UTC (noon); 08:30 New York is 12:30 UTC. Added in the opposite order.
        val newYorkEntry = addEntry(id, ZonedMoment.of(noon + 30.minutes, newYork), Distance(1_000), null)
        val helsinkiEntry = addEntry(id, ZonedMoment.of(noon + 1.minutes, helsinki), Distance(2_000), null)

        val ids = repository.observeLog(id).first().map { it.id }

        assertEquals(listOf(newYorkEntry, helsinkiEntry), ids.take(2))
    }

    @Test
    fun aBackdatedEntryLandsInItsChronologicalPlace() = runTest {
        val id = vehicleAtNoon()
        val today = addEntry(id, at(5.hours), Distance(1_000), null)
        val yesterday = addEntry(id, at((-24).hours + 5.hours), Distance(2_000), null)

        val ids = repository.observeLog(id).first().map { it.id }

        // Newest first: today's entry, the initial event (noon), then yesterday's entry that was added last.
        assertEquals(today, ids[0])
        assertEquals(yesterday, ids.last())
    }

    @Test
    fun entriesAtTheSameInstantComeLastAddedFirst() = runTest {
        val id = vehicleAtNoon()
        val first = addEntry(id, at(1.hours), Distance(1_000), null)
        val second = addEntry(id, at(1.hours), Distance(2_000), null)

        assertEquals(listOf(second, first), repository.observeRecentEvents(id, 2).first().map { it.id })
    }

    @Test
    fun entriesAtTheSameInstantInDifferentZonesComeLastAddedFirst() = runTest {
        val id = vehicleAtNoon()
        // The same instant written in three zones: 15:00 in Helsinki, 08:00 in New York and 12:00 UTC.
        val first = addEntry(id, at(0.hours, helsinki), Distance(1_000), null)
        val second = addEntry(id, at(0.hours, newYork), Distance(2_000), null)
        val third = addEntry(id, at(0.hours, TimeZone.UTC), Distance(3_000), null)

        val newestFirst = repository.observeLog(id).first().map { it.id }
        // The initial event (the vehicle's first row) is last; the entries follow their order of adding, not their zones.
        assertEquals(listOf(third, second, first), newestFirst.take(3))
        assertEquals(listOf(third, second, first), repository.observeRecentEvents(id, 3).first().map { it.id })
        // Each keeps the zone it was entered in.
        assertEquals(
            listOf("UTC", "America/New_York", "Europe/Helsinki"),
            repository.observeLog(id).first().take(3).map { it.occurredAt.zone?.id },
        )
    }

    @Test
    fun theOrderOfTheSameInstantFollowsRowOrderNotTheCreatedTime() = runTest {
        val id = vehicleAtNoon()
        clock.current = noon + 10.hours
        val first = addEntry(id, at(1.hours, helsinki), Distance(1_000), null)
        // The clock steps back (a corrected device clock, say): the entry added later has the earlier created time.
        clock.current = noon + 1.hours
        val second = addEntry(id, at(1.hours, newYork), Distance(2_000), null)

        assertEquals(listOf(second, first), repository.observeRecentEvents(id, 2).first().map { it.id })
    }

    @Test
    fun anEntryAtTheInitialInstantInAnotherZoneCountsWhenAddedAfterIt() = runTest {
        val id = vehicleAtNoon() // the initial event: noon UTC, entered in the device zone
        addEntry(id, at(0.minutes, newYork), Distance(30_000), null)
        addEntry(id, at(0.minutes, helsinki), Distance(20_000), null)

        assertCurrentOdometer(id, 45_250_000)
    }

    @Test
    fun anEntryAtTheSameInstantAddedBeforeAnOdometerSettingEventInAnotherZoneIsReplacedByIt() = runTest {
        val id = vehicleAtNoon()
        addEntry(id, at(1.hours, newYork), Distance(30_000), null)
        // A second baseline for the same instant, entered in Helsinki, added after the entry.
        database.vehicleEventQueries.insertEvent(
            "second-baseline", id, "INITIAL_ODOMETER", (noon + 1.hours).toEpochMilliseconds(), 50_000_000,
            (noon + 1.hours).toEpochMilliseconds(), "Europe/Helsinki", 3 * 3600L, null,
        )
        assertCurrentOdometer(id, 50_000_000)

        // An entry at that instant in yet another zone, added after the baseline, counts.
        addEntry(id, at(1.hours, TimeZone.UTC), Distance(10_000), null)
        assertCurrentOdometer(id, 50_010_000)
    }

    @Test
    fun theRecentEventsIncludeDistanceEntries() = runTest {
        val id = vehicleAtNoon()
        for (i in 1..6) addEntry(id, at(i.hours), Distance(i * 1_000L), null)

        val recent = repository.observeRecentEvents(id, 5).first()

        assertEquals(5, recent.size)
        assertTrue(recent.all { it is VehicleEvent.DistanceEntry })
        assertEquals(Distance(6_000), (recent.first() as VehicleEvent.DistanceEntry).distance)
    }

    @Test
    fun currentOdometerOfAnInitialEventAlone() = runTest {
        assertCurrentOdometer(vehicleAtNoon(), 45_200_000)
    }

    @Test
    fun currentOdometerAddsEntriesAfterTheInitialEvent() = runTest {
        val id = vehicleAtNoon()
        addEntry(id, at(1.hours), Distance(30_000), null)
        addEntry(id, at(2.hours), Distance(20_000), null)
        addEntry(id, at(3.hours), Distance(500), null)

        assertCurrentOdometer(id, 45_250_500)
    }

    @Test
    fun anEntryBeforeTheInitialEventNeverChangesTheCurrentOdometer() = runTest {
        val id = vehicleAtNoon()
        addEntry(id, at((-7).hours * 24), Distance(30_000), null)
        addEntry(id, at(-1.minutes), Distance(20_000), null)

        assertCurrentOdometer(id, 45_200_000)
        assertEquals(3, repository.observeLog(id).first().size)
    }

    @Test
    fun anEntryAtTheInitialInstantAddedAfterItCounts() = runTest {
        val id = vehicleAtNoon()
        addEntry(id, at(0.minutes), Distance(30_000), null)

        assertCurrentOdometer(id, 45_230_000)
    }

    @Test
    fun aLaterOdometerSettingEventReplacesTheRunningTotalInSqlAndInTheFunction() = runTest {
        val id = vehicleAtNoon()
        addEntry(id, at(1.hours), Distance(30_000), null)
        insertEvent("second-baseline", id, "INITIAL_ODOMETER", (noon + 2.hours).toEpochMilliseconds(), 50_000_000)
        addEntry(id, at(3.hours), Distance(10_000), null)
        addEntry(id, at(90.minutes), Distance(7_000), null) // before the second baseline

        assertCurrentOdometer(id, 50_010_000)
    }

    @Test
    fun theSameDistanceInAnotherZoneChangesNothingAboutTheOdometer() = runTest {
        val id = vehicleAtNoon()
        addEntry(id, at(1.hours, newYork), Distance(30_000), null)
        addEntry(id, at(2.hours, helsinki), Distance(20_000), null)

        assertCurrentOdometer(id, 45_250_000)
    }

    // ---- Odometer anchors

    @Test
    fun anAnchorIsAddedAndReadBackWithItsZone() = runTest {
        val id = vehicleAtNoon()
        val eventId = repository.addOdometerAnchor(id, at((-24).hours, newYork), Distance(44_000_000), tenthsIncluded = false)

        val anchor = repository.observeLog(id).first().last { it.id == eventId } as VehicleEvent.OdometerAnchor
        assertEquals(Distance(44_000_000), anchor.reading)
        assertEquals(noon - 24.hours, anchor.occurredAt.instant)
        assertEquals("America/New_York", anchor.occurredAt.zone?.id)
    }

    @Test
    fun anAnchorWithANoteIsReadBackWithIt() = runTest {
        val id = vehicleAtNoon()

        val eventId = repository.addOdometerAnchor(id, at((-24).hours), Distance(44_000_000), tenthsIncluded = false, note = "reset by mistake")

        val anchor = repository.observeLog(id).first().last { it.id == eventId } as VehicleEvent.OdometerAnchor
        assertEquals("reset by mistake", anchor.note)
    }

    @Test
    fun anAnchorWithoutANoteReadsBackNull() = runTest {
        val id = vehicleAtNoon()

        val eventId = repository.addOdometerAnchor(id, at((-24).hours), Distance(44_000_000), tenthsIncluded = false)

        val anchor = repository.observeLog(id).first().last { it.id == eventId } as VehicleEvent.OdometerAnchor
        assertNull(anchor.note)
    }

    @Test
    fun anAnchorWithPhotosIsReadBackWithThem() = runTest {
        val id = vehicleAtNoon()
        val photo = PendingPicture(pictureStore.addPending())

        val eventId = repository.addOdometerAnchor(id, at((-24).hours), Distance(44_000_000), tenthsIncluded = false, photos = listOf(photo))

        val anchor = repository.observeLog(id).first().last { it.id == eventId } as VehicleEvent.OdometerAnchor
        assertEquals(1, anchor.photoIds.size)
    }

    @Test
    fun anAnchorWithoutPhotosReadsBackEmpty() = runTest {
        val id = vehicleAtNoon()

        val eventId = repository.addOdometerAnchor(id, at((-24).hours), Distance(44_000_000), tenthsIncluded = false)

        val anchor = repository.observeLog(id).first().last { it.id == eventId } as VehicleEvent.OdometerAnchor
        assertTrue(anchor.photoIds.isEmpty())
    }

    @Test
    fun theInitialOdometerEventNeverHasPhotos() = runTest {
        val id = vehicleAtNoon()

        val event = repository.observeLog(id).first().single()

        assertTrue(event.photoIds.isEmpty())
    }

    // ---- Adding and removing a photo on an already-saved event (add-event-pictures, the details screen's "Edit" action)

    @Test
    fun addEventPhotoAppendsToAnAlreadySavedEvent() = runTest {
        val id = vehicleAtNoon()
        val eventId = addEntry(id, at(1.hours), Distance(30_000), null)
        val first = pictureStore.addPending()

        repository.addEventPhoto(id, eventId, PendingPicture(first))

        val entry = repository.observeLog(id).first().first { it.id == eventId } as VehicleEvent.DistanceEntry
        assertEquals(1, entry.photoIds.size)
    }

    @Test
    fun addEventPhotoAppendsAfterPhotosAlreadyThere() = runTest {
        val id = vehicleAtNoon()
        val eventId = repository.addDistanceEntry(
            id, at(1.hours), Distance(30_000), null, tenthsIncluded = false, photos = listOf(PendingPicture(pictureStore.addPending())),
        )

        repository.addEventPhoto(id, eventId, PendingPicture(pictureStore.addPending()))

        val entry = repository.observeLog(id).first().first { it.id == eventId } as VehicleEvent.DistanceEntry
        assertEquals(2, entry.photoIds.size)
    }

    @Test
    fun removeEventPhotoDropsItAndDeletesItsFiles() = runTest {
        val id = vehicleAtNoon()
        val first = PendingPicture(pictureStore.addPending())
        val second = PendingPicture(pictureStore.addPending())
        val eventId = repository.addDistanceEntry(id, at(1.hours), Distance(30_000), null, tenthsIncluded = false, photos = listOf(first, second))
        val entryBefore = repository.observeLog(id).first().first { it.id == eventId } as VehicleEvent.DistanceEntry
        val removedId = entryBefore.photoIds[0]
        val keptId = entryBefore.photoIds[1]

        repository.removeEventPhoto(id, eventId, removedId)

        val entryAfter = repository.observeLog(id).first().first { it.id == eventId } as VehicleEvent.DistanceEntry
        assertEquals(listOf(keptId), entryAfter.photoIds)
        assertTrue(removedId !in pictureStore.pictures)
    }

    @Test
    fun removingAPhotoFromOneEventDoesNotTouchAnothers() = runTest {
        val id = vehicleAtNoon()
        val firstEvent = repository.addDistanceEntry(
            id, at(1.hours), Distance(30_000), null, tenthsIncluded = false, photos = listOf(PendingPicture(pictureStore.addPending())),
        )
        val secondEvent = repository.addDistanceEntry(
            id, at(2.hours), Distance(10_000), null, tenthsIncluded = false, photos = listOf(PendingPicture(pictureStore.addPending())),
        )
        val secondPhotoId = (repository.observeLog(id).first().first { it.id == secondEvent } as VehicleEvent.DistanceEntry).photoIds.single()

        repository.removeEventPhoto(id, firstEvent, secondPhotoId)

        val second = repository.observeLog(id).first().first { it.id == secondEvent } as VehicleEvent.DistanceEntry
        assertEquals(listOf(secondPhotoId), second.photoIds)
    }

    @Test
    fun anAnchorBeforeTheInitialEventDoesNotChangeTheCurrentOdometer() = runTest {
        val id = vehicleAtNoon()
        repository.addOdometerAnchor(id, at((-24).hours), Distance(44_000_000), tenthsIncluded = false)

        assertCurrentOdometer(id, 45_200_000)
    }

    @Test
    fun anEntryBetweenAnAnchorAndTheInitialEventDoesNotChangeTheCurrentOdometer() = runTest {
        val id = vehicleAtNoon()
        repository.addOdometerAnchor(id, at((-48).hours), Distance(44_000_000), tenthsIncluded = false)
        addEntry(id, at((-24).hours), Distance(30_000), null)

        assertCurrentOdometer(id, 45_200_000)
    }

    @Test
    fun anAnchorAfterTheInitialEventReplacesTheRunningTotalInSqlAndInTheFunction() = runTest {
        val id = vehicleAtNoon()
        addEntry(id, at(1.hours), Distance(30_000), null)
        repository.addOdometerAnchor(id, at(2.hours), Distance(46_000_000), tenthsIncluded = false)
        addEntry(id, at(3.hours), Distance(10_000), null)

        assertCurrentOdometer(id, 46_010_000)
    }

    @Test
    fun anAnchorAtTheSameInstantAsAnEntryCountsInTheOrderAddedWhateverTheZones() = runTest {
        val id = vehicleAtNoon()
        addEntry(id, at(1.hours, helsinki), Distance(30_000), null)
        repository.addOdometerAnchor(id, at(1.hours, newYork), Distance(50_000_000), tenthsIncluded = false)
        addEntry(id, at(1.hours, TimeZone.UTC), Distance(10_000), null)

        assertCurrentOdometer(id, 50_010_000)
        val newestFirst = repository.observeLog(id).first()
        assertTrue(newestFirst[0] is VehicleEvent.DistanceEntry)
        assertTrue(newestFirst[1] is VehicleEvent.OdometerAnchor)
        assertTrue(newestFirst[2] is VehicleEvent.DistanceEntry)
    }

    @Test
    fun savingAnAnchorRemembersTheTenthsChoiceInTheSameTransaction() = runTest {
        val id = vehicleAtNoon()
        repository.addOdometerAnchor(id, at((-24).hours), Distance(44_000_000), tenthsIncluded = true)

        assertEquals(true, rememberedTenths(id))
    }

    @Test
    fun anAnchorIsOnlyEverAdded() = runTest {
        val id = vehicleAtNoon()
        val before = repository.observeLog(id).first()
        repository.addOdometerAnchor(id, at((-24).hours), Distance(44_000_000), tenthsIncluded = false)

        val after = repository.observeLog(id).first()
        assertEquals(before.size + 1, after.size)
        assertTrue(after.containsAll(before))
    }

    // ---- The tenths choice remembered per vehicle

    private suspend fun rememberedTenths(id: String) = repository.observeVehicle(id).first()?.vehicle?.logDistanceTenths

    @Test
    fun noTenthsChoiceIsRememberedBeforeAnEntryIsSaved() = runTest {
        val id = vehicleAtNoon()
        assertNull(rememberedTenths(id))
        assertNull(repository.observeVehicles().first().single().logDistanceTenths)
    }

    @Test
    fun savingAnEntryRemembersTheTenthsChoiceUsed() = runTest {
        val id = vehicleAtNoon()

        addEntry(id, at(1.hours), Distance(1_000), null, tenthsIncluded = true)
        assertEquals(true, rememberedTenths(id))
        assertEquals(true, repository.observeVehicles().first().single().logDistanceTenths)

        addEntry(id, at(2.hours), Distance(1_000), null, tenthsIncluded = false)
        assertEquals(false, rememberedTenths(id))
    }

    @Test
    fun theChoiceOfOneVehicleDoesNotAffectAnother() = runTest {
        val first = vehicleAtNoon()
        val second = repository.addVehicle("Van", null, VehicleType.CAR, VehicleColors.default, OdometerUnit.MILES, Distance.ZERO)

        addEntry(first, at(1.hours), Distance(1_000), null, tenthsIncluded = true)

        assertEquals(true, rememberedTenths(first))
        assertNull(rememberedTenths(second))
    }

    @Test
    fun aFailedEntryInsertLeavesTheRememberedChoiceUnchanged() = runTest {
        val id = vehicleAtNoon()
        addEntry(id, at(1.hours), Distance(1_000), null, tenthsIncluded = false)
        // Make the next insert fail: its event id is already taken.
        insertEvent("id-4", "other-owner", "INITIAL_ODOMETER", 1, 0)
        database.vehicleQueries.insertVehicle("other-owner", "Other", null, "KILOMETERS", 1, 1, null, "CAR", "203A43")

        assertFails { addEntry(id, at(2.hours), Distance(2_000), null, tenthsIncluded = true) }

        assertEquals(false, rememberedTenths(id))
        assertEquals(2, repository.observeLog(id).first().size)
    }

    @Test
    fun rememberingTheChoiceDoesNotTouchTheVehiclesUpdatedTimeOrLog() = runTest {
        val id = vehicleAtNoon()
        val logBefore = repository.observeLog(id).first()
        val updatedBefore = database.vehicleQueries.selectVehicles().executeAsList().single()

        addEntry(id, at(1.hours), Distance(1_000), null, tenthsIncluded = true)

        assertEquals(updatedBefore.name, repository.observeVehicles().first().single().name)
        assertEquals(logBefore.first(), repository.observeLog(id).first().last())
    }

    // ---- The vehicle last logged for, stored apart from the events (see add-direct-logging's design)

    @Test
    fun nothingIsRememberedOnANewDatabase() = runTest {
        assertNull(repository.observeLastLoggedVehicleId().first())
    }

    @Test
    fun savingADistanceEntryRemembersItsVehicle() = runTest {
        val id = vehicleAtNoon()

        addEntry(id, at(1.hours), Distance(1_000), null)

        assertEquals(id, repository.observeLastLoggedVehicleId().first())
    }

    @Test
    fun savingAnOdometerAnchorRemembersItsVehicleToo() = runTest {
        val id = vehicleAtNoon()

        repository.addOdometerAnchor(id, at((-24).hours), Distance(44_000_000), tenthsIncluded = false)

        assertEquals(id, repository.observeLastLoggedVehicleId().first())
    }

    @Test
    fun aBackdatedEntryStillMakesItsVehicleTheRememberedOne() = runTest {
        // Not derived from the events' dates: an entry dated a month ago is still the one just saved.
        val first = vehicleAtNoon()
        val second = repository.addVehicle("Van", null, VehicleType.CAR, VehicleColors.default, OdometerUnit.MILES, Distance.ZERO)
        addEntry(first, at(1.hours), Distance(1_000), null)

        addEntry(second, at((-24 * 30).hours), Distance(1_000), null)

        assertEquals(second, repository.observeLastLoggedVehicleId().first())
    }

    @Test
    fun theLastVehicleSavedForWins() = runTest {
        val first = vehicleAtNoon()
        val second = repository.addVehicle("Van", null, VehicleType.CAR, VehicleColors.default, OdometerUnit.MILES, Distance.ZERO)

        addEntry(first, at(1.hours), Distance(1_000), null)
        addEntry(second, at(2.hours), Distance(1_000), null)
        addEntry(first, at(3.hours), Distance(1_000), null)

        assertEquals(first, repository.observeLastLoggedVehicleId().first())
    }

    @Test
    fun aFailedEntryInsertLeavesTheRememberedVehicleUnchanged() = runTest {
        val id = vehicleAtNoon()
        addEntry(id, at(1.hours), Distance(1_000), null)
        val other = repository.addVehicle("Van", null, VehicleType.CAR, VehicleColors.default, OdometerUnit.MILES, Distance.ZERO)
        // Make the next insert fail: its event id is already taken (id-1 the vehicle, id-2 its initial event, id-3 the entry, id-4 and id-5 the Van and its initial event; id-6 is next).
        insertEvent("id-6", "other-owner-2", "INITIAL_ODOMETER", 1, 0)
        database.vehicleQueries.insertVehicle("other-owner-2", "Other", null, "KILOMETERS", 1, 1, null, "CAR", "203A43")

        assertFails { addEntry(other, at(2.hours), Distance(2_000), null) }

        assertEquals(id, repository.observeLastLoggedVehicleId().first())
    }

    @Test
    fun theMemoryEmitsWhenItChanges() = runTest {
        val id = vehicleAtNoon()
        val seen = mutableListOf<String?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeLastLoggedVehicleId().collect { seen += it }
        }

        addEntry(id, at(1.hours), Distance(1_000), null)

        assertEquals(listOf(null, id), seen)
    }

    // ---- Pictures

    private fun updatedAtOf(vehicleId: String): Long =
        driver.executeQuery(
            identifier = null,
            sql = "SELECT updated_at FROM vehicle WHERE id = '$vehicleId'",
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0)!!)
            },
            parameters = 0,
        ).value

    private suspend fun pictureIdOf(vehicleId: String): String? = repository.observeVehicle(vehicleId).first()!!.vehicle.pictureId

    private suspend fun addCarWithPicture(): Pair<String, String> {
        val pending = pictureStore.addPending()
        val id = repository.addVehicle("Family car", "ABC-123", VehicleType.CAR, VehicleColors.default, OdometerUnit.KILOMETERS, Distance(45_200_000), PendingPicture(pending))
        return id to pictureIdOf(id)!!
    }

    @Test
    fun aVehicleAddedWithAPictureRefersToItsPromotedFiles() = runTest {
        val small = FakePictureStore.image(1, 1)
        val large = FakePictureStore.image(2, 2)
        val pending = pictureStore.addPending(small, large)

        val id = repository.addVehicle("Family car", null, VehicleType.CAR, VehicleColors.default, OdometerUnit.KILOMETERS, Distance(45_200_000), PendingPicture(pending))

        val pictureId = pictureIdOf(id)!!
        assertEquals(pictureId, repository.observeVehicles().first().single().pictureId)
        assertEquals(setOf(pictureId), pictureStore.everything()) // the pending files moved, nothing else is left
        assertEquals(small, pictureStore.pictures.getValue(pictureId).small)
        assertEquals(large, pictureStore.pictures.getValue(pictureId).large)
    }

    @Test
    fun aVehicleAddedWithoutAPictureHasNoneAndTouchesNoFiles() = runTest {
        val id = addFamilyCar()

        assertNull(pictureIdOf(id))
        assertEquals(emptySet(), pictureStore.everything())
    }

    @Test
    fun theVehicleItsEventAndItsPictureAreSavedTogether() = runTest {
        val (id, pictureId) = addCarWithPicture()

        assertEquals(1, repository.observeLog(id).first().size)
        assertEquals(pictureId, pictureIdOf(id))
    }

    @Test
    fun aFailedAddWithAPictureDeletesTheFilesItMovedIntoUse() = runTest {
        // The event insert fails: its id ("id-2") is already taken by another vehicle's event.
        database.vehicleQueries.insertVehicle("other", "Other", null, "KILOMETERS", 1, 1, null, "CAR", "203A43")
        insertEvent("id-2", "other", "INITIAL_ODOMETER", 1, 0)
        val pending = pictureStore.addPending()

        assertFails { repository.addVehicle("Family car", null, VehicleType.CAR, VehicleColors.default, OdometerUnit.KILOMETERS, Distance(1), PendingPicture(pending)) }

        assertEquals(listOf("other"), repository.observeVehicles().first().map { it.id })
        assertEquals(emptySet(), pictureStore.everything())
    }

    @Test
    fun aPendingPictureThatIsGoneFailsTheAddAndSavesNothing() = runTest {
        assertFails { repository.addVehicle("Family car", null, VehicleType.CAR, VehicleColors.default, OdometerUnit.KILOMETERS, Distance(1), PendingPicture("gone")) }

        assertEquals(emptyList(), repository.observeVehicles().first())
    }

    @Test
    fun aFailingDiskFailsTheAddAndSavesNothing() = runTest {
        val pending = pictureStore.addPending()
        pictureStore.promoteFailure = IllegalStateException("disk full")

        assertFails { repository.addVehicle("Family car", null, VehicleType.CAR, VehicleColors.default, OdometerUnit.KILOMETERS, Distance(1), PendingPicture(pending)) }

        assertEquals(emptyList(), repository.observeVehicles().first())
    }

    @Test
    fun replacingThePictureUsesANewIdAndDeletesTheOldFiles() = runTest {
        val (id, oldPictureId) = addCarWithPicture()
        val replacement = pictureStore.addPending(FakePictureStore.image(7), FakePictureStore.image(8))

        repository.updateVehicle(id, "Family car", "ABC-123", VehicleType.CAR, VehicleColors.default, PictureChange.Replace(PendingPicture(replacement)))

        val newPictureId = pictureIdOf(id)!!
        assertNotEquals(oldPictureId, newPictureId)
        assertEquals(setOf(newPictureId), pictureStore.everything())
        assertContentEquals(byteArrayOf(7), pictureStore.pictures.getValue(newPictureId).small.bytes)
    }

    @Test
    fun removingThePictureClearsTheIdAndDeletesTheFiles() = runTest {
        val (id, _) = addCarWithPicture()

        repository.updateVehicle(id, "Family car", "ABC-123", VehicleType.CAR, VehicleColors.default, PictureChange.Remove)

        assertNull(pictureIdOf(id))
        assertEquals(emptySet(), pictureStore.everything())
    }

    @Test
    fun removingAPictureThatIsNotThereIsHarmless() = runTest {
        val id = addFamilyCar()

        repository.updateVehicle(id, "Family car", "ABC-123", VehicleType.CAR, VehicleColors.default, PictureChange.Remove)

        assertNull(pictureIdOf(id))
    }

    @Test
    fun keepingThePictureLeavesTheIdAndTheFiles() = runTest {
        val (id, pictureId) = addCarWithPicture()

        repository.updateVehicle(id, "Estate car", null, VehicleType.CAR, VehicleColors.default, PictureChange.Keep)
        repository.updateVehicle(id, "Estate car 2", null, VehicleType.CAR, VehicleColors.default) // the default is to keep

        assertEquals(pictureId, pictureIdOf(id))
        assertEquals(setOf(pictureId), pictureStore.everything())
        assertEquals("Estate car 2", repository.observeVehicles().first().single().name)
    }

    @Test
    fun aFailedEditKeepsTheOldPictureInUseAndLeavesNoNewFiles() = runTest {
        val (id, oldPictureId) = addCarWithPicture()
        val replacement = pictureStore.addPending()
        driver.execute(null, "CREATE TRIGGER fail_update BEFORE UPDATE ON vehicle BEGIN SELECT RAISE(ABORT, 'boom'); END", 0)

        assertFails { repository.updateVehicle(id, "Changed", null, VehicleType.CAR, VehicleColors.default, PictureChange.Replace(PendingPicture(replacement))) }

        assertEquals(oldPictureId, pictureIdOf(id))
        assertEquals("Family car", repository.observeVehicles().first().single().name)
        assertEquals(setOf(oldPictureId), pictureStore.everything())
    }

    @Test
    fun aFailedEditThatWouldRemoveThePictureKeepsIt() = runTest {
        val (id, oldPictureId) = addCarWithPicture()
        driver.execute(null, "CREATE TRIGGER fail_update BEFORE UPDATE ON vehicle BEGIN SELECT RAISE(ABORT, 'boom'); END", 0)

        assertFails { repository.updateVehicle(id, "Changed", null, VehicleType.CAR, VehicleColors.default, PictureChange.Remove) }

        assertEquals(oldPictureId, pictureIdOf(id))
        assertEquals(setOf(oldPictureId), pictureStore.everything())
    }

    @Test
    fun aPictureEditMovesTheUpdatedTimeButAddsNoEventAndChangesNoOtherField() = runTest {
        val (id, _) = addCarWithPicture()
        val before = repository.observeLog(id).first()
        val details = repository.observeVehicle(id).first()!!
        clock.current += 1.hours

        repository.updateVehicle(id, "Family car", "ABC-123", VehicleType.CAR, VehicleColors.default, PictureChange.Remove)

        assertEquals(before, repository.observeLog(id).first())
        val after = repository.observeVehicle(id).first()!!
        assertEquals(details.currentOdometer, after.currentOdometer)
        assertEquals(details.vehicle.odometerUnit, after.vehicle.odometerUnit)
        assertEquals(clock.current.toEpochMilliseconds(), updatedAtOf(id))
    }

    // ---- The vehicle's type

    private suspend fun typeOf(vehicleId: String): VehicleType = repository.observeVehicle(vehicleId).first()!!.vehicle.type

    private fun storedTypeOf(vehicleId: String): String =
        driver.executeQuery(
            identifier = null,
            sql = "SELECT vehicle_type FROM vehicle WHERE id = '$vehicleId'",
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getString(0)!!)
            },
            parameters = 0,
        ).value

    @Test
    fun aVehicleAddedWithEachTypeReadsItBackAndStoresItsCode() = runTest {
        for (type in VehicleType.entries) {
            val id = repository.addVehicle(type.name, null, type, VehicleColors.default, OdometerUnit.KILOMETERS, Distance(1))

            assertEquals(type, typeOf(id), type.name)
            assertEquals(type, repository.observeVehicles().first().single { it.id == id }.type, type.name)
            assertEquals(type.code, storedTypeOf(id), type.name)
        }
    }

    @Test
    fun anEditChangesTheType() = runTest {
        val id = addFamilyCar()
        assertEquals(VehicleType.CAR, typeOf(id))

        repository.updateVehicle(id, "Family car", "ABC-123", VehicleType.VAN, VehicleColors.default)

        assertEquals(VehicleType.VAN, typeOf(id))
        assertEquals("VAN", storedTypeOf(id))
    }

    @Test
    fun anEditThatKeepsTheTypeLeavesIt() = runTest {
        val id = repository.addVehicle("Bike", null, VehicleType.MOTORCYCLE, VehicleColors.default, OdometerUnit.KILOMETERS, Distance(1))

        repository.updateVehicle(id, "Big bike", null, VehicleType.MOTORCYCLE, VehicleColors.default)

        assertEquals(VehicleType.MOTORCYCLE, typeOf(id))
        assertEquals("Big bike", repository.observeVehicles().first().single().name)
    }

    @Test
    fun aStoredCodeThisAppDoesNotKnowReadsAsOther() = runTest {
        val id = addFamilyCar()
        database.vehicleQueries.updateVehicleType("HOVERCRAFT", 5, id)

        assertEquals(VehicleType.OTHER, typeOf(id))
        assertEquals(VehicleType.OTHER, repository.observeVehicles().first().single().type)
    }

    @Test
    fun theTypeIsNeverMissingWhateverIsStored() = runTest {
        val id = addFamilyCar()
        for (code in listOf("", "car", "PICKUP", " CAR")) {
            database.vehicleQueries.updateVehicleType(code, 5, id)
            assertEquals(VehicleType.OTHER, typeOf(id), "code '$code'")
        }
    }

    @Test
    fun aTypeEditDoesNotChangeTheLogTheOdometerTheUnitOrThePicture() = runTest {
        val (id, pictureId) = addCarWithPicture()
        addEntry(id, at(1.hours), Distance(30_000), null)
        val logBefore = repository.observeLog(id).first()
        val before = repository.observeVehicle(id).first()!!

        repository.updateVehicle(id, "Family car", "ABC-123", VehicleType.TRUCK, VehicleColors.default)

        assertEquals(logBefore, repository.observeLog(id).first())
        val after = repository.observeVehicle(id).first()!!
        assertEquals(before.currentOdometer, after.currentOdometer)
        assertEquals(before.vehicle.odometerUnit, after.vehicle.odometerUnit)
        assertEquals(pictureId, after.vehicle.pictureId)
        assertEquals(VehicleType.TRUCK, after.vehicle.type)
    }

    @Test
    fun aFailedEditLeavesTheTypeAsItWas() = runTest {
        val id = addFamilyCar()
        driver.execute(null, "CREATE TRIGGER fail_update BEFORE UPDATE ON vehicle BEGIN SELECT RAISE(ABORT, 'boom'); END", 0)

        assertFails { repository.updateVehicle(id, "Changed", null, VehicleType.BUS, VehicleColors.default) }

        assertEquals(VehicleType.CAR, typeOf(id))
        assertEquals("Family car", repository.observeVehicles().first().single().name)
    }

    @Test
    fun aFailedAddSavesNoVehicleAtAll() = runTest {
        // The event insert fails: its id ("id-2") is already taken by another vehicle's event.
        database.vehicleQueries.insertVehicle("other", "Other", null, "KILOMETERS", 1, 1, null, "CAR", "203A43")
        insertEvent("id-2", "other", "INITIAL_ODOMETER", 1, 0)

        assertFails { repository.addVehicle("Van", null, VehicleType.VAN, VehicleColors.default, OdometerUnit.KILOMETERS, Distance(1)) }

        assertEquals(listOf("other"), repository.observeVehicles().first().map { it.id })
    }

    // ---- The vehicle's color

    private suspend fun colorOf(vehicleId: String): Rgb = repository.observeVehicle(vehicleId).first()!!.vehicle.color

    private fun storedColorOf(vehicleId: String): String =
        driver.executeQuery(
            identifier = null,
            sql = "SELECT vehicle_color FROM vehicle WHERE id = '$vehicleId'",
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getString(0)!!)
            },
            parameters = 0,
        ).value

    @Test
    fun aVehicleAddedWithEachPresetReadsItBackAndStoresItsCode() = runTest {
        for (preset in VehicleColors.presets) {
            val id = repository.addVehicle(preset.name, null, VehicleType.CAR, preset.color, OdometerUnit.KILOMETERS, Distance(1))

            assertEquals(preset.color, colorOf(id), preset.name)
            assertEquals(preset.color, repository.observeVehicles().first().single { it.id == id }.color, preset.name)
            assertEquals(preset.color.hex, storedColorOf(id), preset.name)
        }
    }

    @Test
    fun aColorThatIsNotAPresetIsStoredAsItIs() = runTest {
        val id = repository.addVehicle("From a photo", null, VehicleType.CAR, Rgb(0x123ABC), OdometerUnit.KILOMETERS, Distance(1))

        assertEquals(Rgb(0x123ABC), colorOf(id))
        assertEquals("123ABC", storedColorOf(id))
    }

    @Test
    fun anEditChangesTheColor() = runTest {
        val id = addFamilyCar()
        assertEquals(VehicleColors.default, colorOf(id))

        repository.updateVehicle(id, "Family car", "ABC-123", VehicleType.CAR, Rgb(0xE53935))

        assertEquals(Rgb(0xE53935), colorOf(id))
        assertEquals("E53935", storedColorOf(id))
    }

    @Test
    fun anEditThatKeepsTheColorLeavesIt() = runTest {
        val id = repository.addVehicle("Bike", null, VehicleType.MOTORCYCLE, Rgb(0x1E88E5), OdometerUnit.KILOMETERS, Distance(1))

        repository.updateVehicle(id, "Big bike", null, VehicleType.MOTORCYCLE, Rgb(0x1E88E5))

        assertEquals(Rgb(0x1E88E5), colorOf(id))
        assertEquals("Big bike", repository.observeVehicles().first().single().name)
    }

    @Test
    fun aStoredValueThatIsNotAColorReadsAsTheDefault() = runTest {
        val id = addFamilyCar()
        for (text in listOf("", "red", "#E53935", "E5393", "E539355", "GGGGGG", " E53935")) {
            database.vehicleQueries.updateVehicleColor(text, 5, id)

            assertEquals(VehicleColors.default, colorOf(id), "'$text'")
            assertEquals(VehicleColors.default, repository.observeVehicles().first().single().color, "'$text'")
        }
    }

    @Test
    fun aColorEditDoesNotChangeTheLogTheOdometerTheUnitTheTypeOrThePicture() = runTest {
        val (id, pictureId) = addCarWithPicture()
        addEntry(id, at(1.hours), Distance(30_000), null)
        val logBefore = repository.observeLog(id).first()
        val before = repository.observeVehicle(id).first()!!

        repository.updateVehicle(id, "Family car", "ABC-123", VehicleType.CAR, Rgb(0x8E24AA))

        assertEquals(logBefore, repository.observeLog(id).first())
        val after = repository.observeVehicle(id).first()!!
        assertEquals(before.currentOdometer, after.currentOdometer)
        assertEquals(before.vehicle.odometerUnit, after.vehicle.odometerUnit)
        assertEquals(before.vehicle.type, after.vehicle.type)
        assertEquals(pictureId, after.vehicle.pictureId)
        assertEquals(Rgb(0x8E24AA), after.vehicle.color)
    }

    @Test
    fun aColorEditIsAVehicleEditSoUpdatedAtMoves() = runTest {
        val id = addFamilyCar()
        clock.current = Instant.fromEpochMilliseconds(9_000)

        repository.updateVehicle(id, "Family car", "ABC-123", VehicleType.CAR, Rgb(0x8E24AA))

        assertEquals(9_000L, updatedAtOf(id))
    }

    @Test
    fun aFailedEditLeavesTheColorAsItWas() = runTest {
        val id = addFamilyCar()
        driver.execute(null, "CREATE TRIGGER fail_update BEFORE UPDATE ON vehicle BEGIN SELECT RAISE(ABORT, 'boom'); END", 0)

        assertFails { repository.updateVehicle(id, "Changed", null, VehicleType.CAR, Rgb(0xE53935)) }

        assertEquals(VehicleColors.default, colorOf(id))
        assertEquals("Family car", repository.observeVehicles().first().single().name)
    }

    @Test
    fun aFailedAddSavesNoColorEither() = runTest {
        database.vehicleQueries.insertVehicle("other", "Other", null, "KILOMETERS", 1, 1, null, "CAR", "203A43")
        insertEvent("id-2", "other", "INITIAL_ODOMETER", 1, 0)

        assertFails { repository.addVehicle("Van", null, VehicleType.VAN, Rgb(0xE53935), OdometerUnit.KILOMETERS, Distance(1)) }

        assertEquals(listOf("other"), repository.observeVehicles().first().map { it.id })
    }

    // ---- One event by id, re-observed live (add-event-details-view)

    @Test
    fun observeEventReturnsTheMatchingEvent() = runTest {
        val id = addFamilyCar()
        val entryId = repository.addDistanceEntry(id, ZonedMoment(clock.current), Distance(30_000), null, false, "borrowed to Sam")

        val event = repository.observeEvent(id, entryId).first() as VehicleEvent.DistanceEntry
        assertEquals(entryId, event.id)
        assertEquals(Distance(30_000), event.distance)
        assertEquals("borrowed to Sam", event.note)
    }

    @Test
    fun observeEventReturnsNullForAnUnknownId() = runTest {
        val id = addFamilyCar()

        assertNull(repository.observeEvent(id, "no-such-event").first())
    }

    @Test
    fun observeEventReturnsNullForAnEventOfAnotherVehicle() = runTest {
        val id = addFamilyCar()
        val otherId = repository.addVehicle("Van", null, VehicleType.VAN, VehicleColors.default, OdometerUnit.KILOMETERS, Distance(1))
        val otherEntryId = repository.addDistanceEntry(otherId, ZonedMoment(clock.current), Distance(1_000), null, false)

        assertNull(repository.observeEvent(id, otherEntryId).first())
    }

    @Test
    fun observeEventIsQueryBackedNotASnapshot() = runTest {
        // A fresh read after the matching row starts existing sees it — the flow is backed by the query, not a
        // value captured once and cached. (add-event-editing below now also exercises this with a real update.)
        val id = addFamilyCar()
        assertNull(repository.observeEvent(id, "id-3").first())

        val entryId = repository.addDistanceEntry(id, ZonedMoment(clock.current), Distance(30_000), null, false)

        assertEquals("id-3", entryId)
        assertEquals(entryId, (repository.observeEvent(id, entryId).first() as VehicleEvent.DistanceEntry).id)
    }

    @Test
    fun observeEventSeesANoteUpdateLive() = runTest {
        val id = addFamilyCar()
        val entryId = repository.addDistanceEntry(id, ZonedMoment(clock.current), Distance(30_000), null, false)

        repository.updateEventNote(id, entryId, "borrowed to Sam")

        assertEquals("borrowed to Sam", repository.observeEvent(id, entryId).first()?.note)
    }

    @Test
    fun anAlreadyOpenObserverSeesAnAddedPhotoWithoutResubscribing() = runTest {
        // A one-shot `.first()` on a fresh subscription always re-runs the query, so it would pass even if
        // event_picture changes alone never notified vehicle_event's own listeners (the bug touchEvent fixes,
        // VehicleEvent.sq): this instead keeps one subscription open throughout, the way a details screen already
        // showing the event does, and checks it is pushed the change without ever resubscribing.
        val id = addFamilyCar()
        val entryId = repository.addDistanceEntry(id, ZonedMoment(clock.current), Distance(30_000), null, false)
        val seen = mutableListOf<List<String>>()
        val job = launch { repository.observeEvent(id, entryId).collect { seen += it?.photoIds.orEmpty() } }
        advanceUntilIdle()

        repository.addEventPhoto(id, entryId, PendingPicture(pictureStore.addPending()))
        advanceUntilIdle()

        assertEquals(1, seen.last().size)
        job.cancel()
    }

    @Test
    fun anAlreadyOpenObserverSeesARemovedPhotoWithoutResubscribing() = runTest {
        val id = addFamilyCar()
        val entryId = repository.addDistanceEntry(
            id, ZonedMoment(clock.current), Distance(30_000), null, false, photos = listOf(PendingPicture(pictureStore.addPending())),
        )
        val photoId = repository.observeEvent(id, entryId).first()!!.photoIds.single()
        val seen = mutableListOf<List<String>>()
        val job = launch { repository.observeEvent(id, entryId).collect { seen += it?.photoIds.orEmpty() } }
        advanceUntilIdle()

        repository.removeEventPhoto(id, entryId, photoId)
        advanceUntilIdle()

        assertEquals(emptyList(), seen.last())
        job.cancel()
    }

    // ---- Editing a saved event's note (add-event-editing)

    @Test
    fun updateEventNoteChangesOnlyTheTargetEventsNote() = runTest {
        val id = addFamilyCar()
        val untouchedId = repository.addDistanceEntry(id, ZonedMoment(clock.current), Distance(10_000), null, false, "keep me")
        val targetId = repository.addDistanceEntry(id, ZonedMoment(clock.current), Distance(20_000), null, false, "old note")

        repository.updateEventNote(id, targetId, "new note")

        val log = repository.observeLog(id).first()
        assertEquals("new note", log.single { it.id == targetId }.note)
        assertEquals("keep me", log.single { it.id == untouchedId }.note)
        // Every other field of the target event is unchanged.
        val target = log.single { it.id == targetId } as VehicleEvent.DistanceEntry
        assertEquals(Distance(20_000), target.distance)
    }

    @Test
    fun updateEventNoteCanClearANote() = runTest {
        val id = addFamilyCar()
        val entryId = repository.addDistanceEntry(id, ZonedMoment(clock.current), Distance(30_000), null, false, "borrowed to Sam")

        repository.updateEventNote(id, entryId, null)

        assertNull(repository.observeLog(id).first().single { it.id == entryId }.note)
    }

    @Test
    fun updateEventNoteCanAddANoteWhereThereWasNone() = runTest {
        val id = addFamilyCar()
        val entryId = repository.addDistanceEntry(id, ZonedMoment(clock.current), Distance(30_000), null, false)

        repository.updateEventNote(id, entryId, "added later")

        assertEquals("added later", repository.observeLog(id).first().single { it.id == entryId }.note)
    }

    @Test
    fun updateEventNoteOnOneVehicleDoesNotTouchAnothersEvent() = runTest {
        val id = addFamilyCar()
        val entryId = repository.addDistanceEntry(id, ZonedMoment(clock.current), Distance(30_000), null, false, "mine")
        val otherId = repository.addVehicle("Van", null, VehicleType.VAN, VehicleColors.default, OdometerUnit.KILOMETERS, Distance(1))

        // Wrong vehicle id for this event: the WHERE clause matches vehicle_id AND id, so nothing changes.
        repository.updateEventNote(otherId, entryId, "hijacked")

        assertEquals("mine", repository.observeLog(id).first().single { it.id == entryId }.note)
    }
}
