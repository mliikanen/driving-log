package com.mikonoma.drivinglog.vehicle.picture

import com.mikonoma.drivinglog.vehicle.data.FakeClock
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory

@OptIn(ExperimentalCoroutinesApi::class)
class FilePictureStoreTest {

    private val root = Path(SystemTemporaryDirectory, "driving-log-pictures-${counter++}-${kotlin.random.Random.nextLong()}")
    private val clock = FakeClock()
    private var ids = 0
    private val store = FilePictureStore(root, UnconfinedTestDispatcher(), clock) { "id${++ids}" }

    @AfterTest
    fun cleanUp() = deleteRecursively(root)

    private fun deleteRecursively(path: Path) {
        val fs = SystemFileSystem
        if (!fs.exists(path)) return
        if (fs.metadataOrNull(path)?.isDirectory == true) fs.list(path).forEach(::deleteRecursively)
        fs.delete(path, mustExist = false)
    }

    private fun image(vararg bytes: Int, extension: String = "webp", side: Int = 1) =
        EncodedImage(ByteArray(bytes.size) { bytes[it].toByte() }, extension, side, side)

    private val small = image(1, 2, 3)
    private val large = image(4, 5, 6, 7)

    private fun names(dir: Path): List<String> =
        if (SystemFileSystem.exists(dir)) SystemFileSystem.list(dir).map { it.name }.sorted() else emptyList()

    // ---- The photo waiting to be cropped

    @Test
    fun aPendingSourceIsKeptAndReadBack() = runTest {
        val pendingId = store.putPendingSource(byteArrayOf(9, 8, 7))

        assertContentEquals(byteArrayOf(9, 8, 7), store.readPendingSource(pendingId))
    }

    @Test
    fun aPendingSourceIsDiscarded() = runTest {
        val pendingId = store.putPendingSource(byteArrayOf(1))

        store.discardPendingSource(pendingId)

        assertNull(store.readPendingSource(pendingId))
    }

    @Test
    fun aMissingPendingSourceReadsAsNull() = runTest {
        assertNull(store.readPendingSource("nothing"))
    }

    @Test
    fun aPendingIdStartsWithTheTimeItWasMade() = runTest {
        val pendingId = store.putPendingSource(byteArrayOf(1))

        assertEquals(clock.current.toEpochMilliseconds().toString(), pendingId.substringBefore('-'))
    }

    // ---- A confirmed crop waiting for the vehicle to be saved

    @Test
    fun pendingVersionsAreKeptAndReadBackBySize() = runTest {
        val pendingId = store.putPendingSource(byteArrayOf(1))
        store.putPending(pendingId, small, large)

        assertContentEquals(small.bytes, store.readPending(pendingId, PictureSize.SMALL))
        assertContentEquals(large.bytes, store.readPending(pendingId, PictureSize.LARGE))
    }

    @Test
    fun puttingPendingVersionsAgainReplacesTheEarlierOnesEvenInAnotherFormat() = runTest {
        val pendingId = store.putPendingSource(byteArrayOf(1))
        store.putPending(pendingId, small, large)
        store.putPending(pendingId, image(10, extension = "png"), image(11, extension = "png"))

        assertContentEquals(byteArrayOf(10), store.readPending(pendingId, PictureSize.SMALL))
        assertEquals(
            listOf("$pendingId-large.png", "$pendingId-small.png", "$pendingId-source"),
            names(Path(root, "pending")),
        )
    }

    @Test
    fun discardingAPendingPictureDeletesTheSourceAndBothVersions() = runTest {
        val pendingId = store.putPendingSource(byteArrayOf(1))
        store.putPending(pendingId, small, large)

        store.discardPending(pendingId)

        assertEquals(emptyList(), names(Path(root, "pending")))
    }

    @Test
    fun discardingOnePendingPictureLeavesAnother() = runTest {
        val a = store.putPendingSource(byteArrayOf(1)).also { store.putPending(it, small, large) }
        val b = store.putPendingSource(byteArrayOf(2)).also { store.putPending(it, small, large) }

        store.discardPending(a)

        assertContentEquals(small.bytes, store.readPending(b, PictureSize.SMALL))
        assertNull(store.readPending(a, PictureSize.SMALL))
    }

    // ---- Promoting

    @Test
    fun promotingMovesBothVersionsUnderANewPictureId() = runTest {
        val pendingId = store.putPendingSource(byteArrayOf(1))
        store.putPending(pendingId, small, large)

        val pictureId = store.promote(pendingId)!!

        assertNotEquals(pendingId, pictureId)
        assertContentEquals(small.bytes, store.read(pictureId, PictureSize.SMALL))
        assertContentEquals(large.bytes, store.read(pictureId, PictureSize.LARGE))
        // The versions moved out of the pending area; only the source (if it was kept) could remain there.
        assertNull(store.readPending(pendingId, PictureSize.SMALL))
        assertNull(store.readPending(pendingId, PictureSize.LARGE))
    }

