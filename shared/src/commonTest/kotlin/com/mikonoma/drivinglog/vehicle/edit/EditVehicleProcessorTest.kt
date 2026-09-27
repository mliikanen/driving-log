package com.mikonoma.drivinglog.vehicle.edit

import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleColors
import com.mikonoma.drivinglog.vehicle.domain.VehicleType
import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import com.mikonoma.drivinglog.vehicle.UpdateCall
import com.mikonoma.drivinglog.vehicle.color.FakeColorExtractor
import com.mikonoma.drivinglog.vehicle.domain.PendingPicture
import com.mikonoma.drivinglog.vehicle.domain.PictureChange
import com.mikonoma.drivinglog.vehicle.initialEvent
import com.mikonoma.drivinglog.vehicle.picture.CropRect
import com.mikonoma.drivinglog.vehicle.picture.FakeImageCodec
import com.mikonoma.drivinglog.vehicle.picture.FakePictureStore
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.fuusio.kide.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class EditVehicleProcessorTest {

    private val repository = FakeVehicleRepository()
    private val pictures = FakePictureStore()
    private val codec = FakeImageCodec()
    private val colors = FakeColorExtractor()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository.seedVehicle("v1", "Family car", "ABC-123")
        repository.seedEvents("v1", listOf(initialEvent("e1", 100, 45_200_000)))
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun processor() = EditVehicleProcessor("v1", repository, pictures, codec, colors)

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
        assertEquals("", EditVehicleProcessor("v2", repository, pictures, codec, colors).state.licensePlate)
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

    // disable-invalid-save: the screen disables Save while the name is blank (see EditVehicleScreen.kt), so a
    // direct dispatch in that state is a defense-in-depth no-op, not a path a tap can reach; there is no longer an
    // error flag to assert.

    @Test
    fun anEmptyNameIsRefusedAndTheSavedValuesAreKept() {
        val processor = processor()
        processor.dispatch(EditVehicleIntent.NameChanged(""))

        processor.dispatch(EditVehicleIntent.Save)

        assertEquals(emptyList(), repository.updateCalls)
    }

    @Test
    fun aWhitespaceOnlyNameIsRefused() {
        val processor = processor()
        processor.dispatch(EditVehicleIntent.NameChanged("   "))

        processor.dispatch(EditVehicleIntent.Save)

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

        repository.updateVehicle("v1", "Changed elsewhere", null, VehicleType.CAR, VehicleColors.default)

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
        val processor = EditVehicleProcessor("missing", repository, pictures, codec, colors)
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

    // ---- The picture

    private val photo = byteArrayOf(1, 2, 3)
    private val crop = CropRect(500, 0, 3000)

    private fun seedPicture(): String {
        val pictureId = pictures.addPicture()
        repository.seedVehicle("v3", "Van", pictureId = pictureId)
        return pictureId
    }

    private fun EditVehicleProcessor.pickAndCrop() {
        dispatch(EditVehicleIntent.PhotoPicked(PhotoResult.Chosen(photo)))
        dispatch(EditVehicleIntent.CropConfirmed(crop))
    }

    @Test
    fun aTurnedCropReachesTheCodec() {
        val processor = processor()
        processor.dispatch(EditVehicleIntent.PhotoPicked(PhotoResult.Chosen(photo)))

        processor.dispatch(EditVehicleIntent.CropConfirmed(crop, quarterTurns = 2))

        assertEquals(2, codec.encodes.single().quarterTurns)
        assertTrue(processor.state.picture.draft is PictureDraft.Pending)
    }

    @Test
    fun theFormStartsWithTheSavedPictureUnchangedAndItsPreview() {
        val pictureId = seedPicture()

        val state = EditVehicleProcessor("v3", repository, pictures, codec, colors).state

        assertEquals(PictureDraft.Unchanged, state.picture.draft)
        assertEquals(pictureId, state.savedPictureId)
        assertEquals(FakePictureStore.fakeUri("pictures", pictureId, PictureSize.SMALL), state.previewUri)
    }

    @Test
    fun aVehicleWithoutAPictureHasNoPreview() {
        assertNull(processor().state.previewUri)
        assertNull(processor().state.savedPictureId)
    }

    @Test
    fun savingWithoutTouchingThePictureKeepsIt() = runTest {
        seedPicture()
        val processor = EditVehicleProcessor("v3", repository, pictures, codec, colors)
        processor.dispatch(EditVehicleIntent.NameChanged("Big van"))

        processor.test {
            dispatch(EditVehicleIntent.Save)
            expectSideEffect(EditVehicleEffect.Saved)
        }

        assertEquals(PictureChange.Keep, repository.updateCalls.single().picture)
    }

    @Test
    fun aChosenAndCroppedPictureReplacesTheSavedOneOnSave() = runTest {
        seedPicture()
        val processor = EditVehicleProcessor("v3", repository, pictures, codec, colors)
        processor.pickAndCrop()
        val pendingId = (processor.state.picture.draft as PictureDraft.Pending).pendingId
        assertEquals(FakePictureStore.fakeUri("pending", pendingId, PictureSize.SMALL), processor.state.previewUri)

        processor.test {
            dispatch(EditVehicleIntent.Save)
            expectSideEffect(EditVehicleEffect.Saved)
        }

        assertEquals(PictureChange.Replace(PendingPicture(pendingId)), repository.updateCalls.single().picture)
    }

    @Test
    fun removingThePictureClearsThePreviewButNotTheSavedFiles() = runTest {
        val pictureId = seedPicture()
        val processor = EditVehicleProcessor("v3", repository, pictures, codec, colors)

        processor.dispatch(EditVehicleIntent.PictureRemoved)

        assertEquals(PictureDraft.Removed, processor.state.picture.draft)
        assertNull(processor.state.previewUri)
        assertEquals(setOf(pictureId), pictures.everything()) // deleted only when the edit is saved

        processor.test {
            dispatch(EditVehicleIntent.Save)
            expectSideEffect(EditVehicleEffect.Saved)
        }
        assertEquals(PictureChange.Remove, repository.updateCalls.single().picture)
    }

    @Test
    fun removingThenLeavingKeepsTheSavedPictureAndSavesNothing() {
        val pictureId = seedPicture()
        val processor = EditVehicleProcessor("v3", repository, pictures, codec, colors)

        processor.dispatch(EditVehicleIntent.PictureRemoved)
        processor.dispatch(EditVehicleIntent.Left)

        assertEquals(emptyList(), repository.updateCalls)
        assertEquals(setOf(pictureId), pictures.everything())
    }

    @Test
    fun leavingAfterCroppingAnotherPictureDeletesOnlyThePendingFiles() {
        val pictureId = seedPicture()
        val processor = EditVehicleProcessor("v3", repository, pictures, codec, colors)
        processor.pickAndCrop()

        processor.dispatch(EditVehicleIntent.Left)

        assertEquals(setOf(pictureId), pictures.everything())
    }

    @Test
    fun aPhotoThatCannotBeOpenedKeepsTheSavedPictureAndShowsTheError() {
        seedPicture()
        val processor = EditVehicleProcessor("v3", repository, pictures, codec, colors)

        processor.dispatch(EditVehicleIntent.PhotoPicked(PhotoResult.Chosen(ByteArray(0))))

        assertEquals(PictureError.COULD_NOT_OPEN, processor.state.picture.error)
        assertEquals(PictureDraft.Unchanged, processor.state.picture.draft)
        assertTrue(processor.state.previewUri != null)
    }

    @Test
    fun cancellingTheCropKeepsTheSavedPicture() {
        seedPicture()
        val processor = EditVehicleProcessor("v3", repository, pictures, codec, colors)
        processor.dispatch(EditVehicleIntent.PhotoPicked(PhotoResult.Chosen(photo)))
        assertTrue(processor.state.picture.isCropping)

        processor.dispatch(EditVehicleIntent.CropCancelled)

        assertEquals(PictureDraft.Unchanged, processor.state.picture.draft)
        assertFalse(processor.state.picture.isCropping)
    }

    @Test
    fun aFailedSaveKeepsTheDraft() {
        seedPicture()
        val processor = EditVehicleProcessor("v3", repository, pictures, codec, colors)
        processor.pickAndCrop()
        val draft = processor.state.picture.draft
        repository.updateFailure = IllegalStateException("disk full")

        processor.dispatch(EditVehicleIntent.Save)

        assertEquals(draft, processor.state.picture.draft)
        assertFalse(processor.state.isSaving)
    }

    @Test
    fun aVehicleWhosePictureChangesElsewhereGetsTheNewPreviewWhileTheDraftIsUnchanged() {
        val first = seedPicture()
        val processor = EditVehicleProcessor("v3", repository, pictures, codec, colors)
        assertEquals(first, processor.state.savedPictureId)

        // The repository emits the vehicle again with another picture (say, the same vehicle edited on another screen).
        val second = pictures.addPicture()
        repository.setPicture("v3", second)

        assertEquals(second, processor.state.savedPictureId)
        assertEquals(FakePictureStore.fakeUri("pictures", second, PictureSize.SMALL), processor.state.previewUri)
    }

    // ---- The vehicle's type

    private fun seedTyped(type: VehicleType): EditVehicleProcessor {
        repository.seedVehicle("t1", "Rig", type = type)
        return EditVehicleProcessor("t1", repository, pictures, codec, colors)
    }

    @Test
    fun theSavedTypeIsSelected() {
        assertEquals(VehicleType.TRUCK, seedTyped(VehicleType.TRUCK).state.type)
    }

    @Test
    fun theFormAlwaysHasATypeOnceLoaded() {
        val state = processor().state

        assertTrue(state.loaded)
        assertEquals(VehicleType.CAR, state.type)
    }

    @Test
    fun changingTheTypeSavesIt() = runTest {
        val processor = seedTyped(VehicleType.CAR)
        processor.dispatch(EditVehicleIntent.TypeSelected(VehicleType.VAN))
        assertEquals(VehicleType.VAN, processor.state.type)

        processor.test {
            dispatch(EditVehicleIntent.Save)
            expectSideEffect(EditVehicleEffect.Saved)
        }

        assertEquals(VehicleType.VAN, repository.updateCalls.single().type)
    }

    @Test
    fun anEditThatDoesNotTouchTheTypeKeepsIt() = runTest {
        val processor = seedTyped(VehicleType.SCOOTER)
        processor.dispatch(EditVehicleIntent.NameChanged("Renamed"))

        processor.test {
            dispatch(EditVehicleIntent.Save)
            expectSideEffect(EditVehicleEffect.Saved)
        }

        assertEquals(VehicleType.SCOOTER, repository.updateCalls.single().type)
    }

    @Test
    fun leavingWithoutSavingKeepsTheSavedType() {
        val processor = seedTyped(VehicleType.BUS)
        processor.dispatch(EditVehicleIntent.TypeSelected(VehicleType.OTHER))

        processor.dispatch(EditVehicleIntent.Left)

        assertEquals(emptyList(), repository.updateCalls)
        // The saved vehicle is untouched: a new processor over it starts with the saved type.
        assertEquals(VehicleType.BUS, EditVehicleProcessor("t1", repository, pictures, codec, colors).state.type)
    }

    @Test
    fun aVehicleMigratedToCarCanBeChangedToAnotherType() = runTest {
        val processor = seedTyped(VehicleType.CAR) // what the migration gives a vehicle from before types existed
        processor.dispatch(EditVehicleIntent.TypeSelected(VehicleType.BUS))

        processor.test {
            dispatch(EditVehicleIntent.Save)
            expectSideEffect(EditVehicleEffect.Saved)
        }

        assertEquals(VehicleType.BUS, repository.updateCalls.single().type)
    }

    @Test
    fun aRestoredStateKeepsTheChoiceMadeBeforeTheProcessorWasRecreated() {
        repository.seedVehicle("t2", "Rig", type = VehicleType.CAR)
        val restored = EditVehicleProcessor("t2", repository, pictures, codec, colors)
        restored.restoreState(EditVehicleState(loaded = true, name = "Rig", type = VehicleType.SUV))

        assertEquals(VehicleType.SUV, restored.state.type)
    }

    // ---- The vehicle's color

    private val red = Rgb(0xE53935)
    private val blue = Rgb(0x1E88E5)

    private fun seedColored(color: Rgb, pictureId: String? = null): EditVehicleProcessor {
        repository.seedVehicle("c1", "Rig", color = color, pictureId = pictureId)
        return EditVehicleProcessor("c1", repository, pictures, codec, colors)
    }

    @Test
    fun theSavedColorIsSelected() {
        assertEquals(Rgb(0x00796B), seedColored(Rgb(0x00796B)).state.color)
    }

    @Test
    fun aVehicleWithTheDefaultColorStartsWithIt() {
        assertEquals(VehicleColors.default, processor().state.color)
    }

    @Test
    fun openingTheFormTakesNoColorFromAPicture() {
        val processor = seedColored(Rgb(0x00796B), pictureId = pictures.addPicture())

        assertEquals(Rgb(0x00796B), processor.state.color)
        assertNull(processor.state.pictureColor)
        assertEquals(emptyList(), colors.extracted)
        assertEquals(emptyList(), codec.sampledBytes)
    }

    @Test
    fun choosingAColorSelectsIt() {
        val processor = seedColored(Rgb(0x00796B))

        processor.dispatch(EditVehicleIntent.ColorSelected(red))

        assertEquals(red, processor.state.color)
    }

    @Test
    fun changingTheColorSavesIt() = runTest {
        val processor = seedColored(Rgb(0x00796B))
        processor.dispatch(EditVehicleIntent.ColorSelected(red))

        processor.test {
            dispatch(EditVehicleIntent.Save)
            expectSideEffect(EditVehicleEffect.Saved)
        }

        assertEquals(red, repository.updateCalls.single().color)
    }

    @Test
    fun anEditThatDoesNotTouchTheColorKeepsIt() = runTest {
        val processor = seedColored(Rgb(0x00796B))
        processor.dispatch(EditVehicleIntent.NameChanged("Renamed"))

        processor.test {
            dispatch(EditVehicleIntent.Save)
            expectSideEffect(EditVehicleEffect.Saved)
        }

        assertEquals(Rgb(0x00796B), repository.updateCalls.single().color)
        assertEquals(Rgb(0x00796B), repository.observeVehicle("c1").first()!!.vehicle.color)
    }

    @Test
    fun leavingWithoutSavingKeepsTheSavedColor() = runTest {
        val processor = seedColored(Rgb(0x00796B))
        processor.dispatch(EditVehicleIntent.ColorSelected(red))

        processor.dispatch(EditVehicleIntent.Left)

        assertEquals(emptyList(), repository.updateCalls)
        assertEquals(Rgb(0x00796B), repository.observeVehicle("c1").first()!!.vehicle.color)
    }

    @Test
    fun aConfirmedCropSetsTheColorAndTheOfferedPictureColor() {
        colors.color = red
        val processor = seedColored(Rgb(0x00796B))

        processor.pickAndCrop()

        assertEquals(red, processor.state.color)
        assertEquals(red, processor.state.pictureColor)
    }

    @Test
    fun choosingAPresetAfterThePictureKeepsThePictureColorOffered() {
        colors.color = red
        val processor = seedColored(Rgb(0x00796B))
        processor.pickAndCrop()

        processor.dispatch(EditVehicleIntent.ColorSelected(blue))

        assertEquals(blue, processor.state.color)
        assertEquals(red, processor.state.pictureColor)
    }

    @Test
    fun cancellingACropKeepsTheColor() {
        val processor = seedColored(Rgb(0x00796B), pictureId = pictures.addPicture())
        processor.dispatch(EditVehicleIntent.PhotoPicked(PhotoResult.Chosen(photo)))

        processor.dispatch(EditVehicleIntent.CropCancelled)

        assertEquals(Rgb(0x00796B), processor.state.color)
        assertNull(processor.state.pictureColor)
    }

    @Test
    fun removingTheSavedPictureKeepsTheColor() {
        val processor = seedColored(Rgb(0x00796B), pictureId = pictures.addPicture())

        processor.dispatch(EditVehicleIntent.PictureRemoved)

        assertEquals(Rgb(0x00796B), processor.state.color)
        assertNull(processor.state.pictureColor)
    }

    @Test
    fun removingAPictureCroppedInThisFormKeepsItsColorAndDropsThePictureColor() {
        colors.color = red
        val processor = seedColored(Rgb(0x00796B))
        processor.pickAndCrop()

        processor.dispatch(EditVehicleIntent.PictureRemoved)

        assertEquals(red, processor.state.color)
        assertNull(processor.state.pictureColor)
    }

    @Test
    fun aPhotoWithoutAColorLeavesTheColorAsItWas() {
        colors.color = null
        val processor = seedColored(Rgb(0x00796B))

        processor.pickAndCrop()

        assertEquals(Rgb(0x00796B), processor.state.color)
        assertNull(processor.state.pictureColor)
    }

    @Test
    fun aRestoredStateKeepsTheColorAndThePictureColor() {
        colors.color = red
        val first = seedColored(Rgb(0x00796B))
        first.pickAndCrop()
        first.dispatch(EditVehicleIntent.ColorSelected(blue))
        val saved = checkNotNull(first.stateToSave())

        val restored = EditVehicleProcessor("c1", repository, pictures, codec, colors)
        restored.restoreState(saved)

        assertEquals(blue, restored.state.color)
        assertEquals(red, restored.state.pictureColor)
    }

    @Test
    fun aColorEditDoesNotChangeTheTypeOrThePictureChange() = runTest {
        val processor = seedColored(Rgb(0x00796B))
        processor.dispatch(EditVehicleIntent.ColorSelected(red))

        processor.test {
            dispatch(EditVehicleIntent.Save)
            expectSideEffect(EditVehicleEffect.Saved)
        }

        val call = repository.updateCalls.single()
        assertEquals(VehicleType.CAR, call.type)
        assertEquals(PictureChange.Keep, call.picture)
    }

    @Test
    fun theSavedColorIsKeptAsTheOldColorWhileTheFormIsOpen() {
        colors.color = red
        val processor = seedColored(Rgb(0x00796B))
        assertEquals(Rgb(0x00796B), processor.state.savedColor)

        processor.dispatch(EditVehicleIntent.ColorSelected(blue))
        processor.pickAndCrop()
        processor.dispatch(EditVehicleIntent.PictureRemoved)

        assertEquals(Rgb(0x00796B), processor.state.savedColor)
    }

    @Test
    fun aRestoredStateKeepsTheOldColor() {
        val first = seedColored(Rgb(0x00796B))
        first.dispatch(EditVehicleIntent.ColorSelected(blue))
        val saved = checkNotNull(first.stateToSave())

        val restored = EditVehicleProcessor("c1", repository, pictures, codec, colors)
        restored.restoreState(saved)

        assertEquals(Rgb(0x00796B), restored.state.savedColor)
        assertEquals(blue, restored.state.color)
    }
}
