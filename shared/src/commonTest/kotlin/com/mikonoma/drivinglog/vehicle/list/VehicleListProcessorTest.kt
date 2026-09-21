package com.mikonoma.drivinglog.vehicle.list

import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleColors
import com.mikonoma.drivinglog.vehicle.domain.VehicleType
import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import com.mikonoma.drivinglog.vehicle.picture.FakeVehiclePictureStore
import com.mikonoma.drivinglog.vehicle.picture.PictureSize
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
class VehicleListProcessorTest {

    private val repository = FakeVehicleRepository()
    private val pictures = FakeVehiclePictureStore()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun names(processor: VehicleListProcessor) = processor.state.vehicles.map { it.name }

    @Test
    fun anEmptyRepositoryShowsAnEmptyLoadedList() {
        val processor = VehicleListProcessor(repository, pictures)

        assertEquals(false, processor.state.isLoading)
        assertEquals(emptyList(), processor.state.vehicles)
    }

    @Test
    fun vehiclesAreOrderedByNameWithoutRegardToCase() {
        repository.seedVehicle("1", "van")
        repository.seedVehicle("2", "Bike")
        repository.seedVehicle("3", "Family car")

        assertEquals(listOf("Bike", "Family car", "van"), names(VehicleListProcessor(repository, pictures)))
    }

    @Test
    fun lowercaseNamesAreNotSortedAfterUppercaseOnes() {
        repository.seedVehicle("1", "zebra")
        repository.seedVehicle("2", "Yak")
        repository.seedVehicle("3", "apple")

        assertEquals(listOf("apple", "Yak", "zebra"), names(VehicleListProcessor(repository, pictures)))
    }

    @Test
    fun namesThatDifferOnlyInCaseKeepTheOrderTheyWereAdded() {
        repository.seedVehicle("1", "Van", createdAtMillis = 20)
        repository.seedVehicle("2", "van", createdAtMillis = 10)
        repository.seedVehicle("3", "VAN", createdAtMillis = 30)

        assertEquals(listOf("2", "1", "3"), VehicleListProcessor(repository, pictures).state.vehicles.map { it.id })
    }

    @Test
    fun nonAsciiLettersAreFoldedToo() {
        repository.seedVehicle("1", "Äiti", createdAtMillis = 2)
        repository.seedVehicle("2", "äiti", createdAtMillis = 1)

        assertEquals(listOf("2", "1"), VehicleListProcessor(repository, pictures).state.vehicles.map { it.id })
    }

    @Test
    fun eachItemCarriesNameAndPlate() {
        repository.seedVehicle("1", "Family car", plate = "ABC-123")
        repository.seedVehicle("2", "Van", plate = null)

        val items = VehicleListProcessor(repository, pictures).state.vehicles
        assertEquals(VehicleListItem("1", "Family car", "ABC-123", type = VehicleType.CAR), items[0])
        assertEquals(VehicleListItem("2", "Van", null, type = VehicleType.CAR), items[1])
    }

    @Test
    fun theListFollowsTheRepository() {
        val processor = VehicleListProcessor(repository, pictures)
        assertEquals(emptyList(), names(processor))

        repository.seedVehicle("1", "Van")

        assertEquals(listOf("Van"), names(processor))
    }

    @Test
    fun openingAVehicleNavigatesToItsDetails() = runTest {
        VehicleListProcessor(repository, pictures).test {
            dispatch(VehicleListIntent.OpenVehicle("v7"))
            expectSideEffect(VehicleListEffect.ShowDetails("v7"))
        }
    }

    @Test
    fun addingNavigatesToTheAddScreen() = runTest {
        VehicleListProcessor(repository, pictures).test {
            dispatch(VehicleListIntent.AddVehicle)
            expectSideEffect(VehicleListEffect.ShowAdd)
        }
    }

    @Test
    fun theProcessorStartsLoadingBeforeTheFirstEmission() {
        assertTrue(VehicleListState().isLoading)
    }

