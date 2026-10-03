package com.mikonoma.drivinglog.vehicle.ocr

import com.mikonoma.drivinglog.vehicle.domain.PendingCapture
import com.mikonoma.drivinglog.vehicle.picture.ImageCodec
import com.mikonoma.drivinglog.vehicle.picture.MAX_DECODE_SIDE
import com.mikonoma.drivinglog.vehicle.picture.MAX_PHOTO_BYTES
import com.mikonoma.drivinglog.vehicle.picture.PhotoResult
import com.mikonoma.drivinglog.vehicle.picture.PictureError
import kotlinx.serialization.Serializable

/**
 * The review screen of one scan (`odometer-ocr-capture`): the photo waiting in the capture store's pending area, its size in the pixels
 * the boxes are in, every number detected, and the candidate the user has tapped, if any. Candidates are the detections with a kind;
 * [selectedIndex] is an index into [detections] and only ever points at one.
 */
@Serializable
data class ScanReview(val pendingId: String, val width: Int, val height: Int, val detections: List<Detection>, val selectedIndex: Int? = null) {
    val hasCandidates: Boolean get() = detections.any { it.kind != null }
    val selected: Detection? get() = selectedIndex?.let { detections[it] }
}

/**
 * The log event form's scan (`odometer-ocr-capture`): the review screen while it is open, and the scan accepted last, which is saved
 * with the entry. Named ids and detections only, no pixels, so it survives rotation and process death like [com.mikonoma.drivinglog.vehicle.picture.EventPhotoDraft].
 */
@Serializable
data class ScanDraft(
    val review: ScanReview? = null,
    val accepted: PendingCapture? = null,
    val error: PictureError? = null,
    /** True while the live scanner is open (`add-live-scanner`); the photo review opens on top of it and returns to it. */
    val scannerOpen: Boolean = false,
)

/**
 * The scan logic of the log event form: choosing a photo, recognizing it, selecting and confirming a candidate, and cleaning up. It
 * works on a [ScanDraft] and returns the next one, keeping the [store]'s files in step, so the processor only calls it.
 */
class ScanEditor(private val codec: ImageCodec, private val recognizer: TextRecognizer, private val store: CaptureStore) {
    /**
     * The system's chooser gave [result]. A photo is encoded once, full size and uncropped, kept pending, recognized and classified by
     * [classify] (the form's own rule, as for [LiveScanner]); the review opens on it, replacing a review already open (choosing another
     * photo from "no reading found"). Leaving the chooser changes nothing; a photo that cannot be opened or a refused camera set the error.
     */
    suspend fun photoPicked(draft: ScanDraft, result: PhotoResult, classify: (RecognizedPhoto) -> List<Detection>): ScanDraft = when (result) {
        PhotoResult.Cancelled -> draft

        PhotoResult.Unreadable -> draft.copy(error = PictureError.COULD_NOT_OPEN)

        PhotoResult.CameraDenied -> draft.copy(error = PictureError.CAMERA_DENIED)

        is PhotoResult.Chosen -> {
            val bytes = result.bytes
            val photo = if (bytes.size > MAX_PHOTO_BYTES) null else codec.encodeScaled(bytes, listOf(MAX_DECODE_SIDE))?.singleOrNull()
            val recognized = photo?.let { recognizer.recognize(bytes) }
            if (photo == null || recognized == null) {
                draft.copy(error = PictureError.COULD_NOT_OPEN)
            } else {
                draft.review?.let { store.discardPending(it.pendingId) }
                val pendingId = store.putPending(photo)
                val review = ScanReview(pendingId, recognized.width, recognized.height, classify(recognized))
                draft.copy(review = review, error = null)
            }
        }
    }

    /** A candidate was tapped: it becomes the selection, in place of any other. A detection that is not a candidate is ignored. */
    fun selected(draft: ScanDraft, index: Int): ScanDraft {
        val review = draft.review ?: return draft
        if (review.detections.getOrNull(index)?.kind == null) return draft
        return draft.copy(review = review.copy(selectedIndex = index))
    }

    /**
     * The selection was confirmed: the review closes and becomes the accepted scan, replacing (and deleting) one accepted before.
     * Returns the next draft and the accepted detection, or null for both when nothing is selected.
     */
    suspend fun confirmed(draft: ScanDraft): Pair<ScanDraft, Detection>? {
        val review = draft.review ?: return null
        val index = review.selectedIndex ?: return null
        draft.accepted?.let { store.discardPending(it.pendingId) }
        val accepted = PendingCapture(review.pendingId, ScanResult(review.width, review.height, review.detections, index))
        return draft.copy(review = null, accepted = accepted, scannerOpen = false) to review.detections[index]
    }

    /**
     * A reading was tapped in the live scanner (`add-live-scanner`): the frame it was read in is encoded once and kept as the pending
     * scan, with that frame's detections and the tapped one accepted, replacing (and deleting) one accepted before; the scanner closes.
     * Null, and nothing changed, when the frame cannot be encoded or the reading is not among its frame's detections.
     */
    suspend fun liveAccepted(draft: ScanDraft, reading: LiveReading): ScanDraft? {
        val index = reading.frame.detections.indexOf(reading.detection)
        if (index < 0) return null
        val photo = codec.encode(reading.frame.image) ?: return null
        val pendingId = store.putPending(photo)
        draft.accepted?.let { store.discardPending(it.pendingId) }
        val result = ScanResult(reading.frame.image.width, reading.frame.image.height, reading.frame.detections, index)
        return draft.copy(accepted = PendingCapture(pendingId, result), scannerOpen = false, error = null)
    }

    fun scannerOpened(draft: ScanDraft): ScanDraft = draft.copy(scannerOpen = true, error = null)

    /** Leaving the live scanner without a reading: nothing it showed is kept. */
    suspend fun scannerClosed(draft: ScanDraft): ScanDraft = cancelled(draft).copy(scannerOpen = false)

    /** Back out of the review, or "Leave" on "no reading found": the scanned photo is deleted and the form is as it was. */
    suspend fun cancelled(draft: ScanDraft): ScanDraft {
        draft.review?.let { store.discardPending(it.pendingId) }
        return draft.copy(review = null)
    }

    fun errorDismissed(draft: ScanDraft): ScanDraft = draft.copy(error = null)

    /** The user left the form without saving: neither the open review's photo nor the accepted one is kept. */
    suspend fun discardAll(draft: ScanDraft) {
        draft.review?.let { store.discardPending(it.pendingId) }
        draft.accepted?.let { store.discardPending(it.pendingId) }
    }

    /** Where the review screen loads the photo from, or null when no review is open or its file is gone. */
    suspend fun reviewPhotoUri(draft: ScanDraft): String? = draft.review?.let { store.pendingUri(it.pendingId) }
}
