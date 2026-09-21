package com.mikonoma.drivinglog.vehicle.add

import com.mikonoma.drivinglog.locale.DeviceLocale
import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.vehicle.AddCall
import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.PendingPicture
import com.mikonoma.drivinglog.vehicle.format.formatSteps
import com.mikonoma.drivinglog.vehicle.picture.CropRect
import com.mikonoma.drivinglog.vehicle.picture.FakeImageCodec
import com.mikonoma.drivinglog.vehicle.picture.FakeVehiclePictureStore
import com.mikonoma.drivinglog.vehicle.picture.PhotoResult
import com.mikonoma.drivinglog.vehicle.picture.PictureDraft
import com.mikonoma.drivinglog.vehicle.picture.PictureError
import com.mikonoma.drivinglog.vehicle.picture.PictureEditState
import com.mikonoma.drivinglog.vehicle.picture.PictureSize
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
    private val pictures = FakeVehiclePictureStore()
    private val codec = FakeImageCodec()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun processor(region: String? = "FI") = AddVehicleProcessor(repository, FakeLocale(region), pictures, codec)

    private fun AddVehicleProcessor.type(vararg digits: Int) {
        for (d in digits) dispatch(AddVehicleIntent.OdometerEdited(state.entry.digits + d))
    }

    /** What the odometer field shows: nothing at all while empty. */
    private fun AddVehicleProcessor.shown() =
        state.entry.let { entry -> entry.steps?.let { formatSteps(it, entry.unit.hasTenths, NumberSymbols.ENGLISH_US) } ?: "" }

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
    fun theFormStartsEmptyWithNoOdometerAtAll() {
        val state = processor().state
        assertEquals("", state.name)
        assertEquals("", state.licensePlate)
        assertTrue(state.entry.isEmpty)
        assertFalse(state.nameError)
        assertFalse(state.odometerError)
    }

    @Test
    fun theOdometerFieldStartsEmptyInEveryUnit() {
        for (unit in OdometerUnit.entries) {
            val processor = processor()
            processor.dispatch(AddVehicleIntent.UnitSelected(unit))
            assertEquals("", processor.shown(), unit.name)
        }
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
    fun typeDeleteToEmptyAndTypeAgain() {
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
        assertEquals(listOf("0.1", "1.2", "0.1", "", "0.2", "2.3", "23.0"), shown)
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
    fun clearEmptiesTheOdometer() {
        val processor = processor()
        processor.type(1, 2, 3)
        processor.dispatch(AddVehicleIntent.OdometerCleared)
        assertTrue(processor.state.entry.isEmpty)
        assertEquals("", processor.shown())
    }

    @Test
    fun aTypedZeroIsShownAndNotEmpty() {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.UnitSelected(OdometerUnit.KILOMETERS_TENTHS))
        processor.dispatch(AddVehicleIntent.OdometerEdited("0"))
        assertEquals("0.0", processor.shown())
        assertFalse(processor.state.entry.isEmpty)
        processor.dispatch(AddVehicleIntent.OdometerEdited(""))
        assertEquals("", processor.shown())
    }

    @Test
    fun changingTheUnitOfAnEmptyOdometerLeavesItEmpty() {
        val processor = processor("US")
        processor.dispatch(AddVehicleIntent.UnitSelected(OdometerUnit.KILOMETERS_TENTHS))
        assertEquals("", processor.shown())
        assertTrue(processor.state.entry.isEmpty)
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

    // A first typed zero is kept as an unshown prefix, so backspace undoes exactly what was typed.

    private fun AddVehicleProcessor.edit(text: String) = dispatch(AddVehicleIntent.OdometerEdited(text))

    private fun tenthsProcessor(): AddVehicleProcessor =
        processor().also { it.dispatch(AddVehicleIntent.UnitSelected(OdometerUnit.KILOMETERS_TENTHS)) }

    @Test
    fun aFirstZeroIsKeptWhileTypingMoreDigits() {
        val processor = tenthsProcessor()
        val shown = buildList {
            processor.type(0); add(processor.shown())
            processor.type(5); add(processor.shown())
            processor.type(3); add(processor.shown())
        }
        assertEquals(listOf("0.0", "0.5", "5.3"), shown)
    }

    @Test
    fun backspaceGoesBackThroughThePrefixZero() {
        val processor = tenthsProcessor()
        processor.type(0, 5, 3)

        val shown = buildList {
            processor.edit("05"); add(processor.shown())
            processor.edit("0"); add(processor.shown())
            processor.edit(""); add(processor.shown())
        }

        assertEquals(listOf("0.5", "0.0", ""), shown)
    }

    @Test
    fun backspaceOnAPrefixedEntryLeavesTheTypedZeroThatCanBeSaved() = runTest {
        val processor = tenthsProcessor()
        processor.dispatch(AddVehicleIntent.NameChanged("Van"))
        processor.type(0, 5)
        processor.edit("0")
        assertEquals("0.0", processor.shown())

        processor.test {
            dispatch(AddVehicleIntent.Save)
            expectSideEffect(AddVehicleEffect.Saved)
        }

        assertEquals(Distance.ZERO, repository.addCalls.single().initialOdometer)
    }

    @Test
    fun backspacingTheWholePrefixedEntryBlocksSavingAgain() {
        val processor = tenthsProcessor()
        processor.dispatch(AddVehicleIntent.NameChanged("Van"))
        processor.type(0, 5)
        processor.edit("0")
        processor.edit("")

        processor.dispatch(AddVehicleIntent.Save)

        assertEquals("", processor.shown())
        assertTrue(processor.state.odometerError)
        assertEquals(emptyList(), repository.addCalls)
    }

    @Test
    fun extraLeadingZerosFromTheKeyboardAreIgnored() {
        val processor = tenthsProcessor()
        processor.type(0)
        processor.edit("00")
        assertEquals("0.0", processor.shown())
        assertEquals("0", processor.state.entry.digits)
        processor.edit("000")
        assertEquals("0", processor.state.entry.digits)

        processor.type(5)
        assertEquals("0.5", processor.shown())
        assertEquals("05", processor.state.entry.digits)
    }

    @Test
    fun aPrefixedEntrySavesItsValue() = runTest {
        val processor = tenthsProcessor()
        processor.dispatch(AddVehicleIntent.NameChanged("Van"))
        processor.type(0, 5, 3)

        processor.test {
            dispatch(AddVehicleIntent.Save)
            expectSideEffect(AddVehicleEffect.Saved)
        }

        assertEquals(Distance(5_300), repository.addCalls.single().initialOdometer)
    }

    @Test
    fun aPastedLeadingZeroBehavesLikeATypedOne() {
        val processor = tenthsProcessor()
        processor.edit("0123")
        assertEquals("12.3", processor.shown())

        val shown = buildList {
            for (text in listOf("012", "01", "0", "")) {
                processor.edit(text)
                add(processor.shown())
            }
        }
        assertEquals(listOf("1.2", "0.1", "0.0", ""), shown)
    }

    @Test
    fun changingTheUnitDropsThePrefixButKeepsTheNumber() {
        val processor = processor()
        processor.type(0, 5)
        assertEquals("5", processor.shown())

        processor.dispatch(AddVehicleIntent.UnitSelected(OdometerUnit.KILOMETERS_TENTHS))

        assertEquals("5.0", processor.shown())
        assertFalse(processor.state.entry.zeroPrefix)
    }

    @Test
    fun aPrefixedEntrySurvivesTheRestoredState() {
        val processor = tenthsProcessor()
        processor.type(0, 5)
        val saved = checkNotNull(processor.stateToSave())

        // restoreState must come before any intent, so restore into a fresh processor.
        val restored = processor()
        restored.restoreState(saved)

        assertEquals("0.5", restored.shown())
        assertTrue(restored.state.entry.zeroPrefix)
        restored.edit("0")
        assertEquals("0.0", restored.shown())
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
    fun savingWithANameTheDefaultUnitAndATypedZeroSavesAZeroReading() = runTest {
        val processor = processor("US")
        processor.dispatch(AddVehicleIntent.NameChanged("Van"))
        processor.dispatch(AddVehicleIntent.OdometerEdited("0"))

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
        processor.type(1)

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
        processor.type(1)

        processor.test {
            dispatch(AddVehicleIntent.Save)
            expectSideEffect(AddVehicleEffect.Saved)
        }

        assertEquals(null, repository.addCalls.single().licensePlate)
    }

    @Test
    fun anEmptyNameIsRefusedAndNothingIsSaved() {
        val processor = processor()
        processor.type(1)

        processor.dispatch(AddVehicleIntent.Save)

        assertTrue(processor.state.nameError)
        assertFalse(processor.state.odometerError)
        assertEquals(emptyList(), repository.addCalls)
    }

    @Test
    fun anEmptyOdometerIsRefusedAndNothingIsSaved() {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.NameChanged("Van"))

        processor.dispatch(AddVehicleIntent.Save)

        assertTrue(processor.state.odometerError)
        assertFalse(processor.state.nameError)
        assertEquals(emptyList(), repository.addCalls)
    }

    @Test
    fun aMissingNameAndAMissingOdometerAreBothReported() {
        val processor = processor()

        processor.dispatch(AddVehicleIntent.Save)

        assertTrue(processor.state.nameError)
        assertTrue(processor.state.odometerError)
        assertEquals(emptyList(), repository.addCalls)
    }

    @Test
    fun theOdometerErrorClearsWhenADigitIsTyped() {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.NameChanged("Van"))
        processor.dispatch(AddVehicleIntent.Save)
        assertTrue(processor.state.odometerError)

        processor.type(5)

        assertFalse(processor.state.odometerError)
    }

    @Test
    fun theOdometerErrorStaysWhileNothingIsEntered() {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.NameChanged("Van"))
        processor.dispatch(AddVehicleIntent.Save)

        processor.dispatch(AddVehicleIntent.OdometerEdited("."))
        processor.dispatch(AddVehicleIntent.OdometerCleared)

        assertTrue(processor.state.odometerError)
    }

    @Test
    fun aRefusedSaveCanBeFixedAndSavedWithoutLeavingTheScreen() = runTest {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.NameChanged("Van"))
        processor.dispatch(AddVehicleIntent.Save)
        assertEquals(emptyList(), repository.addCalls)
        processor.type(4, 2)

        processor.test {
            dispatch(AddVehicleIntent.Save)
            expectSideEffect(AddVehicleEffect.Saved)
        }

        assertEquals(1, repository.addCalls.size)
    }

    @Test
    fun aTypedZeroSatisfiesTheRequiredOdometer() {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.NameChanged("Van"))
        processor.dispatch(AddVehicleIntent.OdometerEdited("0"))

        processor.dispatch(AddVehicleIntent.Save)

        assertFalse(processor.state.odometerError)
        assertEquals(Distance.ZERO, repository.addCalls.single().initialOdometer)
    }

    @Test
    fun aWhitespaceOnlyNameIsRefusedAndNothingIsSaved() {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.NameChanged("    "))
        processor.type(1)

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
        processor.type(1)
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

    // ---- The picture

    private val photo = byteArrayOf(1, 2, 3)
    private val crop = CropRect(500, 0, 3000)

    private fun AddVehicleProcessor.pickAndCrop() {
        dispatch(AddVehicleIntent.PhotoPicked(PhotoResult.Chosen(photo)))
        dispatch(AddVehicleIntent.CropConfirmed(crop))
    }

    @Test
    fun theFormStartsWithNoPicture() {
        val state = processor().state

        assertEquals(PictureDraft.None, state.picture.draft)
        assertNull(state.previewUri)
        assertNull(state.cropImage)
    }

    @Test
    fun aChosenPhotoOpensTheCropWithTheDecodedPhoto() {
        val processor = processor()

        processor.dispatch(AddVehicleIntent.PhotoPicked(PhotoResult.Chosen(photo)))

        assertTrue(processor.state.picture.isCropping)
        assertEquals(4000, processor.state.cropImage!!.width)
        assertEquals(3000, processor.state.cropImage!!.height)
        assertEquals(PictureDraft.None, processor.state.picture.draft)
    }

    @Test
    fun leavingThePickerChangesNothing() {
        val processor = processor()

        processor.dispatch(AddVehicleIntent.PhotoPicked(PhotoResult.Cancelled))

        assertEquals(PictureEditState(), processor.state.picture)
    }

    @Test
    fun aRefusedCameraShowsItsMessageAndNoCrop() {
        val processor = processor()

        processor.dispatch(AddVehicleIntent.PhotoPicked(PhotoResult.CameraDenied))

        assertEquals(PictureError.CAMERA_DENIED, processor.state.picture.error)
        assertFalse(processor.state.picture.isCropping)
        assertEquals(PictureDraft.None, processor.state.picture.draft)
    }

    @Test
    fun anUnreadableImageFromTheChooserShowsTheCouldNotOpenError() {
        val processor = processor()

        processor.dispatch(AddVehicleIntent.PhotoPicked(PhotoResult.Unreadable))

        assertEquals(PictureError.COULD_NOT_OPEN, processor.state.picture.error)
    }

    @Test
    fun aPhotoThatCannotBeOpenedShowsTheErrorAndNoCrop() {
        val processor = processor()

        processor.dispatch(AddVehicleIntent.PhotoPicked(PhotoResult.Chosen(ByteArray(0))))

        assertEquals(PictureError.COULD_NOT_OPEN, processor.state.picture.error)
        assertFalse(processor.state.picture.isCropping)
        assertNull(processor.state.cropImage)
        processor.dispatch(AddVehicleIntent.PictureErrorDismissed)
        assertNull(processor.state.picture.error)
    }

    @Test
    fun aConfirmedCropIsThePreviewAndClosesTheCrop() {
        val processor = processor()

        processor.pickAndCrop()

        val draft = processor.state.picture.draft as PictureDraft.Pending
        assertFalse(processor.state.picture.isCropping)
        assertNull(processor.state.cropImage)
        assertEquals(FakeVehiclePictureStore.fakeUri("pending", draft.pendingId, PictureSize.SMALL), processor.state.previewUri)
    }

    @Test
    fun cancellingTheCropKeepsTheFormAsItWas() {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.PhotoPicked(PhotoResult.Chosen(photo)))

        processor.dispatch(AddVehicleIntent.CropCancelled)

        assertEquals(PictureEditState(), processor.state.picture)
        assertNull(processor.state.cropImage)
        assertEquals(emptySet(), pictures.everything())
    }

    @Test
    fun removingThePictureClearsThePreviewAndDeletesItsFiles() {
        val processor = processor()
        processor.pickAndCrop()

        processor.dispatch(AddVehicleIntent.PictureRemoved)

        assertEquals(PictureDraft.None, processor.state.picture.draft)
        assertNull(processor.state.previewUri)
        assertEquals(emptySet(), pictures.everything())
    }

    @Test
    fun aVehicleIsSavedWithItsPendingPicture() = runTest {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.NameChanged("Family car"))
        processor.type(4, 5, 2, 0, 0)
        processor.pickAndCrop()
        val pendingId = (processor.state.picture.draft as PictureDraft.Pending).pendingId

        processor.test {
            dispatch(AddVehicleIntent.Save)
            expectSideEffect(AddVehicleEffect.Saved)
        }

        assertEquals(PendingPicture(pendingId), repository.addCalls.single().picture)
    }

    @Test
    fun aVehicleWithoutAPictureIsSavedWithNone() = runTest {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.NameChanged("Van"))
        processor.type(1)

        processor.test {
            dispatch(AddVehicleIntent.Save)
            expectSideEffect(AddVehicleEffect.Saved)
        }

        assertNull(repository.addCalls.single().picture)
    }

    @Test
    fun aFailedSaveKeepsThePictureForAnotherTry() {
        val processor = processor()
        processor.dispatch(AddVehicleIntent.NameChanged("Van"))
        processor.type(1)
        processor.pickAndCrop()
        val draft = processor.state.picture.draft
        repository.addFailure = IllegalStateException("disk full")

        processor.dispatch(AddVehicleIntent.Save)

        assertEquals(draft, processor.state.picture.draft)
        assertTrue(processor.state.previewUri != null)
        assertEquals(1, pictures.pending.size)
    }

    @Test
    fun leavingWithoutSavingDeletesThePendingFiles() {
        val processor = processor()
        processor.pickAndCrop()
        processor.dispatch(AddVehicleIntent.PhotoPicked(PhotoResult.Chosen(byteArrayOf(9))))

        processor.dispatch(AddVehicleIntent.Left)

        assertEquals(emptySet(), pictures.everything())
        assertEquals(emptyList(), repository.addCalls)
    }

    @Test
    fun aRestoredPendingPictureShowsItsPreviewAfterARefresh() {
        val pendingId = pictures.addPending()
        val processor = processor()
        processor.restoreState(processor.state.copy(picture = PictureEditState(draft = PictureDraft.Pending(pendingId))))
        assertNull(processor.state.previewUri) // the preview is not saved, it is rebuilt

        processor.dispatch(AddVehicleIntent.PictureRefresh)

        assertEquals(FakeVehiclePictureStore.fakeUri("pending", pendingId, PictureSize.SMALL), processor.state.previewUri)
    }

    @Test
    fun aRestoredCropReopensWithTheDecodedPhotoAfterARefresh() = runTest {
        val sourceId = pictures.putPendingSource(photo)
        val processor = processor()
        processor.restoreState(processor.state.copy(picture = PictureEditState(cropSourceId = sourceId)))
        assertNull(processor.state.cropImage)

        processor.dispatch(AddVehicleIntent.PictureRefresh)

        assertEquals(4000, processor.state.cropImage!!.width)
    }

    @Test
    fun aRestoredCropWhosePhotoIsGoneClosesWithTheError() {
        val processor = processor()
        processor.restoreState(processor.state.copy(picture = PictureEditState(cropSourceId = "gone")))

        processor.dispatch(AddVehicleIntent.PictureRefresh)

        assertNull(processor.state.cropImage)
        assertFalse(processor.state.picture.isCropping)
        assertEquals(PictureError.COULD_NOT_OPEN, processor.state.picture.error)
    }
}
