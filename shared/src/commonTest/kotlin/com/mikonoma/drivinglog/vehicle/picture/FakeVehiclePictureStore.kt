package com.mikonoma.drivinglog.vehicle.picture

/** An in-memory picture store for tests, with a few ways to look inside it and to make it fail. */
class FakeVehiclePictureStore : VehiclePictureStore {

    class Versions(val small: EncodedImage, val large: EncodedImage)

    val sources = mutableMapOf<String, ByteArray>()
    val pending = mutableMapOf<String, Versions>()
    val pictures = mutableMapOf<String, Versions>()
    val sweeps = mutableListOf<Set<String>>()

    /** When set, [promote] throws it (a disk that fails). */
    var promoteFailure: Throwable? = null

    private var counter = 0

    /** A confirmed crop waiting to be saved, as the crop screen leaves it. Returns its pending id. */
    fun addPending(small: EncodedImage = image(1), large: EncodedImage = image(2)): String {
        val id = "pending-${++counter}"
        pending[id] = Versions(small, large)
        return id
    }

    /** A picture in use, as a saved vehicle refers to it. Returns its picture id. */
    fun addPicture(small: EncodedImage = image(1), large: EncodedImage = image(2)): String {
        val id = "picture-${++counter}"
        pictures[id] = Versions(small, large)
        return id
    }

    /** The ids of every file group the store holds, to assert that nothing is left behind. */
    fun everything(): Set<String> = sources.keys + pending.keys + pictures.keys

    override suspend fun putPendingSource(bytes: ByteArray): String {
        val id = "pending-${++counter}"
        sources[id] = bytes
        return id
    }

    override suspend fun readPendingSource(pendingId: String): ByteArray? = sources[pendingId]

    override suspend fun discardPendingSource(pendingId: String) {
        sources.remove(pendingId)
    }

    override suspend fun putPending(pendingId: String, small: EncodedImage, large: EncodedImage) {
        pending[pendingId] = Versions(small, large)
    }

    override suspend fun readPending(pendingId: String, size: PictureSize): ByteArray? =
        pending[pendingId]?.let { if (size == PictureSize.SMALL) it.small.bytes else it.large.bytes }

    override suspend fun discardPending(pendingId: String) {
        pending.remove(pendingId)
        sources.remove(pendingId)
    }

    override suspend fun promote(pendingId: String): String? {
        promoteFailure?.let { throw it }
        val versions = pending.remove(pendingId) ?: return null
        val id = "picture-${++counter}"
        pictures[id] = versions
        return id
    }

    /** A fake URI naming the picture and size, so tests can tell them apart; null when the picture is not there. */
    override suspend fun uri(pictureId: String, size: PictureSize): String? =
        if (pictureId in pictures) fakeUri("pictures", pictureId, size) else null

    override suspend fun pendingUri(pendingId: String, size: PictureSize): String? =
        if (pendingId in pending) fakeUri("pending", pendingId, size) else null

    override suspend fun read(pictureId: String, size: PictureSize): ByteArray? =
        pictures[pictureId]?.let { if (size == PictureSize.SMALL) it.small.bytes else it.large.bytes }

    override suspend fun delete(pictureId: String) {
        pictures.remove(pictureId)
    }

    override suspend fun sweep(referencedIds: Set<String>) {
        sweeps += referencedIds
        pictures.keys.retainAll(referencedIds)
    }

    companion object {
        fun fakeUri(area: String, id: String, size: PictureSize) = "file:///fake/$area/$id-${size.name.lowercase()}.webp"

        fun image(vararg bytes: Int, extension: String = "webp") =
            EncodedImage(ByteArray(bytes.size) { bytes[it].toByte() }, extension, 1, 1)
    }
}
