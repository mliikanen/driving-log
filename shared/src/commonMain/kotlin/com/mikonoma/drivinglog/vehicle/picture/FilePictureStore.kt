package com.mikonoma.drivinglog.vehicle.picture

import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

/**
 * Keeps the picture files under [root]: `{root}/{id}-small.{ext}` and `-large.{ext}` for pictures in use, and
 * `{root}/pending/{pendingId}-source`, `-small.{ext}` and `-large.{ext}` for the ones being worked on. A pending id starts
 * with the time it was made (`{epochMillis}-{uuid}`), because kotlinx-io does not tell a file's age and the sweep needs it.
 * [newId] makes the unique part of an id.
 */
class FilePictureStore(
    private val root: Path,
    private val dispatcher: CoroutineDispatcher,
    private val clock: Clock,
    private val newId: () -> String,
) : PictureStore {

    private val fs = SystemFileSystem
    private val pending = Path(root, PENDING_DIR)

    override suspend fun putPendingSource(bytes: ByteArray): String = withContext(dispatcher) {
        val pendingId = "${clock.now().toEpochMilliseconds()}-${newId()}"
        write(Path(pending, "$pendingId-$SOURCE"), bytes)
        pendingId
    }

    override suspend fun readPendingSource(pendingId: String): ByteArray? =
        withContext(dispatcher) { readOrNull(Path(pending, "$pendingId-$SOURCE")) }

    override suspend fun discardPendingSource(pendingId: String) =
        withContext(dispatcher) { fs.delete(Path(pending, "$pendingId-$SOURCE"), mustExist = false) }

    override suspend fun putPending(pendingId: String, small: EncodedImage, large: EncodedImage) = withContext(dispatcher) {
        deleteVersions(pending, pendingId)
        write(Path(pending, "$pendingId-small.${small.extension}"), small.bytes)
        write(Path(pending, "$pendingId-large.${large.extension}"), large.bytes)
    }

    override suspend fun readPending(pendingId: String, size: PictureSize): ByteArray? =
        withContext(dispatcher) { findVersion(pending, pendingId, size)?.let(::readOrNull) }

    override suspend fun discardPending(pendingId: String) = withContext(dispatcher) {
        deleteVersions(pending, pendingId)
        fs.delete(Path(pending, "$pendingId-$SOURCE"), mustExist = false)
    }

    override suspend fun promote(pendingId: String): String? = withContext(dispatcher) {
        val small = findVersion(pending, pendingId, PictureSize.SMALL)
        val large = findVersion(pending, pendingId, PictureSize.LARGE)
        if (small == null || large == null) return@withContext null
        val pictureId = newId()
        fs.createDirectories(root)
        fs.atomicMove(small, Path(root, "$pictureId-small.${small.name.substringAfterLast('.')}"))
        fs.atomicMove(large, Path(root, "$pictureId-large.${large.name.substringAfterLast('.')}"))
        pictureId
    }

    override suspend fun uri(pictureId: String, size: PictureSize): String? =
        withContext(dispatcher) { findVersion(root, pictureId, size)?.let { fileUri(it.toString()) } }

    override suspend fun pendingUri(pendingId: String, size: PictureSize): String? =
        withContext(dispatcher) { findVersion(pending, pendingId, size)?.let { fileUri(it.toString()) } }

    override suspend fun read(pictureId: String, size: PictureSize): ByteArray? =
        withContext(dispatcher) { findVersion(root, pictureId, size)?.let(::readOrNull) }

    override suspend fun delete(pictureId: String) = withContext(dispatcher) { deleteVersions(root, pictureId) }

    override suspend fun sweep(referencedIds: Set<String>) = withContext(dispatcher) {
        for (file in filesIn(root)) {
            if (idOf(file) !in referencedIds) fs.delete(file, mustExist = false)
        }
        val now = clock.now().toEpochMilliseconds()
        for (file in filesIn(pending)) {
            val madeAt = idOf(file).substringBefore('-').toLongOrNull()
            if (madeAt == null || now - madeAt > MAX_PENDING_AGE_MILLIS) fs.delete(file, mustExist = false)
        }
    }

    /** The id a file name starts with: everything before the last hyphen (`{id}-small.webp`, `{pendingId}-source`). */
    private fun idOf(file: Path): String = file.name.substringBeforeLast('-')

    private fun filesIn(dir: Path): List<Path> {
        if (!fs.exists(dir)) return emptyList()
        return fs.list(dir).filter { fs.metadataOrNull(it)?.isRegularFile == true }
    }

    private fun findVersion(dir: Path, id: String, size: PictureSize): Path? {
        val prefix = "$id-${size.name.lowercase()}."
        return KNOWN_EXTENSIONS.map { Path(dir, prefix + it) }.firstOrNull { fs.exists(it) }
    }

    private fun deleteVersions(dir: Path, id: String) {
        for (size in PictureSize.entries) for (extension in KNOWN_EXTENSIONS) {
            fs.delete(Path(dir, "$id-${size.name.lowercase()}.$extension"), mustExist = false)
        }
    }

    private fun write(path: Path, bytes: ByteArray) {
        path.parent?.let { fs.createDirectories(it) }
        fs.sink(path).buffered().use { it.write(bytes) }
    }

    private fun readOrNull(path: Path): ByteArray? =
        if (fs.exists(path)) fs.source(path).buffered().use { it.readByteArray() } else null

    private companion object {
        const val PENDING_DIR = "pending"
        const val SOURCE = "source"
        val KNOWN_EXTENSIONS = listOf("webp", "png")
        val MAX_PENDING_AGE_MILLIS = 24.hours.inWholeMilliseconds
    }
}
