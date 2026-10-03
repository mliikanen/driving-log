package com.mikonoma.drivinglog.vehicle.ocr

import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.picture.EncodedImage
import com.mikonoma.drivinglog.vehicle.picture.fileUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/**
 * The photos of scans (`odometer-ocr-capture`), in the application's private storage. Unlike a vehicle's or an event's picture, a scan
 * photo is one full-size version, never cropped and never shown in a list: the review screen shows it while the user picks a reading,
 * and afterwards it is kept only to review a misdetection. It follows the same pending/promote shape as the picture stores: a scan is
 * *pending* while the log event form is open, and is moved under a photo id by [promote] when the form is saved.
 */
interface CaptureStore {
    /** Keeps [photo] as a new pending scan and returns its pending id. */
    suspend fun putPending(photo: EncodedImage): String

    /** Where a pending scan's photo can be loaded from (the review screen), or null when its file is gone. */
    suspend fun pendingUri(pendingId: String): String?

    suspend fun discardPending(pendingId: String)

    /** Moves a pending scan's photo into use under a new photo id, which it returns; null, and nothing moved, when the file is gone. */
    suspend fun promote(pendingId: String): String?

    /** Deletes a photo in use. Nothing to delete is not an error. */
    suspend fun delete(photoId: String)

    /** The bytes of a photo in use, or null when the file is missing. */
    suspend fun read(photoId: String): ByteArray?

    /** Deletes the photos in use that are not in [referencedIds], and pending ones older than a day or whose age cannot be told. */
    suspend fun sweep(referencedIds: Set<String>)
}

/**
 * Keeps scan photos under [root]: `{root}/{photoId}.{ext}` in use and `{root}/pending/{pendingId}.{ext}` pending. A pending id starts
 * with the time it was made (`{epochMillis}-{uuid}`), as in `FilePictureStore`, because kotlinx-io does not tell a file's age.
 */
class FileCaptureStore(private val root: Path, private val dispatcher: CoroutineDispatcher, private val clock: Clock, private val newId: () -> String) :
    CaptureStore {

    private val fs = SystemFileSystem
    private val pending = Path(root, PENDING_DIR)

    override suspend fun putPending(photo: EncodedImage): String = withContext(dispatcher) {
        val pendingId = "${clock.now().toEpochMilliseconds()}-${newId()}"
        fs.createDirectories(pending)
        fs.sink(Path(pending, "$pendingId.${photo.extension}")).buffered().use { it.write(photo.bytes) }
        pendingId
    }

    override suspend fun pendingUri(pendingId: String): String? = withContext(dispatcher) { find(pending, pendingId)?.let { fileUri(it.toString()) } }

    override suspend fun discardPending(pendingId: String) = withContext(dispatcher) { deleteAll(pending, pendingId) }

    override suspend fun promote(pendingId: String): String? = withContext(dispatcher) {
        val file = find(pending, pendingId) ?: return@withContext null
        val photoId = newId()
        fs.createDirectories(root)
        fs.atomicMove(file, Path(root, "$photoId.${file.name.substringAfterLast('.')}"))
        photoId
    }

    override suspend fun delete(photoId: String) = withContext(dispatcher) { deleteAll(root, photoId) }

    override suspend fun read(photoId: String): ByteArray? =
        withContext(dispatcher) { find(root, photoId)?.let { path -> fs.source(path).buffered().use { it.readByteArray() } } }

    override suspend fun sweep(referencedIds: Set<String>) = withContext(dispatcher) {
        for (file in filesIn(root)) {
            if (file.name.substringBeforeLast('.') !in referencedIds) fs.delete(file, mustExist = false)
        }
        val now = clock.now().toEpochMilliseconds()
        for (file in filesIn(pending)) {
            val madeAt = file.name.substringBefore('-').toLongOrNull()
            if (madeAt == null || now - madeAt > MAX_PENDING_AGE_MILLIS) fs.delete(file, mustExist = false)
        }
    }

    private fun filesIn(dir: Path): List<Path> {
        if (!fs.exists(dir)) return emptyList()
        return fs.list(dir).filter { fs.metadataOrNull(it)?.isRegularFile == true }
    }

    private fun find(dir: Path, id: String): Path? = KNOWN_EXTENSIONS.map { Path(dir, "$id.$it") }.firstOrNull { fs.exists(it) }

    private fun deleteAll(dir: Path, id: String) {
        for (extension in KNOWN_EXTENSIONS) fs.delete(Path(dir, "$id.$extension"), mustExist = false)
    }

    private companion object {
        const val PENDING_DIR = "pending"
        val KNOWN_EXTENSIONS = listOf("webp", "png")
        val MAX_PENDING_AGE_MILLIS = 24.hours.inWholeMilliseconds
    }
}

/**
 * Deletes the scan photos no event refers to (left by an interrupted save) and stale pending ones (a form left without saving, or a
 * process that died with one open). Run once when the app starts, like `sweepPictures`.
 */
suspend fun sweepCaptures(vehicles: VehicleRepository, captures: CaptureStore) {
    captures.sweep(vehicles.capturePhotoIds())
}
