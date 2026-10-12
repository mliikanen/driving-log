package com.mikonoma.drivinglog.vehicle.log

import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.FuelType
import com.mikonoma.drivinglog.vehicle.domain.FuelUnit
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.RefuelingMileage
import com.mikonoma.drivinglog.vehicle.domain.Volume
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import com.mikonoma.drivinglog.vehicle.initialEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

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

    // ---- One row per event of an entry (atomic-log-events)

    /** A refueling logged with a trip-distance mileage (atomic-log-events): its entry holds a refueling and a distance. */
    private suspend fun logRefuelingWithMileage(at: Long): String = repository.addRefueling(
        "v1",
        ZonedMoment(Instant.fromEpochMilliseconds(at)),
        Volume(42_300),
        FuelUnit.LITERS,
        FuelType.DIESEL,
        filledUp = true,
        mileage = RefuelingMileage.Added(Distance(30_000)),
    )

    @Test
    fun aRefuelingWithMileageIsListedAsItsRefuelingAboveItsDistance() = runTest {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents("v1", listOf(initialEvent("i", 100, 45_200_000)))

        val refuelingId = logRefuelingWithMileage(200)

        assertEquals(listOf(refuelingId, "$refuelingId-mileage", "i"), VehicleLogProcessor("v1", repository).state.events.map { it.id })
    }
}
