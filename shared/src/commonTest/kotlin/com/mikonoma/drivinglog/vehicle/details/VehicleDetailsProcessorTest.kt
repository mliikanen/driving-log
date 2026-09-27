package com.mikonoma.drivinglog.vehicle.details

import com.mikonoma.drivinglog.vehicle.domain.VehicleColors
import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleType
import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import com.mikonoma.drivinglog.vehicle.picture.FakePictureStore
import com.mikonoma.drivinglog.vehicle.picture.PictureSize
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.distanceEvent
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
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
    private val pictures = FakePictureStore()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun processor(id: String = "v1") = VehicleDetailsProcessor(id, repository, pictures)

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

        repository.updateVehicle("v1", "Estate car", "XYZ-789", VehicleType.CAR, VehicleColors.default)

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

    @Test
    fun logEventNavigatesToTheLogEventForm() = runTest {
        repository.seedVehicle("v1", "Family car")
        processor().test {
            dispatch(VehicleDetailsIntent.LogEventClicked)
            expectSideEffect(VehicleDetailsEffect.ShowLogEvent("v1"))
        }
    }

    @Test
    fun tappingAnEventNavigatesToItsDetails() = runTest {
        repository.seedVehicle("v1", "Family car")
        processor().test {
            dispatch(VehicleDetailsIntent.EventClicked("e2"))
            expectSideEffect(VehicleDetailsEffect.ShowEventDetails("v1", "e2"))
        }
    }

    @Test
    fun theCurrentOdometerIncludesDistanceEntries() {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents(
            "v1",
            listOf(distanceEvent("d2", 300, 20_000), distanceEvent("d1", 200, 30_000), initialEvent("i", 100, 45_200_000)),
        )

        assertEquals(Distance(45_250_000), processor().state.currentOdometer)
    }

    @Test
    fun theRecentEventsMixDistanceEntriesAndTheInitialEventNewestFirst() {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents(
            "v1",
            listOf(distanceEvent("d2", 300, 20_000, loggedOdometer = 45_250_000), distanceEvent("d1", 200, 30_000), initialEvent("i", 100, 45_200_000)),
        )

        val recent = processor().state.recentEvents

        assertEquals(listOf("d2", "d1", "i"), recent.map { it.id })
        assertTrue(recent[0] is VehicleEvent.DistanceEntry)
        assertEquals(Distance(45_250_000), (recent[0] as VehicleEvent.DistanceEntry).loggedOdometer)
    }

    @Test
    fun aBackdatedEntryIsShownInItsChronologicalPlaceWithinTheFiveNewest() {
        repository.seedVehicle("v1", "Family car")
        // The repository returns the log newest first by time: the backdated entry "old" (time 150) was added last.
        val newestFirst = listOf(
            distanceEvent("e5", 600, 1_000), distanceEvent("e4", 500, 1_000), distanceEvent("e3", 400, 1_000),
            distanceEvent("e2", 300, 1_000), distanceEvent("e1", 200, 1_000), distanceEvent("old", 150, 1_000),
            initialEvent("i", 100, 45_200_000),
        )
        repository.seedEvents("v1", newestFirst)

        assertEquals(listOf("e5", "e4", "e3", "e2", "e1"), processor().state.recentEvents.map { it.id })
    }

    // ---- Pictures

    @Test
    fun aVehicleWithAPictureHasTheUriOfItsLargeVersion() {
        val pictureId = pictures.addPicture()
        repository.seedVehicle("v1", "Family car", pictureId = pictureId)

        assertEquals(FakePictureStore.fakeUri("pictures", pictureId, PictureSize.LARGE), processor().state.pictureUri)
    }

    @Test
    fun aVehicleWithoutAPictureHasNoUri() {
        repository.seedVehicle("v1", "Van")

        assertNull(processor().state.pictureUri)
    }

    @Test
    fun aVehicleWhoseFileIsGoneHasNoUri() {
        repository.seedVehicle("v1", "Van", pictureId = "gone")

        assertNull(processor().state.pictureUri)
    }

    @Test
    fun changingOrRemovingThePictureChangesTheUri() {
        val first = pictures.addPicture()
        repository.seedVehicle("v1", "Van", pictureId = first)
        val processor = processor()

        val second = pictures.addPicture()
        repository.setPicture("v1", second)
        assertEquals(FakePictureStore.fakeUri("pictures", second, PictureSize.LARGE), processor.state.pictureUri)

        repository.setPicture("v1", null)
        assertNull(processor.state.pictureUri)
    }

    // ---- The vehicle's type

    @Test
    fun theStateHasTheVehiclesType() {
        repository.seedVehicle("v1", "Rig", type = VehicleType.BUS)

        assertEquals(VehicleType.BUS, processor().state.type)
    }

    @Test
    fun theStateFollowsAChangeOfType() {
        repository.seedVehicle("v1", "Rig", type = VehicleType.BUS)
        val processor = processor()

        repository.setType("v1", VehicleType.SUV)

        assertEquals(VehicleType.SUV, processor.state.type)
    }

    // ---- The vehicle's color

    @Test
    fun theStateHasTheVehiclesColor() {
        repository.seedVehicle("v1", "Rig", color = Rgb(0x8E24AA))

        assertEquals(Rgb(0x8E24AA), processor().state.color)
    }

    @Test
    fun theStateFollowsAChangeOfColor() {
        repository.seedVehicle("v1", "Rig", color = Rgb(0x8E24AA))
        val processor = processor()

        repository.setColor("v1", Rgb(0x43A047))

        assertEquals(Rgb(0x43A047), processor.state.color)
    }

    @Test
    fun theDetailsHaveTheDefaultColorUntilTheVehicleHasLoaded() {
        assertEquals(VehicleColors.default, VehicleDetailsState().color)
    }
}
