package com.mikonoma.drivinglog.vehicle.picture

/** A photo larger than this is refused as unreadable, which keeps the bytes held in memory bounded. */
const val MAX_PHOTO_BYTES = 40L * 1024 * 1024

/**
 * The picture logic shared by the add and edit screens: choosing a photo, cropping it, removing the picture and cleaning up. It
 * works on a [PictureEditState] and returns the next one, keeping the files in the [store] in step, so the processors only call it
 * and never re-implement a rule. [removedDraft] is the draft "Remove picture" leads to: [PictureDraft.None] when adding,
 * [PictureDraft.Removed] when editing.
 */
class PictureDraftEditor(
    private val store: VehiclePictureStore,
    private val codec: ImageCodec,
    private val removedDraft: PictureDraft,
) {
    /**
     * The system's chooser gave [result]. A photo is kept and the crop opens; leaving without one changes nothing; a photo that cannot
     * be opened (not an image, or too large) and a refused camera change nothing but set their error. Choosing again while a crop is
     * open replaces the earlier photo.
     */
    suspend fun photoPicked(state: PictureEditState, result: PhotoResult): PictureEditState = when (result) {
        PhotoResult.Cancelled -> state
        PhotoResult.Unreadable -> state.copy(error = PictureError.COULD_NOT_OPEN)
        PhotoResult.CameraDenied -> state.copy(error = PictureError.CAMERA_DENIED)
        is PhotoResult.Chosen -> {
            val bytes = result.bytes
            if (bytes.size > MAX_PHOTO_BYTES || codec.decode(bytes) == null) {
                state.copy(error = PictureError.COULD_NOT_OPEN)
            } else {
                state.cropSourceId?.let { store.discardPendingSource(it) }
                state.copy(cropSourceId = store.putPendingSource(bytes), error = null)
            }
        }
    }

    /** The photo being cropped, decoded for the crop screen, or null when there is none or its file is gone. */
    suspend fun cropImage(state: PictureEditState): DecodedImage? {
        val source = state.cropSourceId?.let { store.readPendingSource(it) } ?: return null
        return codec.decode(source)
    }

    /**
     * The crop was confirmed: encode the two versions of [crop] into the pending area under the photo's pending id, drop the
     * photo and any earlier pending picture, and make the pending picture the draft. When the photo is gone or cannot be
     * encoded the crop closes with the error and the draft is as it was.
     */
    suspend fun cropConfirmed(state: PictureEditState, crop: CropRect): PictureEditState {
        val pendingId = state.cropSourceId ?: return state
        val source = store.readPendingSource(pendingId)
        val encoded = source?.let { codec.encodeSquare(it, crop, pictureSides(crop.side)) }
        if (encoded == null) {
            store.discardPending(pendingId)
            return state.copy(cropSourceId = null, error = PictureError.COULD_NOT_OPEN)
        }
        store.putPending(pendingId, encoded.small, encoded.large)
        store.discardPendingSource(pendingId)
        (state.draft as? PictureDraft.Pending)?.let { store.discardPending(it.pendingId) }
        return state.copy(draft = PictureDraft.Pending(pendingId), cropSourceId = null, error = null)
    }

    /** The crop was cancelled: the chosen photo is dropped and the draft is as it was. */
    suspend fun cropCancelled(state: PictureEditState): PictureEditState {
        state.cropSourceId?.let { store.discardPendingSource(it) }
        return state.copy(cropSourceId = null)
    }

    /** "Remove picture": a pending picture's files are deleted and the draft is [removedDraft]. */
    suspend fun removed(state: PictureEditState): PictureEditState {
        (state.draft as? PictureDraft.Pending)?.let { store.discardPending(it.pendingId) }
        return state.copy(draft = removedDraft, error = null)
    }

    fun errorDismissed(state: PictureEditState): PictureEditState = state.copy(error = null)

    /** The user left the screen without saving: nothing pending is kept. */
    suspend fun discardAll(state: PictureEditState) {
        state.cropSourceId?.let { store.discardPending(it) }
        (state.draft as? PictureDraft.Pending)?.let { store.discardPending(it.pendingId) }
    }

    /** The URI of the picture the form shows as its preview (the small version), or null for none. */
    suspend fun previewUri(state: PictureEditState, savedPictureId: String?): String? = when (val draft = state.draft) {
        is PictureDraft.Pending -> store.pendingUri(draft.pendingId, PictureSize.SMALL)
        PictureDraft.Unchanged -> savedPictureId?.let { store.uri(it, PictureSize.SMALL) }
        PictureDraft.None, PictureDraft.Removed -> null
    }
}
