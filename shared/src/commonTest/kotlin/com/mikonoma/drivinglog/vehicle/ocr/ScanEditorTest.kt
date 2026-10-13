package com.mikonoma.drivinglog.vehicle.ocr

import com.mikonoma.drivinglog.vehicle.domain.PendingCapture
import com.mikonoma.drivinglog.vehicle.picture.FakeImageCodec
import com.mikonoma.drivinglog.vehicle.picture.PhotoResult
import com.mikonoma.drivinglog.vehicle.picture.PictureError
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * What each way of leaving the chooser does to a scan, from the live scanner's photo action and from the review's "choose another photo"
 * (`ask-camera-for-camera-app`): only a refused camera shows the missing-permission message, and the scanner, the review and the
 * accepted scan stay as they were.
 */
class ScanEditorTest {

    private val store = FakeCaptureStore()
    private val editor = ScanEditor(FakeImageCodec(), FakeTextRecognizer(), store)
    private val classify: (RecognizedPhoto) -> List<Detection> = { emptyList() }

    private val accepted = PendingCapture("accepted", ScanResult(4, 3, emptyList(), 0))
    private val scanner = ScanDraft(scannerOpen = true, accepted = accepted)
    private val review = scanner.copy(review = ScanReview("reviewed", 4, 3, emptyList()))

    @Test
    fun aRefusedCameraOnTheScannerSetsTheCameraErrorAndKeepsTheScan() = runTest {
        assertEquals(scanner.copy(error = PictureError.CAMERA_DENIED), editor.photoPicked(scanner, PhotoResult.CameraDenied, classify))
    }

    @Test
    fun aRefusedCameraOnTheReviewKeepsTheReviewOpen() = runTest {
        val next = editor.photoPicked(review, PhotoResult.CameraDenied, classify)

        assertEquals(review.copy(error = PictureError.CAMERA_DENIED), next)
        assertEquals(emptyList(), store.discarded)
    }

    @Test
    fun leavingTheChooserChangesNothingAndShowsNoMessage() = runTest {
        assertEquals(scanner, editor.photoPicked(scanner, PhotoResult.Cancelled, classify))
        assertEquals(review, editor.photoPicked(review, PhotoResult.Cancelled, classify))
    }

    @Test
    fun anUnreadablePhotoSetsTheCouldNotOpenErrorNotTheCameraOne() = runTest {
        assertEquals(PictureError.COULD_NOT_OPEN, editor.photoPicked(scanner, PhotoResult.Unreadable, classify).error)
        assertEquals(review.copy(error = PictureError.COULD_NOT_OPEN), editor.photoPicked(review, PhotoResult.Unreadable, classify))
    }

    @Test
    fun dismissingClearsTheCameraError() {
        assertNull(editor.errorDismissed(review.copy(error = PictureError.CAMERA_DENIED)).error)
    }
}
