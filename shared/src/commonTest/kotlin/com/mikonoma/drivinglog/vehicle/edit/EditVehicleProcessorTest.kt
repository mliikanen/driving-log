package com.mikonoma.drivinglog.vehicle.edit

import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import com.mikonoma.drivinglog.vehicle.UpdateCall
import com.mikonoma.drivinglog.vehicle.initialEvent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.fuusio.kide.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class EditVehicleProcessorTest {

    private val repository = FakeVehicleRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository.seedVehicle("v1", "Family car", "ABC-123")
        repository.seedEvents("v1", listOf(initialEvent("e1", 100, 45_200_000)))
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun processor() = EditVehicleProcessor("v1", repository)

    @Test
    fun theFormStartsWithTheCurrentValues() {
        val state = processor().state

        assertTrue(state.loaded)
        assertEquals("Family car", state.name)
        assertEquals("ABC-123", state.licensePlate)
    }

    @Test
    fun aVehicleWithoutAPlateStartsWithAnEmptyPlateField() {
        repository.seedVehicle("v2", "Van", plate = null)
        assertEquals("", EditVehicleProcessor("v2", repository).state.licensePlate)
    }

    @Test
    fun changingNameAndPlateSavesThemTrimmed() = runTest {
        val processor = processor()
        processor.dispatch(EditVehicleIntent.NameChanged(" Estate car "))
        processor.dispatch(EditVehicleIntent.LicensePlateChanged("XYZ-789 "))

        processor.test {
            dispatch(EditVehicleIntent.Save)
            expectSideEffect(EditVehicleEffect.Saved)
        }

        assertEquals(listOf(UpdateCall("v1", "Estate car", "XYZ-789")), repository.updateCalls)
    }

    @Test
    fun clearingThePlateRemovesIt() = runTest {
        val processor = processor()
        processor.dispatch(EditVehicleIntent.LicensePlateChanged(""))

        processor.test {
            dispatch(EditVehicleIntent.Save)
            expectSideEffect(EditVehicleEffect.Saved)
        }

        assertEquals(null, repository.updateCalls.single().licensePlate)
    }

    @Test
    fun aWhitespaceOnlyPlateRemovesIt() = runTest {
        val processor = processor()
        processor.dispatch(EditVehicleIntent.LicensePlateChanged("   "))

        processor.test {
            dispatch(EditVehicleIntent.Save)
            expectSideEffect(EditVehicleEffect.Saved)
        }

        assertEquals(null, repository.updateCalls.single().licensePlate)
    }

    @Test
    fun anEmptyNameIsRefusedAndTheSavedValuesAreKept() {
        val processor = processor()
        processor.dispatch(EditVehicleIntent.NameChanged(""))

        processor.dispatch(EditVehicleIntent.Save)

        assertTrue(processor.state.nameError)
        assertEquals(emptyList(), repository.updateCalls)
    }

    @Test
    fun aWhitespaceOnlyNameIsRefused() {
        val processor = processor()
        processor.dispatch(EditVehicleIntent.NameChanged("   "))

        processor.dispatch(EditVehicleIntent.Save)

        assertTrue(processor.state.nameError)
        assertEquals(emptyList(), repository.updateCalls)
    }

    @Test
    fun leavingWithoutSavingKeepsThePreviousValues() = runTest {
        val processor = processor()
        processor.dispatch(EditVehicleIntent.NameChanged("Changed"))

        // Leaving is navigation only: nothing reaches the repository.
        assertEquals(emptyList(), repository.updateCalls)
        assertEquals("Family car", repository.observeVehicleName())
    }

    @Test
    fun savingDoesNotCreateLogEvents() = runTest {
        val before = repository.eventsOf("v1")
        val processor = processor()
        processor.dispatch(EditVehicleIntent.NameChanged("Estate car"))

        processor.test {
            dispatch(EditVehicleIntent.Save)
            expectSideEffect(EditVehicleEffect.Saved)
        }

        assertEquals(before, repository.eventsOf("v1"))
    }

    @Test
    fun laterDatabaseChangesDoNotOverwriteWhatTheUserIsTyping() = runTest {
        val processor = processor()
        processor.dispatch(EditVehicleIntent.NameChanged("Typing"))

        repository.updateVehicle("v1", "Changed elsewhere", null)

        assertEquals("Typing", processor.state.name)
    }

    @Test
    fun aRestoredStateKeepsItsTypedText() {
        val processor = processor()
        processor.restoreState(EditVehicleState(loaded = true, name = "Half typed", licensePlate = "AB"))

        assertEquals("Half typed", processor.state.name)
        assertEquals("AB", processor.state.licensePlate)
    }

    @Test
    fun aMissingVehicleIsReportedAndCannotBeSaved() {
        val processor = EditVehicleProcessor("missing", repository)
        assertTrue(processor.state.notFound)

        processor.dispatch(EditVehicleIntent.Save)

        assertEquals(emptyList(), repository.updateCalls)
    }

    @Test
    fun aFailedSaveCanBeRetried() {
        val processor = processor()
        processor.dispatch(EditVehicleIntent.NameChanged("Estate car"))
        repository.updateFailure = IllegalStateException("disk full")

        processor.dispatch(EditVehicleIntent.Save)

        assertFalse(processor.state.isSaving)
        repository.updateFailure = null
        processor.dispatch(EditVehicleIntent.Save)
        assertEquals(1, repository.updateCalls.size)
    }

    private suspend fun FakeVehicleRepository.observeVehicleName(): String? =
        observeVehicle("v1").first()?.vehicle?.name
}
