package com.mikonoma.drivinglog.vehicle.eventdetails

import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import com.mikonoma.drivinglog.vehicle.distanceEvent
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import com.mikonoma.drivinglog.vehicle.initialEvent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailsProcessorTest {

    private val repository = FakeVehicleRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun processor(vehicleId: String = "v1", eventId: String) = EventDetailsProcessor(vehicleId, eventId, repository)

    @Test
    fun showsTheMatchingEventAndTheVehiclesUnit() {
        repository.seedVehicle("v1", "Family car", unit = OdometerUnit.KILOMETERS_TENTHS)
        repository.seedEvents("v1", listOf(distanceEvent("e2", 200, 30_000), initialEvent("e1", 100, 45_200_000)))

        val state = processor(eventId = "e2").state

        assertEquals(false, state.isLoading)
        assertEquals(false, state.notFound)
        assertEquals("e2", state.event?.id)
        assertEquals(OdometerUnit.KILOMETERS_TENTHS, state.unit)
    }

    @Test
    fun anUnknownEventIsReported() {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents("v1", listOf(initialEvent("e1", 100, 45_200_000)))

        val state = processor(eventId = "no-such-event").state

        assertEquals(false, state.isLoading)
        assertTrue(state.notFound)
        assertNull(state.event)
    }

    @Test
    fun aMissingVehicleIsReported() {
        val state = processor(vehicleId = "missing", eventId = "e1").state

        assertEquals(false, state.isLoading)
        assertTrue(state.notFound)
    }

    @Test
    fun theInitialOdometerEventHasNoNote() {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents("v1", listOf(initialEvent("e1", 100, 45_200_000)))

        val state = processor(eventId = "e1").state

        assertNull(state.event?.note)
    }

    @Test
    fun aDistanceEventCarriesItsNote() {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents(
            "v1",
            listOf(VehicleEvent.DistanceEntry("e2", ZonedMoment(Instant.fromEpochMilliseconds(200)), Distance(30_000), note = "borrowed to Sam")),
        )

        val state = processor(eventId = "e2").state

        assertEquals("borrowed to Sam", state.event?.note)
    }
}
