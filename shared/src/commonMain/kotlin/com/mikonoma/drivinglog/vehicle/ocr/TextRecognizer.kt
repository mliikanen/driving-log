package com.mikonoma.drivinglog.vehicle.ocr

import com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage
import kotlinx.serialization.Serializable

/** A rectangle in the pixels of a [RecognizedPhoto]: [left] and [top] inclusive, [right] and [bottom] exclusive. */
@Serializable
data class TextBox(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
}

/** One word the recognizer read ("ODO", "71140", "km"), with where it is in the photo. */
data class RecognizedElement(val text: String, val box: TextBox)

/** One line of text the recognizer read, as its words in reading order. */
data class RecognizedLine(val text: String, val box: TextBox, val elements: List<RecognizedElement>)

/**
 * What the recognizer read in a photo. The boxes are in the pixels of the photo as [width] x [height], which is the photo
 * with its orientation applied and decoded no larger than `MAX_DECODE_SIDE` on its longer side (the same way `ImageCodec`
 * decodes), so a screen that shows the photo can scale the boxes onto it.
 */
data class RecognizedPhoto(val width: Int, val height: Int, val lines: List<RecognizedLine>)

/**
 * Reading text in a photo, on the device and without a network connection (`odometer-ocr-capture`), which only the
 * platform can do. [isAvailable] is false on a platform with no recognizer yet (iOS), where nothing offers a scan.
 */
interface TextRecognizer {
    val isAvailable: Boolean

    /** The text in the photo, or null when the bytes are not an image the platform can decode. */
    suspend fun recognize(bytes: ByteArray): RecognizedPhoto?

    /**
     * The text in an already decoded, upright image: a camera frame of the live scanner (`add-live-scanner`). The boxes are in
     * [frame]'s pixels.
     */
    suspend fun recognize(frame: RgbImage): RecognizedPhoto?
}

/** The recognizer of a platform that has none yet: nothing offers a scan, and a scan would read nothing. */
object UnavailableTextRecognizer : TextRecognizer {
    override val isAvailable: Boolean = false
    override suspend fun recognize(bytes: ByteArray): RecognizedPhoto? = null
    override suspend fun recognize(frame: RgbImage): RecognizedPhoto? = null
}

/** A photo is recognized with its longer side scaled up to this when it is smaller (design.md). */
const val RECOGNITION_SIDE = 2560

/** How much a [width] x [height] photo is scaled for recognition: up to [RECOGNITION_SIDE] on its longer side, at most twice, never down. */
fun recognitionScale(width: Int, height: Int): Double {
    val longer = maxOf(width, height)
    if (longer <= 0 || longer >= RECOGNITION_SIDE) return 1.0
    return minOf(2.0, RECOGNITION_SIDE.toDouble() / longer)
}
