package com.mikonoma.drivinglog.vehicle.details

import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.initialEvent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.fuusio.kide.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleDetailsProcessorTest {

    private val repository = FakeVehicleRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun processor(id: String = "v1") = VehicleDetailsProcessor(id, repository)

    @Test
    fun showsNamePlateAndUnit() {
        repository.seedVehicle("v1", "Family car", "ABC-123", OdometerUnit.MILES_TENTHS)

        val state = processor().state

        assertEquals(false, state.isLoading)
        assertEquals("Family car", state.name)
        assertEquals("ABC-123", state.licensePlate)
        assertEquals(OdometerUnit.MILES_TENTHS, state.unit)
    }

    @Test
    fun aVehicleWithoutAPlateHasNone() {
        repository.seedVehicle("v1", "Van", plate = null)
        assertNull(processor().state.licensePlate)
    }

    @Test
    fun oneEventShowsTheInitialOdometer() {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents("v1", listOf(initialEvent("e1", 100, 45_200_000)))

        val state = processor().state

        assertEquals(1, state.recentEvents.size)
        assertEquals(Distance(45_200_000), state.currentOdometer)
    }

    @Test
    fun sevenEventsShowTheFiveNewest() {
        repository.seedVehicle("v1", "Family car")
        val newestFirst = (7 downTo 1).map { initialEvent("e$it", it * 100L, it * 1_000L) }
        repository.seedEvents("v1", newestFirst)

        val state = processor().state

        assertEquals(VehicleDetailsProcessor.RECENT_EVENT_LIMIT, 5)
        assertEquals(listOf("e7", "e6", "e5", "e4", "e3"), state.recentEvents.map { it.id })
        assertTrue("e2" !in state.recentEvents.map { it.id } && "e1" !in state.recentEvents.map { it.id })
    }

    @Test
    fun theCurrentOdometerComesFromTheNewestEvent() {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents("v1", listOf(initialEvent("newer", 200, 45_900_000), initialEvent("initial", 100, 45_200_000)))

        assertEquals(Distance(45_900_000), processor().state.currentOdometer)
    }

    @Test
    fun eachUnitIsReportedForFormatting() {
        for (unit in OdometerUnit.entries) {
            val id = unit.name
            repository.seedVehicle(id, id, unit = unit)
            assertEquals(unit, processor(id).state.unit)
        }
    }

    @Test
    fun theDetailsFollowEdits() = runTest {
        repository.seedVehicle("v1", "Family car")
        val processor = processor()

        repository.updateVehicle("v1", "Estate car", "XYZ-789")

        assertEquals("Estate car", processor.state.name)
        assertEquals("XYZ-789", processor.state.licensePlate)
    }

    @Test
    fun aMissingVehicleIsReported() {
        val state = processor("missing").state

        assertEquals(false, state.isLoading)
        assertTrue(state.notFound)
    }

    @Test
    fun editNavigatesToTheEditScreen() = runTest {
        repository.seedVehicle("v1", "Family car")
        processor().test {
            dispatch(VehicleDetailsIntent.EditClicked)
            expectSideEffect(VehicleDetailsEffect.ShowEdit("v1"))
        }
    }

    @Test
    fun viewLogNavigatesToTheFullLog() = runTest {
        repository.seedVehicle("v1", "Family car")
        processor().test {
            dispatch(VehicleDetailsIntent.ViewLogClicked)
            expectSideEffect(VehicleDetailsEffect.ShowLog("v1"))
        }
    }
}
