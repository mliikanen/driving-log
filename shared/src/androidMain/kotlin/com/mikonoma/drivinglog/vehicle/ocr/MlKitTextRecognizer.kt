package com.mikonoma.drivinglog.vehicle.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.core.graphics.scale
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mikonoma.drivinglog.vehicle.data.ioDispatcher
import com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage
import com.mikonoma.drivinglog.vehicle.picture.MAX_DECODE_SIDE
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

/**
 * ML Kit's on-device Latin-script recognizer, with its model bundled in the app, so it reads text without a network
 * connection from the first scan. The photo is decoded like `AndroidImageCodec` does (orientation applied, bounded to
 * [MAX_DECODE_SIDE] with a power-of-two sample size), so the boxes are in the same pixels a screen decoding it shows.
 */
class MlKitTextRecognizer(private val dispatcher: CoroutineDispatcher = ioDispatcher) : TextRecognizer {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override val isAvailable: Boolean = true

    override suspend fun recognize(bytes: ByteArray): RecognizedPhoto? {
        val bitmap = withContext(dispatcher) { decodeForRecognition(bytes) } ?: return null
        return recognize(bitmap)
    }

    override suspend fun recognize(frame: RgbImage): RecognizedPhoto? = recognize(frame.toBitmap())

    private suspend fun recognize(bitmap: Bitmap): RecognizedPhoto {
        // A small photo is recognized at up to twice its size (design.md): small dashboard digits that are dropped or misread at their
        // own size read right when larger. The boxes are scaled back to the decoded photo's pixels.
        val scale = recognitionScale(bitmap.width, bitmap.height)
        val input = if (scale == 1.0) {
            bitmap
        } else {
            withContext(dispatcher) {
                bitmap.scale((bitmap.width * scale).roundToInt(), (bitmap.height * scale).roundToInt())
            }
        }
        val text = suspendCancellableCoroutine { continuation ->
            recognizer.process(InputImage.fromBitmap(input, 0))
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
        return RecognizedPhoto(bitmap.width, bitmap.height, text.textBlocks.flatMap { it.lines }.mapNotNull { toLine(it, scale) })
    }

    private fun toLine(line: Text.Line, scale: Double): RecognizedLine? {
        val box = line.boundingBox?.toTextBox(scale) ?: return null
        val elements = line.elements.mapNotNull { element -> element.boundingBox?.let { RecognizedElement(element.text, it.toTextBox(scale)) } }
        return RecognizedLine(line.text, box, elements)
    }

    private fun Rect.toTextBox(scale: Double) =
        TextBox((left / scale).roundToInt(), (top / scale).roundToInt(), (right / scale).roundToInt(), (bottom / scale).roundToInt())
}
