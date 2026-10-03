package com.mikonoma.drivinglog.vehicle.picture

/** The two stored versions of a picture: the small one for lists and pickers, the large one for full screen views. */
enum class PictureSize { SMALL, LARGE }

/** One encoded version of a picture: its bytes, the extension of its format (`webp`, `png`) and its size in pixels. */
class EncodedImage(val bytes: ByteArray, val extension: String, val width: Int, val height: Int)

/**
 * The picture files in the application's private storage. A picture in use is named by a picture id, which is what the owning
 * record (a vehicle, or an event, `add-event-pictures`) stores. A picture the user is still working on (a photo waiting to be
 * cropped, or one already encoded but waiting for its owning record to be saved) is *pending*, named by a pending id, and is
 * moved under a new picture id by [promote] when the owning record is saved.
 */
// One function per thing done with a picture's files (stage, promote, read, delete, sweep).
@Suppress("TooManyFunctions")
interface PictureStore {
    /**
     * Keeps the bytes of a chosen photo and mints a new pending id for it. A caller that crops reads it back with
     * [readPendingSource] once the user confirms a crop; a caller that does not crop (`add-event-pictures`) encodes
     * straight from the bytes it already has and only uses the returned id, discarding the source immediately with
     * [discardPendingSource].
     */
    suspend fun putPendingSource(bytes: ByteArray): String

    suspend fun readPendingSource(pendingId: String): ByteArray?

    suspend fun discardPendingSource(pendingId: String)

    /** Keeps the two versions of a confirmed crop under [pendingId], replacing any earlier ones. */
    suspend fun putPending(pendingId: String, small: EncodedImage, large: EncodedImage)

    suspend fun readPending(pendingId: String, size: PictureSize): ByteArray?

    /** Deletes every file of a pending picture: the source and the versions. */
    suspend fun discardPending(pendingId: String)

    /**
     * Moves the two versions of a pending picture into the pictures in use, under a new picture id which it returns. Null, and
     * nothing moved, when the pending versions are missing (a photo that was not cropped, or files that are gone).
     */
    suspend fun promote(pendingId: String): String?

    /**
     * Where a picture in use can be loaded from: a `file://` URI of the version's file, or null when the file is missing. This is
     * what the view states carry, so that a screen only has to load a URI (which may one day be an `https://` one).
     */
    suspend fun uri(pictureId: String, size: PictureSize): String?

    /** The same for a pending picture (the preview on the add and edit screens). */
    suspend fun pendingUri(pendingId: String, size: PictureSize): String?

    /** The bytes of a picture in use, or null when the file is missing. */
    suspend fun read(pictureId: String, size: PictureSize): ByteArray?

    /** Deletes both versions of a picture in use. Nothing to delete is not an error. */
    suspend fun delete(pictureId: String)

    /**
     * Deletes the pictures in use that are not in [referencedIds], and the pending files that are older than a day or whose
     * age cannot be told. Younger pending files stay: a form restored after the process died may still use them.
     */
    suspend fun sweep(referencedIds: Set<String>)
}
