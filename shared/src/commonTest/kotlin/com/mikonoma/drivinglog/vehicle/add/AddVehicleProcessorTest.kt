package com.mikonoma.drivinglog.vehicle.add

import com.mikonoma.drivinglog.locale.DeviceLocale
import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.vehicle.AddCall
import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.format.formatSteps
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.fuusio.kide.test.test

private class FakeLocale(override val regionCode: String?) : DeviceLocale {
    override fun numberSymbols() = NumberSymbols.ENGLISH_US
}

@OptIn(ExperimentalCoroutinesApi::class)
class AddVehicleProcessorTest {

    private val repository = FakeVehicleRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun processor(region: String? = "FI") = AddVehicleProcessor(repository, FakeLocale(region))

    private fun AddVehicleProcessor.type(vararg digits: Int) {
        for (d in digits) dispatch(AddVehicleIntent.OdometerEdited(state.entry.digits + d))
    }

    private fun AddVehicleProcessor.shown() = state.entry.let { formatSteps(it.steps, it.unit.hasTenths, NumberSymbols.ENGLISH_US) }

    // The preselected unit.

    @Test
    fun milesArePreselectedInTheUnitedStates() {
        assertEquals(OdometerUnit.MILES, processor("US").state.entry.unit)
    }

    @Test
    fun kilometersArePreselectedInFinland() {
        assertEquals(OdometerUnit.KILOMETERS, processor("FI").state.entry.unit)
    }

    @Test
    fun kilometersArePreselectedForAnUnknownRegion() {
        assertEquals(OdometerUnit.KILOMETERS, processor(null).state.entry.unit)
    }

    @Test
    fun theFormStartsEmptyWithAZeroOdometer() {
        val state = processor().state
        assertEquals("", state.name)
        assertEquals("", state.licensePlate)
        assertEquals(0, state.entry.steps)
        assertFalse(state.nameError)
    }

    @Test
    fun theUserCanChangeTheUnit() {
        val processor = processor("US")
        processor.dispatch(AddVehicleIntent.UnitSelected(OdometerUnit.KILOMETERS_TENTHS))
        assertEquals(OdometerUnit.KILOMETERS_TENTHS, processor.state.entry.unit)
    }

    // The odometer field.

