package com.mikonoma.drivinglog.vehicle.edit

import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import com.mikonoma.drivinglog.vehicle.UpdateCall
import com.mikonoma.drivinglog.vehicle.domain.PendingPicture
import com.mikonoma.drivinglog.vehicle.domain.PictureChange
import com.mikonoma.drivinglog.vehicle.initialEvent
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.fuusio.kide.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class EditVehicleProcessorTest {

    private val repository = FakeVehicleRepository()
    private val pictures = FakeVehiclePictureStore()
    private val codec = FakeImageCodec()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository.seedVehicle("v1", "Family car", "ABC-123")
        repository.seedEvents("v1", listOf(initialEvent("e1", 100, 45_200_000)))
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun processor() = EditVehicleProcessor("v1", repository, pictures, codec)

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
        assertEquals("", EditVehicleProcessor("v2", repository, pictures, codec).state.licensePlate)
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
        val processor = EditVehicleProcessor("missing", repository, pictures, codec)
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
    fun theFormStartsWithTheSavedPictureUnchangedAndItsPreview() {
        val pictureId = seedPicture()

        val state = EditVehicleProcessor("v3", repository, pictures, codec).state

        assertEquals(PictureDraft.Unchanged, state.picture.draft)
        assertEquals(pictureId, state.savedPictureId)
        assertEquals(FakeVehiclePictureStore.fakeUri("pictures", pictureId, PictureSize.SMALL), state.previewUri)
    }

    @Test
    fun aVehicleWithoutAPictureHasNoPreview() {
        assertNull(processor().state.previewUri)
        assertNull(processor().state.savedPictureId)
    }

    @Test
    fun savingWithoutTouchingThePictureKeepsIt() = runTest {
        seedPicture()
        val processor = EditVehicleProcessor("v3", repository, pictures, codec)
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
        val processor = EditVehicleProcessor("v3", repository, pictures, codec)
        processor.pickAndCrop()
        val pendingId = (processor.state.picture.draft as PictureDraft.Pending).pendingId
        assertEquals(FakeVehiclePictureStore.fakeUri("pending", pendingId, PictureSize.SMALL), processor.state.previewUri)

        processor.test {
            dispatch(EditVehicleIntent.Save)
            expectSideEffect(EditVehicleEffect.Saved)
        }

        assertEquals(PictureChange.Replace(PendingPicture(pendingId)), repository.updateCalls.single().picture)
    }

    @Test
    fun removingThePictureClearsThePreviewButNotTheSavedFiles() = runTest {
        val pictureId = seedPicture()
        val processor = EditVehicleProcessor("v3", repository, pictures, codec)

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
        val processor = EditVehicleProcessor("v3", repository, pictures, codec)

        processor.dispatch(EditVehicleIntent.PictureRemoved)
        processor.dispatch(EditVehicleIntent.Left)

        assertEquals(emptyList(), repository.updateCalls)
        assertEquals(setOf(pictureId), pictures.everything())
    }

    @Test
    fun leavingAfterCroppingAnotherPictureDeletesOnlyThePendingFiles() {
        val pictureId = seedPicture()
        val processor = EditVehicleProcessor("v3", repository, pictures, codec)
        processor.pickAndCrop()

        processor.dispatch(EditVehicleIntent.Left)

        assertEquals(setOf(pictureId), pictures.everything())
    }

    @Test
    fun aPhotoThatCannotBeOpenedKeepsTheSavedPictureAndShowsTheError() {
        seedPicture()
        val processor = EditVehicleProcessor("v3", repository, pictures, codec)

        processor.dispatch(EditVehicleIntent.PhotoPicked(PhotoResult.Chosen(ByteArray(0))))

        assertEquals(PictureError.COULD_NOT_OPEN, processor.state.picture.error)
        assertEquals(PictureDraft.Unchanged, processor.state.picture.draft)
        assertTrue(processor.state.previewUri != null)
    }

    @Test
    fun cancellingTheCropKeepsTheSavedPicture() {
        seedPicture()
        val processor = EditVehicleProcessor("v3", repository, pictures, codec)
        processor.dispatch(EditVehicleIntent.PhotoPicked(PhotoResult.Chosen(photo)))
        assertTrue(processor.state.picture.isCropping)

        processor.dispatch(EditVehicleIntent.CropCancelled)

        assertEquals(PictureDraft.Unchanged, processor.state.picture.draft)
        assertFalse(processor.state.picture.isCropping)
    }

    @Test
    fun aFailedSaveKeepsTheDraft() {
        seedPicture()
        val processor = EditVehicleProcessor("v3", repository, pictures, codec)
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
        val processor = EditVehicleProcessor("v3", repository, pictures, codec)
        assertEquals(first, processor.state.savedPictureId)

        // The repository emits the vehicle again with another picture (say, the same vehicle edited on another screen).
        val second = pictures.addPicture()
        repository.setPicture("v3", second)

        assertEquals(second, processor.state.savedPictureId)
        assertEquals(FakeVehiclePictureStore.fakeUri("pictures", second, PictureSize.SMALL), processor.state.previewUri)
    }
}
