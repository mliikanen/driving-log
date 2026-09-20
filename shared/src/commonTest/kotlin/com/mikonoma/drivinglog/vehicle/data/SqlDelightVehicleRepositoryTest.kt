package com.mikonoma.drivinglog.vehicle.data

import app.cash.sqldelight.db.SqlDriver
import com.mikonoma.drivinglog.db.DrivingLogDatabase
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class FakeClock(var current: Instant = Instant.fromEpochMilliseconds(1_000)) : Clock {
    override fun now(): Instant = current
}

@OptIn(ExperimentalCoroutinesApi::class)
class SqlDelightVehicleRepositoryTest {

    private val driver: SqlDriver = createTestDriver()
    private val database = DrivingLogDatabase(driver)
    private val clock = FakeClock()
    private var idCounter = 0
    private val repository = SqlDelightVehicleRepository(
        database = database,
        clock = clock,
        newId = { "id-${++idCounter}" },
        dispatcher = UnconfinedTestDispatcher(),
    )

    @AfterTest
    fun close() = driver.close()

    private fun insertEvent(id: String, vehicleId: String, type: String, at: Long, meters: Long?) =
        database.vehicleEventQueries.insertEvent(id, vehicleId, type, at, meters, at)

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
        assertEquals(clock.current, event.occurredAt)
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
        database.vehicleQueries.insertVehicle("other", "Other", null, "KILOMETERS", 1, 1)
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
}
