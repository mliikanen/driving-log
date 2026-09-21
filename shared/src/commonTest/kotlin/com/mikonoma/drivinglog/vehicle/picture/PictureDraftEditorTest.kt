package com.mikonoma.drivinglog.vehicle.picture

import com.mikonoma.drivinglog.vehicle.domain.PendingPicture
import com.mikonoma.drivinglog.vehicle.domain.PictureChange
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class PictureDraftEditorTest {

    private val store = FakeVehiclePictureStore()
    private val codec = FakeImageCodec(width = 4000, height = 3000)
    private val adding = PictureDraftEditor(store, codec, PictureDraft.None)
    private val editing = PictureDraftEditor(store, codec, PictureDraft.Removed)

    private val photo = byteArrayOf(1, 2, 3)
    private val start = PictureEditState()
    private val crop = CropRect(500, 0, 3000)

    // ---- Choosing a photo

    @Test
    fun aChosenPhotoIsKeptAndTheCropOpens() = runTest {
        val next = adding.photoPicked(start, PhotoResult.Chosen(photo))

        assertTrue(next.isCropping)
        assertContentEquals(photo, store.sources.getValue(next.cropSourceId!!))
        assertEquals(PictureDraft.None, next.draft)
        assertNull(next.error)
    }

    @Test
    fun leavingThePickerChangesNothing() = runTest {
        assertEquals(start, adding.photoPicked(start, PhotoResult.Cancelled))
        assertEquals(emptySet(), store.everything())
    }

    @Test
    fun aFileThatIsNotAnImageSetsTheErrorAndKeepsTheDraft() = runTest {
        val pending = PictureEditState(draft = PictureDraft.Pending("p1"))

        val next = editing.photoPicked(pending, PhotoResult.Chosen(ByteArray(0)))

        assertEquals(PictureError.COULD_NOT_OPEN, next.error)
        assertEquals(PictureDraft.Pending("p1"), next.draft)
        assertFalse(next.isCropping)
        assertEquals(emptySet(), store.everything())
    }

    @Test
    fun aPhotoThatIsTooLargeIsRefusedAsUnreadable() = runTest {
        val tooBig = ByteArray((MAX_PHOTO_BYTES + 1).toInt())

        val next = adding.photoPicked(start, PhotoResult.Chosen(tooBig))

        assertEquals(PictureError.COULD_NOT_OPEN, next.error)
        assertEquals(emptySet(), store.everything())
    }

    @Test
    fun aProviderThatGaveAnUnreadableImageSetsTheCouldNotOpenError() = runTest {
        val next = adding.photoPicked(start, PhotoResult.Unreadable)

        assertEquals(PictureError.COULD_NOT_OPEN, next.error)
        assertFalse(next.isCropping)
        assertEquals(emptySet(), store.everything())
    }

    @Test
    fun aRefusedCameraSetsItsOwnErrorAndChangesNothingElse() = runTest {
        val pending = PictureEditState(draft = PictureDraft.Pending("p1"))

        val next = editing.photoPicked(pending, PhotoResult.CameraDenied)

        assertEquals(PictureError.CAMERA_DENIED, next.error)
        assertEquals(PictureDraft.Pending("p1"), next.draft)
        assertFalse(next.isCropping)
        assertEquals(emptySet(), store.everything())
    }

    @Test
    fun aNewPhotoClearsAnEarlierCameraError() = runTest {
        val denied = adding.photoPicked(start, PhotoResult.CameraDenied)

        assertNull(adding.photoPicked(denied, PhotoResult.Chosen(photo)).error)
    }

    @Test
    fun leavingTheChooserKeepsAnEarlierErrorUntilTheNextAction() = runTest {
        val denied = adding.photoPicked(start, PhotoResult.CameraDenied)

        assertEquals(PictureError.CAMERA_DENIED, adding.photoPicked(denied, PhotoResult.Cancelled).error)
    }

    @Test
    fun anErrorClearsWhenAPhotoIsChosenThatCanBeOpened() = runTest {
        val next = adding.photoPicked(adding.photoPicked(start, PhotoResult.Chosen(ByteArray(0))), PhotoResult.Chosen(photo))

        assertNull(next.error)
        assertTrue(next.isCropping)
    }

    @Test
    fun choosingAgainWhileCroppingReplacesTheEarlierPhoto() = runTest {
        val first = adding.photoPicked(start, PhotoResult.Chosen(photo))
        val second = adding.photoPicked(first, PhotoResult.Chosen(byteArrayOf(9)))

        assertEquals(setOf(second.cropSourceId!!), store.sources.keys)
        assertContentEquals(byteArrayOf(9), store.sources.getValue(second.cropSourceId!!))
    }

    @Test
    fun theCropScreenGetsThePhotoDecoded() = runTest {
        val image = adding.cropImage(adding.photoPicked(start, PhotoResult.Chosen(photo)))

        assertEquals(4000, image!!.width)
        assertEquals(3000, image.height)
    }

    @Test
    fun withNoPhotoThereIsNothingToCrop() = runTest {
        assertNull(adding.cropImage(start))
        assertNull(adding.cropImage(PictureEditState(cropSourceId = "gone")))
    }

    // ---- Confirming the crop

    @Test
    fun aConfirmedCropBecomesThePendingPictureAndTheSourceIsDropped() = runTest {
        val cropping = adding.photoPicked(start, PhotoResult.Chosen(photo))
        val id = cropping.cropSourceId!!

        val next = adding.cropConfirmed(cropping, crop)

        assertEquals(PictureDraft.Pending(id), next.draft)
        assertFalse(next.isCropping)
        assertNull(next.error)
        assertEquals(emptySet(), store.sources.keys)
        assertEquals(setOf(id), store.pending.keys)
    }

    @Test
    fun theVersionsAreEncodedFromTheCropAtTheSidesForItsSize() = runTest {
        val cropping = adding.photoPicked(start, PhotoResult.Chosen(photo))

        adding.cropConfirmed(cropping, CropRect(10, 20, 400))

        val encode = codec.encodes.single()
        assertEquals(CropRect(10, 20, 400), encode.crop)
        assertEquals(PictureSides(256, 400), encode.sides)
        assertContentEquals(photo, encode.bytes)
    }

    @Test
    fun confirmingReplacesAnEarlierPendingPicture() = runTest {
        val first = adding.cropConfirmed(adding.photoPicked(start, PhotoResult.Chosen(photo)), crop)
        val second = adding.cropConfirmed(adding.photoPicked(first, PhotoResult.Chosen(byteArrayOf(5))), crop)

        assertEquals(setOf((second.draft as PictureDraft.Pending).pendingId), store.pending.keys)
    }

    @Test
    fun confirmingWithNoPhotoChangesNothing() = runTest {
        assertEquals(start, adding.cropConfirmed(start, crop))
    }

    @Test
    fun aPhotoWhoseFileIsGoneClosesTheCropWithTheErrorAndKeepsTheDraft() = runTest {
        val state = PictureEditState(draft = PictureDraft.Unchanged, cropSourceId = "gone")

        val next = editing.cropConfirmed(state, crop)

        assertNull(next.cropSourceId)
        assertEquals(PictureError.COULD_NOT_OPEN, next.error)
        assertEquals(PictureDraft.Unchanged, next.draft)
    }

    @Test
    fun aPhotoThatCannotBeEncodedClosesTheCropWithTheErrorAndDropsIt() = runTest {
        val cropping = adding.photoPicked(start, PhotoResult.Chosen(photo))
        codec.width = 10 // still decodes; make the encode fail by emptying the stored bytes
        store.sources[cropping.cropSourceId!!] = ByteArray(0)

        val next = adding.cropConfirmed(cropping, crop)

        assertEquals(PictureError.COULD_NOT_OPEN, next.error)
        assertNull(next.cropSourceId)
        assertEquals(emptySet(), store.everything())
    }

    // ---- Cancelling

    @Test
    fun cancellingTheCropDropsThePhotoAndKeepsTheDraft() = runTest {
        val pending = adding.cropConfirmed(adding.photoPicked(start, PhotoResult.Chosen(photo)), crop)
        val cropping = adding.photoPicked(pending, PhotoResult.Chosen(byteArrayOf(7)))

        val next = adding.cropCancelled(cropping)

        assertNull(next.cropSourceId)
        assertEquals(pending.draft, next.draft)
        assertEquals(setOf((pending.draft as PictureDraft.Pending).pendingId), store.everything())
    }

    @Test
    fun cancellingWithNothingOpenChangesNothing() = runTest {
        assertEquals(start, adding.cropCancelled(start))
    }

    // ---- Removing

    @Test
    fun removingAPendingPictureDeletesItsFilesAndAddingHasNone() = runTest {
        val pending = adding.cropConfirmed(adding.photoPicked(start, PhotoResult.Chosen(photo)), crop)

        val next = adding.removed(pending)

        assertEquals(PictureDraft.None, next.draft)
        assertEquals(emptySet(), store.everything())
    }

    @Test
    fun removingOnTheEditScreenMarksThePictureRemoved() = runTest {
        assertEquals(PictureDraft.Removed, editing.removed(PictureEditState(draft = PictureDraft.Unchanged)).draft)
    }

    @Test
    fun removingAPendingPictureOnTheEditScreenAlsoDeletesItsFiles() = runTest {
        val pending = editing.cropConfirmed(editing.photoPicked(PictureEditState(draft = PictureDraft.Unchanged), PhotoResult.Chosen(photo)), crop)

        val next = editing.removed(pending)

        assertEquals(PictureDraft.Removed, next.draft)
        assertEquals(emptySet(), store.everything())
    }

    // ---- Leaving and the error

    @Test
    fun leavingDiscardsThePendingPictureAndAPhotoBeingCropped() = runTest {
        val pending = adding.cropConfirmed(adding.photoPicked(start, PhotoResult.Chosen(photo)), crop)
        val cropping = adding.photoPicked(pending, PhotoResult.Chosen(byteArrayOf(4)))

        adding.discardAll(cropping)

        assertEquals(emptySet(), store.everything())
    }

    @Test
    fun leavingWithNothingKeptIsHarmless() = runTest {
        adding.discardAll(start)
        editing.discardAll(PictureEditState(draft = PictureDraft.Unchanged))
    }

    @Test
    fun dismissingTheErrorClearsIt() {
        assertNull(adding.errorDismissed(PictureEditState(error = PictureError.CAMERA_DENIED)).error)
    }

    // ---- The preview

    @Test
    fun thePreviewIsThePendingPicturesSmallVersion() = runTest {
        val pending = adding.cropConfirmed(adding.photoPicked(start, PhotoResult.Chosen(photo)), crop)
        val id = (pending.draft as PictureDraft.Pending).pendingId

        assertEquals(FakeVehiclePictureStore.fakeUri("pending", id, PictureSize.SMALL), adding.previewUri(pending, null))
    }

    @Test
    fun theUnchangedPreviewIsTheSavedPicturesSmallVersion() = runTest {
        val saved = store.addPicture()

        assertEquals(
            FakeVehiclePictureStore.fakeUri("pictures", saved, PictureSize.SMALL),
            editing.previewUri(PictureEditState(draft = PictureDraft.Unchanged), saved),
        )
    }

    @Test
    fun noPictureRemovedAndAMissingFileHaveNoPreview() = runTest {
        assertNull(adding.previewUri(start, "picture-1"))
        assertNull(editing.previewUri(PictureEditState(draft = PictureDraft.Removed), "picture-1"))
        assertNull(editing.previewUri(PictureEditState(draft = PictureDraft.Unchanged), "gone"))
        assertNull(editing.previewUri(PictureEditState(draft = PictureDraft.Unchanged), null))
    }

    // ---- What saving does

    @Test
    fun anAddedVehicleIsSavedWithThePendingPictureOnly() {
        assertEquals(PendingPicture("p1"), PictureDraft.Pending("p1").forAdd())
        assertNull(PictureDraft.None.forAdd())
    }

    @Test
    fun anEditMapsEachDraftToAChange() {
        assertEquals(PictureChange.Keep, PictureDraft.Unchanged.toChange())
        assertEquals(PictureChange.Remove, PictureDraft.Removed.toChange())
        assertEquals(PictureChange.Replace(PendingPicture("p1")), PictureDraft.Pending("p1").toChange())
        assertNotNull(PictureChange.Keep)
    }
}
