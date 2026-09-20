package com.mikonoma.drivinglog.vehicle.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.mikonoma.drivinglog.db.DrivingLogDatabase
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.EventZone
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.PendingPicture
import com.mikonoma.drivinglog.vehicle.domain.PictureChange
import com.mikonoma.drivinglog.vehicle.picture.FakeVehiclePictureStore
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import com.mikonoma.drivinglog.vehicle.domain.currentOdometer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotEquals
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
    private val pictureStore = FakeVehiclePictureStore()
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
        database.vehicleEventQueries.insertEvent(id, vehicleId, type, at, meters, at, null, null)

    private suspend fun addFamilyCar(unit: OdometerUnit = OdometerUnit.KILOMETERS, meters: Long = 45_200_000) =
        repository.addVehicle("Family car", "ABC-123", unit, Distance(meters))

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
        repository.addVehicle("Van", null, OdometerUnit.MILES, Distance.ZERO)
        assertNull(repository.observeVehicles().first().single().licensePlate)
    }

    @Test
    fun defaultOdometerOfZeroIsLogged() = runTest {
        val id = repository.addVehicle("Van", null, OdometerUnit.MILES, Distance.ZERO)
        assertEquals(Distance.ZERO, repository.observeVehicle(id).first()?.currentOdometer)
        assertEquals(Distance.ZERO, (repository.observeLog(id).first().single() as VehicleEvent.InitialOdometer).reading)
    }

    @Test
    fun theUnitIsStoredAndReturnedForEveryUnit() = runTest {
        for (unit in OdometerUnit.entries) {
            val id = repository.addVehicle(unit.name, null, unit, Distance.ZERO)
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
        database.vehicleQueries.insertVehicle("other", "Other", null, "KILOMETERS", 1, 1, null)
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

        repository.updateVehicle(id, "Estate car", "XYZ-789")

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
        repository.updateVehicle(id, "Family car", null)
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
        repository.updateVehicle(id, "Estate car", null)
        assertEquals(listOf<String?>("Family car", "Estate car"), names)
    }

    // ---- Distance entries, time zones and the derived odometer

    private val noon = Instant.parse("2026-09-20T12:00:00Z")
    private val helsinki = TimeZone.of("Europe/Helsinki")
    private val newYork = TimeZone.of("America/New_York")

    private fun at(delta: kotlin.time.Duration, zone: TimeZone = TimeZone.UTC) = ZonedMoment.of(noon + delta, zone)

    /** A vehicle whose initial odometer (45 200 km) happens at noon UTC. */
    private suspend fun vehicleAtNoon(): String {
        clock.current = noon
        return repository.addVehicle("Family car", null, OdometerUnit.KILOMETERS, Distance(45_200_000))
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
    fun theSchemaIsVersionFour() {
        assertEquals(4L, DrivingLogDatabase.Schema.version)
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
            (noon + 1.hours).toEpochMilliseconds(), "Europe/Helsinki", 3 * 3600L,
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
        val second = repository.addVehicle("Van", null, OdometerUnit.MILES, Distance.ZERO)

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
        database.vehicleQueries.insertVehicle("other-owner", "Other", null, "KILOMETERS", 1, 1, null)

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
        val id = repository.addVehicle("Family car", "ABC-123", OdometerUnit.KILOMETERS, Distance(45_200_000), PendingPicture(pending))
        return id to pictureIdOf(id)!!
    }

    @Test
    fun aVehicleAddedWithAPictureRefersToItsPromotedFiles() = runTest {
        val small = FakeVehiclePictureStore.image(1, 1)
        val large = FakeVehiclePictureStore.image(2, 2)
        val pending = pictureStore.addPending(small, large)

        val id = repository.addVehicle("Family car", null, OdometerUnit.KILOMETERS, Distance(45_200_000), PendingPicture(pending))

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
        database.vehicleQueries.insertVehicle("other", "Other", null, "KILOMETERS", 1, 1, null)
        insertEvent("id-2", "other", "INITIAL_ODOMETER", 1, 0)
        val pending = pictureStore.addPending()

        assertFails { repository.addVehicle("Family car", null, OdometerUnit.KILOMETERS, Distance(1), PendingPicture(pending)) }

        assertEquals(listOf("other"), repository.observeVehicles().first().map { it.id })
        assertEquals(emptySet(), pictureStore.everything())
    }

    @Test
    fun aPendingPictureThatIsGoneFailsTheAddAndSavesNothing() = runTest {
        assertFails { repository.addVehicle("Family car", null, OdometerUnit.KILOMETERS, Distance(1), PendingPicture("gone")) }

        assertEquals(emptyList(), repository.observeVehicles().first())
    }

    @Test
    fun aFailingDiskFailsTheAddAndSavesNothing() = runTest {
        val pending = pictureStore.addPending()
        pictureStore.promoteFailure = IllegalStateException("disk full")

        assertFails { repository.addVehicle("Family car", null, OdometerUnit.KILOMETERS, Distance(1), PendingPicture(pending)) }

        assertEquals(emptyList(), repository.observeVehicles().first())
    }

    @Test
    fun replacingThePictureUsesANewIdAndDeletesTheOldFiles() = runTest {
        val (id, oldPictureId) = addCarWithPicture()
        val replacement = pictureStore.addPending(FakeVehiclePictureStore.image(7), FakeVehiclePictureStore.image(8))

        repository.updateVehicle(id, "Family car", "ABC-123", PictureChange.Replace(PendingPicture(replacement)))

        val newPictureId = pictureIdOf(id)!!
        assertNotEquals(oldPictureId, newPictureId)
        assertEquals(setOf(newPictureId), pictureStore.everything())
        assertContentEquals(byteArrayOf(7), pictureStore.pictures.getValue(newPictureId).small.bytes)
    }

    @Test
    fun removingThePictureClearsTheIdAndDeletesTheFiles() = runTest {
        val (id, _) = addCarWithPicture()

        repository.updateVehicle(id, "Family car", "ABC-123", PictureChange.Remove)

        assertNull(pictureIdOf(id))
        assertEquals(emptySet(), pictureStore.everything())
    }

    @Test
    fun removingAPictureThatIsNotThereIsHarmless() = runTest {
        val id = addFamilyCar()

        repository.updateVehicle(id, "Family car", "ABC-123", PictureChange.Remove)

        assertNull(pictureIdOf(id))
    }

    @Test
    fun keepingThePictureLeavesTheIdAndTheFiles() = runTest {
        val (id, pictureId) = addCarWithPicture()

        repository.updateVehicle(id, "Estate car", null, PictureChange.Keep)
        repository.updateVehicle(id, "Estate car 2", null) // the default is to keep

        assertEquals(pictureId, pictureIdOf(id))
        assertEquals(setOf(pictureId), pictureStore.everything())
        assertEquals("Estate car 2", repository.observeVehicles().first().single().name)
    }

    @Test
    fun aFailedEditKeepsTheOldPictureInUseAndLeavesNoNewFiles() = runTest {
        val (id, oldPictureId) = addCarWithPicture()
        val replacement = pictureStore.addPending()
        driver.execute(null, "CREATE TRIGGER fail_update BEFORE UPDATE ON vehicle BEGIN SELECT RAISE(ABORT, 'boom'); END", 0)

        assertFails { repository.updateVehicle(id, "Changed", null, PictureChange.Replace(PendingPicture(replacement))) }

        assertEquals(oldPictureId, pictureIdOf(id))
        assertEquals("Family car", repository.observeVehicles().first().single().name)
        assertEquals(setOf(oldPictureId), pictureStore.everything())
    }

    @Test
    fun aFailedEditThatWouldRemoveThePictureKeepsIt() = runTest {
        val (id, oldPictureId) = addCarWithPicture()
        driver.execute(null, "CREATE TRIGGER fail_update BEFORE UPDATE ON vehicle BEGIN SELECT RAISE(ABORT, 'boom'); END", 0)

        assertFails { repository.updateVehicle(id, "Changed", null, PictureChange.Remove) }

        assertEquals(oldPictureId, pictureIdOf(id))
        assertEquals(setOf(oldPictureId), pictureStore.everything())
    }

    @Test
    fun aPictureEditMovesTheUpdatedTimeButAddsNoEventAndChangesNoOtherField() = runTest {
        val (id, _) = addCarWithPicture()
        val before = repository.observeLog(id).first()
        val details = repository.observeVehicle(id).first()!!
        clock.current += 1.hours

        repository.updateVehicle(id, "Family car", "ABC-123", PictureChange.Remove)

        assertEquals(before, repository.observeLog(id).first())
        val after = repository.observeVehicle(id).first()!!
        assertEquals(details.currentOdometer, after.currentOdometer)
        assertEquals(details.vehicle.odometerUnit, after.vehicle.odometerUnit)
        assertEquals(clock.current.toEpochMilliseconds(), updatedAtOf(id))
    }
}