    @Test
    fun aPromotedPictureKeepsTheFormatItWasWrittenIn() = runTest {
        val pendingId = store.putPendingSource(byteArrayOf(1))
        store.putPending(pendingId, image(1, extension = "png"), image(2, extension = "png"))

        val pictureId = store.promote(pendingId)!!

        assertEquals(listOf("$pictureId-large.png", "$pictureId-small.png"), names(root).filter { it.startsWith(pictureId) })
    }

    @Test
    fun promotingWithoutVersionsMovesNothingAndReturnsNull() = runTest {
        val pendingId = store.putPendingSource(byteArrayOf(1)) // a photo that was never cropped

        assertNull(store.promote(pendingId))
        assertNull(store.promote("unknown"))
        assertEquals(emptyList(), names(root).filterNot { it == "pending" })
    }

    @Test
    fun promotingWithOnlyOneVersionMovesNothing() = runTest {
        val pendingId = store.putPendingSource(byteArrayOf(1))
        store.putPending(pendingId, small, large)
        SystemFileSystem.delete(Path(root, "pending", "$pendingId-large.webp"))

        assertNull(store.promote(pendingId))
        assertContentEquals(small.bytes, store.readPending(pendingId, PictureSize.SMALL))
    }

    // ---- Pictures in use

    @Test
    fun aMissingPictureReadsAsNull() = runTest {
        assertNull(store.read("nothing", PictureSize.SMALL))
        assertNull(store.read("nothing", PictureSize.LARGE))
    }

    @Test
    fun deletingAPictureRemovesBothVersionsAndOnlyThatPicture() = runTest {
        val a = store.putPendingSource(byteArrayOf(1)).also { store.putPending(it, small, large) }.let { store.promote(it)!! }
        val b = store.putPendingSource(byteArrayOf(1)).also { store.putPending(it, small, large) }.let { store.promote(it)!! }

        store.delete(a)

        assertNull(store.read(a, PictureSize.SMALL))
        assertNull(store.read(a, PictureSize.LARGE))
        assertContentEquals(small.bytes, store.read(b, PictureSize.SMALL))
    }

    @Test
    fun deletingAPictureThatIsNotThereIsNotAnError() = runTest {
        store.delete("nothing")
    }

    // ---- Sweeping

    private suspend fun picture(): String = store.putPendingSource(byteArrayOf(1)).also { store.putPending(it, small, large) }.let { store.promote(it)!! }

    @Test
    fun sweepDeletesPicturesNoVehicleRefersToAndKeepsTheOthers() = runTest {
        val used = picture()
        val unused = picture()

        store.sweep(setOf(used))

        assertContentEquals(small.bytes, store.read(used, PictureSize.SMALL))
        assertContentEquals(large.bytes, store.read(used, PictureSize.LARGE))
        assertNull(store.read(unused, PictureSize.SMALL))
        assertNull(store.read(unused, PictureSize.LARGE))
    }

    @Test
    fun sweepKeepsBothFormatsOfAReferencedPicture() = runTest {
        val pendingId = store.putPendingSource(byteArrayOf(1))
        store.putPending(pendingId, image(1, extension = "png"), image(2, extension = "png"))
        val pictureId = store.promote(pendingId)!!

        store.sweep(setOf(pictureId))

        assertContentEquals(byteArrayOf(1), store.read(pictureId, PictureSize.SMALL))
    }

    @Test
    fun sweepDeletesPendingFilesOlderThanADayAndKeepsYoungerOnes() = runTest {
        val old = store.putPendingSource(byteArrayOf(1)).also { store.putPending(it, small, large) }
        clock.current += 23.hours + 59.minutes
        val young = store.putPendingSource(byteArrayOf(2)).also { store.putPending(it, small, large) }
        clock.current += 2.minutes // the old one is now 24 h 1 min old, the young one 2 minutes

        store.sweep(emptySet())

        assertNull(store.readPending(old, PictureSize.SMALL))
        assertNull(store.readPendingSource(old))
        assertContentEquals(small.bytes, store.readPending(young, PictureSize.SMALL))
        assertContentEquals(byteArrayOf(2), store.readPendingSource(young))
    }

    @Test
    fun sweepKeepsAPendingPictureJustUnderADayOld() = runTest {
        val pendingId = store.putPendingSource(byteArrayOf(1))
        clock.current += 23.hours + 59.minutes

        store.sweep(emptySet())

        assertContentEquals(byteArrayOf(1), store.readPendingSource(pendingId))
    }

