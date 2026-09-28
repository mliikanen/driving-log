package com.mikonoma.drivinglog.vehicle.ocr

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What one scan found (`odometer-ocr-capture`), as it is stored with the event it filled in: the size of the photo the boxes are in,
 * every number detected (not only the one accepted) and which one the user accepted. Kept only to review a misdetection later.
 */
@Serializable
data class ScanResult(
    val width: Int,
    val height: Int,
    val detections: List<Detection>,
    /** The index in [detections] of the reading the user accepted. */
    val acceptedIndex: Int,
) {
    val accepted: Detection get() = detections[acceptedIndex]

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        /** Unknown fields are ignored, so a result stored by a later version (a new optional field) still reads. */
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String): ScanResult = json.decodeFromString(serializer(), text)
    }
}
