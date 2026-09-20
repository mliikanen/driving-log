package com.mikonoma.drivinglog.vehicle.list

import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.fuusio.kide.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleListProcessorTest {

    private val repository = FakeVehicleRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun names(processor: VehicleListProcessor) = processor.state.vehicles.map { it.name }

    @Test
    fun anEmptyRepositoryShowsAnEmptyLoadedList() {
        val processor = VehicleListProcessor(repository)

        assertEquals(false, processor.state.isLoading)
        assertEquals(emptyList(), processor.state.vehicles)
    }

    @Test
    fun vehiclesAreOrderedByNameWithoutRegardToCase() {
        repository.seedVehicle("1", "van")
        repository.seedVehicle("2", "Bike")
        repository.seedVehicle("3", "Family car")

        assertEquals(listOf("Bike", "Family car", "van"), names(VehicleListProcessor(repository)))
    }

    @Test
    fun lowercaseNamesAreNotSortedAfterUppercaseOnes() {
        repository.seedVehicle("1", "zebra")
        repository.seedVehicle("2", "Yak")
        repository.seedVehicle("3", "apple")

        assertEquals(listOf("apple", "Yak", "zebra"), names(VehicleListProcessor(repository)))
    }

    @Test
    fun namesThatDifferOnlyInCaseKeepTheOrderTheyWereAdded() {
        repository.seedVehicle("1", "Van", createdAtMillis = 20)
        repository.seedVehicle("2", "van", createdAtMillis = 10)
        repository.seedVehicle("3", "VAN", createdAtMillis = 30)

        assertEquals(listOf("2", "1", "3"), VehicleListProcessor(repository).state.vehicles.map { it.id })
    }

    @Test
    fun nonAsciiLettersAreFoldedToo() {
        repository.seedVehicle("1", "Äiti", createdAtMillis = 2)
        repository.seedVehicle("2", "äiti", createdAtMillis = 1)

        assertEquals(listOf("2", "1"), VehicleListProcessor(repository).state.vehicles.map { it.id })
    }

    @Test
    fun eachItemCarriesNameAndPlate() {
        repository.seedVehicle("1", "Family car", plate = "ABC-123")
        repository.seedVehicle("2", "Van", plate = null)

        val items = VehicleListProcessor(repository).state.vehicles
        assertEquals(VehicleListItem("1", "Family car", "ABC-123"), items[0])
        assertEquals(VehicleListItem("2", "Van", null), items[1])
    }

    @Test
    fun theListFollowsTheRepository() {
        val processor = VehicleListProcessor(repository)
        assertEquals(emptyList(), names(processor))

        repository.seedVehicle("1", "Van")

        assertEquals(listOf("Van"), names(processor))
    }

    @Test
    fun openingAVehicleNavigatesToItsDetails() = runTest {
        VehicleListProcessor(repository).test {
            dispatch(VehicleListIntent.OpenVehicle("v7"))
            expectSideEffect(VehicleListEffect.ShowDetails("v7"))
        }
    }

    @Test
    fun addingNavigatesToTheAddScreen() = runTest {
        VehicleListProcessor(repository).test {
            dispatch(VehicleListIntent.AddVehicle)
            expectSideEffect(VehicleListEffect.ShowAdd)
        }
    }

    @Test
    fun theProcessorStartsLoadingBeforeTheFirstEmission() {
        assertTrue(VehicleListState().isLoading)
    }
}