    @Test
    fun sweepDeletesPendingFilesWhoseAgeCannotBeTold() = runTest {
        SystemFileSystem.createDirectories(Path(root, "pending"))
        SystemFileSystem.sink(Path(root, "pending", "stray-source")).buffered().use { it.write(byteArrayOf(1)) }

        store.sweep(emptySet())

        assertEquals(emptyList(), names(Path(root, "pending")))
    }

    @Test
    fun sweepOnAnEmptyStorageDoesNothing() = runTest {
        store.sweep(emptySet())
    }

    // ---- URIs

    private fun decode(uri: String): String {
        assertTrue(uri.startsWith("file://"))
        val out = mutableListOf<Byte>()
        val rest = uri.removePrefix("file://")
        var i = 0
        while (i < rest.length) {
            if (rest[i] == '%') { out += rest.substring(i + 1, i + 3).toInt(16).toByte(); i += 3 } else { out += rest[i].code.toByte(); i++ }
        }
        return out.toByteArray().decodeToString()
    }

    @Test
    fun theUriOfAPictureInUseIsTheFileUriOfItsVersion() = runTest {
        val pictureId = picture()

        for (size in PictureSize.entries) {
            val uri = store.uri(pictureId, size)!!
            assertEquals(Path(root, "$pictureId-${size.name.lowercase()}.webp").toString(), decode(uri))
            assertTrue(uri.startsWith("file:///"))
        }
    }

    @Test
    fun theUriOfAPendingPictureIsTheFileUriOfItsVersion() = runTest {
        val pendingId = store.putPendingSource(byteArrayOf(1)).also { store.putPending(it, small, large) }

        assertEquals(Path(root, "pending", "$pendingId-small.webp").toString(), decode(store.pendingUri(pendingId, PictureSize.SMALL)!!))
        assertEquals(Path(root, "pending", "$pendingId-large.webp").toString(), decode(store.pendingUri(pendingId, PictureSize.LARGE)!!))
    }

    @Test
    fun theUriFollowsTheFormatTheVersionWasWrittenIn() = runTest {
        val pendingId = store.putPendingSource(byteArrayOf(1))
        store.putPending(pendingId, image(1, extension = "png"), image(2, extension = "png"))

        assertTrue(store.pendingUri(pendingId, PictureSize.SMALL)!!.endsWith("-small.png"))
        assertTrue(store.uri(store.promote(pendingId)!!, PictureSize.LARGE)!!.endsWith("-large.png"))
    }

    @Test
    fun aMissingFileHasNoUri() = runTest {
        val pictureId = picture()
        store.delete(pictureId)

        assertNull(store.uri(pictureId, PictureSize.SMALL))
        assertNull(store.uri("nothing", PictureSize.LARGE))
        assertNull(store.pendingUri("nothing", PictureSize.SMALL))
    }

    @Test
    fun aUriOfAPromotedPictureNoLongerExistsForThePendingId() = runTest {
        val pendingId = store.putPendingSource(byteArrayOf(1)).also { store.putPending(it, small, large) }
        store.promote(pendingId)

        assertNull(store.pendingUri(pendingId, PictureSize.SMALL))
    }

    @Test
    fun aRootWithSpacesAndUnsafeCharactersIsPercentEncoded() = runTest {
        val odd = Path(root, "Application Support", "we!rd #1 ä")
        val oddStore = FilePictureStore(odd, UnconfinedTestDispatcher(), clock) { "odd${++ids}" }
        val pendingId = oddStore.putPendingSource(byteArrayOf(1)).also { oddStore.putPending(it, small, large) }
        val pictureId = oddStore.promote(pendingId)!!

        val uri = oddStore.uri(pictureId, PictureSize.SMALL)!!

        assertTrue(' ' !in uri && '#' !in uri && '!' !in uri)
        assertTrue("Application%20Support" in uri)
        assertEquals(Path(odd, "$pictureId-small.webp").toString(), decode(uri))
    }

    @Test
    fun fileUriKeepsUnreservedCharactersAndEncodesTheRest() {
        assertEquals("file:///a/b-c_d.e~f/1", fileUri("/a/b-c_d.e~f/1"))
        assertEquals("file:///My%20Files/x%23y%25", fileUri("/My Files/x#y%"))
        assertEquals("file:///%C3%A4%E2%82%AC", fileUri("/ä€"))
    }

    @Test
    fun aRelativePathIsNotAFileUri() {
        assertFailsWith<IllegalArgumentException> { fileUri("relative/path") }
    }

    private companion object {
        var counter = 0
    }
}
