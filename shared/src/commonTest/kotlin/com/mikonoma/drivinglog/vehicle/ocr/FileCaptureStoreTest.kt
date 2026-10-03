package com.mikonoma.drivinglog.vehicle.ocr

import com.mikonoma.drivinglog.vehicle.data.FakeClock
import com.mikonoma.drivinglog.vehicle.picture.EncodedImage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalCoroutinesApi::class)
class FileCaptureStoreTest {

    private val root = Path(SystemTemporaryDirectory, "driving-log-captures-${kotlin.random.Random.nextLong()}")
    private val clock = FakeClock()
    private var ids = 0
    private val store = FileCaptureStore(root, UnconfinedTestDispatcher(), clock) { "id${++ids}" }

    private val photo = EncodedImage(byteArrayOf(9, 8, 7), "webp", 3, 2)

    @AfterTest
    fun cleanUp() = deleteRecursively(root)

    private fun deleteRecursively(path: Path) {
        val fs = SystemFileSystem
        if (!fs.exists(path)) return
        if (fs.metadataOrNull(path)?.isDirectory == true) fs.list(path).forEach(::deleteRecursively)
        fs.delete(path, mustExist = false)
    }

    private fun names(dir: Path): List<String> = if (SystemFileSystem.exists(dir)) SystemFileSystem.list(dir).map { it.name }.sorted() else emptyList()

    @Test
    fun aPendingScanHasAFileUri() = runTest {
        val pendingId = store.putPending(photo)

        val uri = assertNotNull(store.pendingUri(pendingId))
        assertTrue(uri.startsWith("file://") && uri.endsWith("/pending/$pendingId.webp"), uri)
    }

    @Test
    fun promotingMovesThePhotoUnderANewId() = runTest {
        val pendingId = store.putPending(photo)

        val photoId = assertNotNull(store.promote(pendingId))

        assertContentEquals(photo.bytes, store.read(photoId))
        assertNull(store.pendingUri(pendingId))
        assertEquals(emptyList(), names(Path(root, "pending")))
    }

    @Test
    fun promotingAMissingScanMovesNothing() = runTest {
        assertNull(store.promote("0-gone"))
    }

    @Test
    fun discardingDeletesThePendingPhoto() = runTest {
        val pendingId = store.putPending(photo)

        store.discardPending(pendingId)

        assertEquals(emptyList(), names(Path(root, "pending")))
    }

    @Test
    fun theSweepKeepsReferencedPhotosAndYoungPendingOnes() = runTest {
        val kept = store.promote(store.putPending(photo))!!
        val stray = store.promote(store.putPending(photo))!!
        val oldPending = store.putPending(photo)
        clock.current += 25.hours
        val youngPending = store.putPending(photo)

        store.sweep(setOf(kept))

        assertNotNull(store.read(kept))
        assertNull(store.read(stray))
        assertNull(store.pendingUri(oldPending))
        assertNotNull(store.pendingUri(youngPending))
    }
}
