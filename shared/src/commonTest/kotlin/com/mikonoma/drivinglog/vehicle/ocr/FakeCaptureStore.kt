package com.mikonoma.drivinglog.vehicle.ocr

import com.mikonoma.drivinglog.vehicle.picture.EncodedImage

/** An in-memory capture store for tests, with ways to look inside it and to make it fail. */
class FakeCaptureStore : CaptureStore {
    val pending = mutableMapOf<String, EncodedImage>()
    val photos = mutableMapOf<String, EncodedImage>()
    val discarded = mutableListOf<String>()
    val sweeps = mutableListOf<Set<String>>()

    private var counter = 0

    /** A scan photo waiting to be saved, as an accepted scan leaves it. Returns its pending id. */
    fun addPending(): String {
        val id = "capture-pending-${++counter}"
        pending[id] = EncodedImage(byteArrayOf(1, 2, 3), "webp", 4, 3)
        return id
    }

    override suspend fun putPending(photo: EncodedImage): String {
        val id = "capture-pending-${++counter}"
        pending[id] = photo
        return id
    }

    override suspend fun pendingUri(pendingId: String): String? = if (pendingId in pending) "file:///captures/pending/$pendingId.webp" else null

    override suspend fun discardPending(pendingId: String) {
        discarded += pendingId
        pending.remove(pendingId)
    }

    override suspend fun promote(pendingId: String): String? {
        val photo = pending.remove(pendingId) ?: return null
        val id = "capture-${++counter}"
        photos[id] = photo
        return id
    }

    override suspend fun delete(photoId: String) {
        photos.remove(photoId)
    }

    override suspend fun read(photoId: String): ByteArray? = photos[photoId]?.bytes

    override suspend fun sweep(referencedIds: Set<String>) {
        sweeps += referencedIds
        photos.keys.retainAll(referencedIds)
    }
}
