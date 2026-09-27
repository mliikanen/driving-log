package com.mikonoma.drivinglog.vehicle.picture

import com.mikonoma.drivinglog.vehicle.domain.PendingPicture
import kotlinx.serialization.Serializable

/**
 * Up to [MAX_PHOTOS] pending photo ids attached while composing or editing a "Distance"/"Odometer reading" event
 * (`add-event-pictures`), plus what confirmation or error is showing. Named ids only, like `PictureDraft` — no
 * pixels held in state, so it survives rotation and process death.
 */
@Serializable
data class EventPhotoDraft(
    val pendingIds: List<String> = emptyList(),
    /** The pending id whose "Remove this photo?" confirmation is shown, or null when none is. */
    val removalPendingId: String? = null,
    val error: PictureError? = null,
) {
    val isFull: Boolean get() = pendingIds.size >= MAX_PHOTOS

    companion object {
        const val MAX_PHOTOS = 5
    }
}

/**
 * The photo-strip logic shared by the log event form and, later, the details screen's "Edit" action
 * (`add-event-pictures`): choosing a photo (no crop — an event photo is looked at for its content, see design.md),
 * removing one (with confirmation) and cleaning up. Works on an [EventPhotoDraft] and returns the next one, keeping
 * [store]'s files in step, so a caller only calls it and never re-implements a rule.
 */
class EventPhotoDraftEditor(
    private val store: PictureStore,
    private val codec: ImageCodec,
) {
    /**
     * The system's chooser gave [result]. Already at the [EventPhotoDraft.MAX_PHOTOS] cap, a photo that cannot be
     * opened (not an image, or too large) or a refused camera leave [state] as it is except for [EventPhotoDraft.error];
     * otherwise the photo is decoded, scaled to the small and large caps (no crop) and appended to the strip.
     */
    suspend fun photoPicked(state: EventPhotoDraft, result: PhotoResult): EventPhotoDraft = when (result) {
        PhotoResult.Cancelled -> state
        PhotoResult.Unreadable -> state.copy(error = PictureError.COULD_NOT_OPEN)
        PhotoResult.CameraDenied -> state.copy(error = PictureError.CAMERA_DENIED)
        is PhotoResult.Chosen -> {
            if (state.isFull) {
                state
            } else {
                val bytes = result.bytes
                val encoded = if (bytes.size > MAX_PHOTO_BYTES) null else codec.encodeScaled(bytes, listOf(SMALL_SIDE, EVENT_PHOTO_LARGE_SIDE))
                if (encoded == null || encoded.size != 2) {
                    state.copy(error = PictureError.COULD_NOT_OPEN)
                } else {
                    val pendingId = store.putPendingSource(bytes)
                    store.putPending(pendingId, encoded[0], encoded[1])
                    store.discardPendingSource(pendingId)
                    state.copy(pendingIds = state.pendingIds + pendingId, error = null)
                }
            }
        }
    }

    /** The remove action on one thumbnail: asks for confirmation before removing [pendingId]. */
    fun removeRequested(state: EventPhotoDraft, pendingId: String): EventPhotoDraft = state.copy(removalPendingId = pendingId)

    /** Confirmed: the photo's files are deleted and it leaves the strip. */
    suspend fun removeConfirmed(state: EventPhotoDraft): EventPhotoDraft {
        val pendingId = state.removalPendingId ?: return state
        store.discardPending(pendingId)
        return state.copy(pendingIds = state.pendingIds - pendingId, removalPendingId = null)
    }

    /** Cancelled or dismissed: nothing is removed. */
    fun removeCancelled(state: EventPhotoDraft): EventPhotoDraft = state.copy(removalPendingId = null)

    fun errorDismissed(state: EventPhotoDraft): EventPhotoDraft = state.copy(error = null)

    /** The user left without saving: every attached photo's pending files are discarded. */
    suspend fun discardAll(state: EventPhotoDraft) {
        for (pendingId in state.pendingIds) store.discardPending(pendingId)
    }

    /** The strip's thumbnail URIs (the small version), paired with each photo's pending id, in attach order. A
     * photo whose files are gone is skipped. */
    suspend fun previewUris(state: EventPhotoDraft): List<Pair<String, String>> =
        state.pendingIds.mapNotNull { id -> store.pendingUri(id, PictureSize.SMALL)?.let { id to it } }

    /** What saving commits: the attached photos, in attach order. */
    fun toPending(state: EventPhotoDraft): List<PendingPicture> = state.pendingIds.map { PendingPicture(it) }
}
