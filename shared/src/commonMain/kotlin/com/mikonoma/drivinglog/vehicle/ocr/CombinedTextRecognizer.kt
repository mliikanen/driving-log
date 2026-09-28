package com.mikonoma.drivinglog.vehicle.ocr

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Several recognizers over the same photo, as one (`add-seven-segment-ocr`): ML Kit for printed text and PP-OCR for seven-segment
 * digits run side by side, and the lines of both are returned. A recognizer that fails or cannot read the photo only leaves its own
 * lines out. The boxes are in the photo's pixels as the first recognizer that read it decoded it; another's are scaled to match.
 * Candidate detection keeps one candidate where two recognizers read the same place.
 */
class CombinedTextRecognizer(private val recognizers: List<TextRecognizer>) : TextRecognizer {

    constructor(vararg recognizers: TextRecognizer) : this(recognizers.toList())

    override val isAvailable: Boolean get() = recognizers.any { it.isAvailable }

    override suspend fun recognize(bytes: ByteArray): RecognizedPhoto? {
        val photos = coroutineScope {
            recognizers.filter { it.isAvailable }.map { recognizer ->
                async {
                    try {
                        recognizer.recognize(bytes)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        null
                    }
                }
            }.awaitAll()
        }.filterNotNull()
        val first = photos.firstOrNull() ?: return null
        return RecognizedPhoto(first.width, first.height, photos.flatMap { it.scaledTo(first.width, first.height).lines })
    }

    private fun RecognizedPhoto.scaledTo(w: Int, h: Int): RecognizedPhoto {
        if (width == w && height == h) return this
        val sx = w.toDouble() / width
        val sy = h.toDouble() / height
        fun TextBox.scaled() = TextBox((left * sx).toInt(), (top * sy).toInt(), (right * sx).toInt(), (bottom * sy).toInt())
        return RecognizedPhoto(w, h, lines.map { line -> RecognizedLine(line.text, line.box.scaled(), line.elements.map { RecognizedElement(it.text, it.box.scaled()) }) })
    }
}
