package com.mikonoma.drivinglog.vehicle.log

import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.initialEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleLogProcessorTest {

    private val repository = FakeVehicleRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun showsEveryEventNewestFirst() {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents("v1", (7 downTo 1).map { initialEvent("e$it", it * 100L, it * 1_000L) })

        val state = VehicleLogProcessor("v1", repository).state

        assertEquals(false, state.isLoading)
        assertEquals((7 downTo 1).map { "e$it" }, state.events.map { it.id })
    }

    @Test
    fun readingsUseTheVehiclesUnit() {
        repository.seedVehicle("v1", "Family car", unit = OdometerUnit.KILOMETERS_TENTHS)
        repository.seedEvents("v1", listOf(initialEvent("e1", 100, 45_200_300)))

        val state = VehicleLogProcessor("v1", repository).state

        assertEquals(OdometerUnit.KILOMETERS_TENTHS, state.unit)
        assertEquals("Family car", state.vehicleName)
    }

    @Test
    fun theLogFollowsNewEvents() {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents("v1", listOf(initialEvent("e1", 100, 1_000)))
        val processor = VehicleLogProcessor("v1", repository)

        repository.seedEvents("v1", listOf(initialEvent("e2", 200, 2_000), initialEvent("e1", 100, 1_000)))

        assertEquals(listOf("e2", "e1"), processor.state.events.map { it.id })
    }

    @Test
    fun aMissingVehicleIsReported() {
        val state = VehicleLogProcessor("missing", repository).state

        assertEquals(false, state.isLoading)
        assertTrue(state.notFound)
    }
}
