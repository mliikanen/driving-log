package com.mikonoma.drivinglog.vehicle.picture

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * What each way of leaving the chooser does to an event's photo strip (`ask-camera-for-camera-app`): only a refused camera shows the
 * missing-permission message, and leaving without a photo or an unreadable one never does.
 */
class EventPhotoDraftEditorTest {

    private val store = FakePictureStore()
    private val editor = EventPhotoDraftEditor(store, FakeImageCodec())
    private val withPhotos = EventPhotoDraft(pendingIds = listOf("p1", "p2"))

    @Test
    fun aRefusedCameraSetsTheCameraErrorAndKeepsThePhotos() = runTest {
        val next = editor.photoPicked(withPhotos, PhotoResult.CameraDenied)

        assertEquals(PictureError.CAMERA_DENIED, next.error)
        assertEquals(listOf("p1", "p2"), next.pendingIds)
        assertEquals(emptySet(), store.everything())
    }

    @Test
    fun leavingTheChooserChangesNothingAndShowsNoMessage() = runTest {
        assertEquals(withPhotos, editor.photoPicked(withPhotos, PhotoResult.Cancelled))
    }

    @Test
    fun leavingTheChooserKeepsAnEarlierErrorAsItWas() = runTest {
        val denied = withPhotos.copy(error = PictureError.CAMERA_DENIED)

        assertEquals(denied, editor.photoPicked(denied, PhotoResult.Cancelled))
    }

    @Test
    fun anUnreadablePhotoSetsTheCouldNotOpenErrorNotTheCameraOne() = runTest {
        val next = editor.photoPicked(withPhotos, PhotoResult.Unreadable)

        assertEquals(PictureError.COULD_NOT_OPEN, next.error)
        assertEquals(listOf("p1", "p2"), next.pendingIds)
    }

    @Test
    fun aPhotoThatIsNotAnImageReplacesACameraErrorWithItsOwn() = runTest {
        val denied = withPhotos.copy(error = PictureError.CAMERA_DENIED)

        assertEquals(PictureError.COULD_NOT_OPEN, editor.photoPicked(denied, PhotoResult.Chosen(ByteArray(0))).error)
    }

    @Test
    fun aPhotoAddedAfterARefusalClearsTheMessage() = runTest {
        val denied = withPhotos.copy(error = PictureError.CAMERA_DENIED)

        val next = editor.photoPicked(denied, PhotoResult.Chosen(byteArrayOf(1, 2, 3)))

        assertNull(next.error)
        assertEquals(3, next.pendingIds.size)
    }

    @Test
    fun dismissingClearsTheCameraError() {
        assertNull(editor.errorDismissed(withPhotos.copy(error = PictureError.CAMERA_DENIED)).error)
    }
}