    // ---- Pictures

    @Test
    fun aVehicleWithAPictureHasTheUriOfItsSmallVersion() {
        val pictureId = pictures.addPicture()
        repository.seedVehicle("v1", "Family car", pictureId = pictureId)

        val item = VehicleListProcessor(repository, pictures).state.vehicles.single()

        assertEquals(FakeVehiclePictureStore.fakeUri("pictures", pictureId, PictureSize.SMALL), item.pictureUri)
    }

    @Test
    fun aVehicleWithoutAPictureHasNoUri() {
        repository.seedVehicle("v1", "Van")

        assertNull(VehicleListProcessor(repository, pictures).state.vehicles.single().pictureUri)
    }

    @Test
    fun aVehicleWhoseFileIsGoneHasNoUri() {
        repository.seedVehicle("v1", "Van", pictureId = "gone")

        assertNull(VehicleListProcessor(repository, pictures).state.vehicles.single().pictureUri)
    }

    @Test
    fun eachVehicleHasItsOwnUriAndChangingThePictureChangesIt() {
        val a = pictures.addPicture()
        val b = pictures.addPicture()
        repository.seedVehicle("v1", "A", pictureId = a)
        repository.seedVehicle("v2", "B", pictureId = b)
        val processor = VehicleListProcessor(repository, pictures)
        assertEquals(FakeVehiclePictureStore.fakeUri("pictures", a, PictureSize.SMALL), processor.state.vehicles[0].pictureUri)
        assertEquals(FakeVehiclePictureStore.fakeUri("pictures", b, PictureSize.SMALL), processor.state.vehicles[1].pictureUri)

        val replacement = pictures.addPicture()
        repository.setPicture("v1", replacement)

        assertEquals(FakeVehiclePictureStore.fakeUri("pictures", replacement, PictureSize.SMALL), processor.state.vehicles[0].pictureUri)
    }

    @Test
    fun theItemHoldsOnlyAUriNeverAnIdOrBytes() {
        val pictureId = pictures.addPicture()
        repository.seedVehicle("v1", "A", pictureId = pictureId)

        val text = VehicleListProcessor(repository, pictures).state.vehicles.single().toString()

        assertEquals(false, pictureId in text.replace(FakeVehiclePictureStore.fakeUri("pictures", pictureId, PictureSize.SMALL), ""))
    }

    // ---- The vehicle's type

    @Test
    fun eachItemHasItsVehiclesType() {
        repository.seedVehicle("1", "Bike", type = VehicleType.MOTORCYCLE)
        repository.seedVehicle("2", "Rig", type = VehicleType.TRUCK)

        val items = VehicleListProcessor(repository, pictures).state.vehicles

        assertEquals(listOf(VehicleType.MOTORCYCLE, VehicleType.TRUCK), items.map { it.type })
    }

    @Test
    fun anItemFollowsAChangeOfType() {
        repository.seedVehicle("1", "Bike", type = VehicleType.MOTORCYCLE)
        val processor = VehicleListProcessor(repository, pictures)

        repository.setType("1", VehicleType.SCOOTER)

        assertEquals(VehicleType.SCOOTER, processor.state.vehicles.single().type)
    }

    // ---- The vehicle's color

    @Test
    fun eachItemHasItsVehiclesColor() {
        repository.seedVehicle("1", "Bike", color = Rgb(0xE53935))
        repository.seedVehicle("2", "Rig")

        val items = VehicleListProcessor(repository, pictures).state.vehicles

        assertEquals(listOf(Rgb(0xE53935), VehicleColors.default), items.map { it.color })
    }

    @Test
    fun anItemFollowsAChangeOfColor() {
        repository.seedVehicle("1", "Bike", color = Rgb(0xE53935))
        val processor = VehicleListProcessor(repository, pictures)

        repository.setColor("1", Rgb(0x1E88E5))

        assertEquals(Rgb(0x1E88E5), processor.state.vehicles.single().color)
    }
}