    @Test
    fun typingFillsAtenthsOdometerFromTheRight() {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.UnitSelected(OdometerUnit.KILOMETERS_TENTHS))
        val shown = buildList {
            for (d in listOf(1, 2, 3)) {
                processor.type(d)
                add(processor.shown())
            }
        }
        assertEquals(listOf("0.1", "1.2", "12.3"), shown)
    }

    @Test
    fun typingFillsAWholeOdometerFromTheRight() {
        val processor = processor()
        val shown = buildList {
            for (d in listOf(1, 2, 3)) {
                processor.type(d)
                add(processor.shown())
            }
        }
        assertEquals(listOf("1", "12", "123"), shown)
    }

    @Test
    fun backspaceRestoresTheEarlierValue() {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.UnitSelected(OdometerUnit.KILOMETERS_TENTHS))
        processor.type(1, 2)
        assertEquals("1.2", processor.shown())
        processor.type(3)
        assertEquals("12.3", processor.shown())
        processor.dispatch(AddVehicleIntent.OdometerEdited("12")) // the keyboard deleted the last digit
        assertEquals("1.2", processor.shown())
    }

    @Test
    fun typeDeleteToZeroAndTypeAgain() {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.UnitSelected(OdometerUnit.KILOMETERS_TENTHS))
        val shown = buildList {
            processor.type(1); add(processor.shown())
            processor.type(2); add(processor.shown())
            processor.dispatch(AddVehicleIntent.OdometerEdited("1")); add(processor.shown())
            processor.dispatch(AddVehicleIntent.OdometerEdited("")); add(processor.shown())
            processor.type(2); add(processor.shown())
            processor.type(3); add(processor.shown())
            processor.type(0); add(processor.shown())
        }
        assertEquals(listOf("0.1", "1.2", "0.1", "0.0", "0.2", "2.3", "23.0"), shown)
    }

    @Test
    fun nonDigitsFromTheKeyboardAreIgnored() {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.UnitSelected(OdometerUnit.KILOMETERS_TENTHS))
        processor.type(1, 2, 3)
        for (typed in listOf(",", ".", "-")) {
            processor.dispatch(AddVehicleIntent.OdometerEdited(processor.state.entry.digits + typed))
        }
        assertEquals("12.3", processor.shown())
    }

    @Test
    fun clearResetsTheOdometer() {
        val processor = processor()
        processor.type(1, 2, 3)
        processor.dispatch(AddVehicleIntent.OdometerCleared)
        assertEquals(0, processor.state.entry.steps)
    }

    @Test
    fun changingTheUnitKeepsTheNumberAndRescales() {
        val processor = processor()
        processor.type(1, 2, 3)
        processor.dispatch(AddVehicleIntent.UnitSelected(OdometerUnit.KILOMETERS_TENTHS))
        assertEquals("123.0", processor.shown())
        processor.dispatch(AddVehicleIntent.UnitSelected(OdometerUnit.KILOMETERS))
        assertEquals("123", processor.shown())
    }

    @Test
    fun changingToAWholeUnitRoundsTheTenths() {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.UnitSelected(OdometerUnit.MILES_TENTHS))
        processor.type(1, 2, 6)
        processor.dispatch(AddVehicleIntent.UnitSelected(OdometerUnit.MILES))
        assertEquals("13", processor.shown())
    }

    // Saving.

    @Test
    fun savingWithAllFieldsAddsTheVehicle() = runTest {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.NameChanged("Family car"))
        processor.dispatch(AddVehicleIntent.LicensePlateChanged("ABC-123"))
        processor.type(4, 5, 2, 0, 0)

        processor.test {
            dispatch(AddVehicleIntent.Save)
            expectSideEffect(AddVehicleEffect.Saved)
        }

        assertEquals(
            listOf(AddCall("Family car", "ABC-123", OdometerUnit.KILOMETERS, Distance(45_200_000))),
            repository.addCalls,
        )
    }

    @Test
    fun savingWithOnlyANameUsesTheDefaultUnitAndZero() = runTest {
        val processor = processor("US")
        processor.dispatch(AddVehicleIntent.NameChanged("Van"))

        processor.test {
            dispatch(AddVehicleIntent.Save)
            expectSideEffect(AddVehicleEffect.Saved)
        }

        assertEquals(listOf(AddCall("Van", null, OdometerUnit.MILES, Distance.ZERO)), repository.addCalls)
    }

    @Test
    fun aTenthsReadingIsSavedAsExactMeters() = runTest {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.NameChanged("Van"))
        processor.dispatch(AddVehicleIntent.UnitSelected(OdometerUnit.KILOMETERS_TENTHS))
        processor.type(4, 5, 2, 0, 0, 3)

        processor.test {
            dispatch(AddVehicleIntent.Save)
            expectSideEffect(AddVehicleEffect.Saved)
        }

        assertEquals(Distance(45_200_300), repository.addCalls.single().initialOdometer)
    }

    @Test
    fun nameAndPlateAreTrimmed() = runTest {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.NameChanged("  Family car "))
        processor.dispatch(AddVehicleIntent.LicensePlateChanged(" ABC-123  "))

        processor.test {
            dispatch(AddVehicleIntent.Save)
            expectSideEffect(AddVehicleEffect.Saved)
        }

        assertEquals("Family car", repository.addCalls.single().name)
        assertEquals("ABC-123", repository.addCalls.single().licensePlate)
    }

    @Test
    fun aWhitespaceOnlyPlateIsSavedAsNone() = runTest {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.NameChanged("Van"))
        processor.dispatch(AddVehicleIntent.LicensePlateChanged("   "))

        processor.test {
            dispatch(AddVehicleIntent.Save)
            expectSideEffect(AddVehicleEffect.Saved)
        }

        assertEquals(null, repository.addCalls.single().licensePlate)
    }

    @Test
    fun anEmptyNameIsRefusedAndNothingIsSaved() {
        val processor = processor()

        processor.dispatch(AddVehicleIntent.Save)

        assertTrue(processor.state.nameError)
        assertEquals(emptyList(), repository.addCalls)
    }

    @Test
    fun aWhitespaceOnlyNameIsRefusedAndNothingIsSaved() {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.NameChanged("    "))

        processor.dispatch(AddVehicleIntent.Save)

        assertTrue(processor.state.nameError)
        assertEquals(emptyList(), repository.addCalls)
    }

    @Test
    fun theNameErrorClearsWhenTheUserTypes() {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.Save)
        assertTrue(processor.state.nameError)

        processor.dispatch(AddVehicleIntent.NameChanged("V"))

        assertFalse(processor.state.nameError)
    }

    @Test
    fun aFailedSaveCanBeRetried() {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.NameChanged("Van"))
        repository.addFailure = IllegalStateException("disk full")

        processor.dispatch(AddVehicleIntent.Save)

        assertFalse(processor.state.isSaving)
        assertEquals(emptyList(), repository.addCalls)

        repository.addFailure = null
        processor.dispatch(AddVehicleIntent.Save)
        assertEquals(1, repository.addCalls.size)
    }

    @Test
    fun leavingWithoutSavingAddsNothing() {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.NameChanged("Van"))
        processor.type(1, 2, 3)

        // Leaving the screen is navigation only: nothing reaches the repository.
        assertEquals(emptyList(), repository.addCalls)
    }
}
