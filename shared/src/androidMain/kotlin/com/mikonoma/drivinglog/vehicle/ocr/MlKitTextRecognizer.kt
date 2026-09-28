package com.mikonoma.drivinglog.vehicle.ocr

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mikonoma.drivinglog.vehicle.data.ioDispatcher
import com.mikonoma.drivinglog.vehicle.picture.MAX_DECODE_SIDE
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * ML Kit's on-device Latin-script recognizer, with its model bundled in the app, so it reads text without a network
 * connection from the first scan. The photo is decoded like `AndroidImageCodec` does (orientation applied, bounded to
 * [MAX_DECODE_SIDE] with a power-of-two sample size), so the boxes are in the same pixels a screen decoding it shows.
 */
class MlKitTextRecognizer(private val dispatcher: CoroutineDispatcher = ioDispatcher) : TextRecognizer {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override val isAvailable: Boolean = true

    override suspend fun recognize(bytes: ByteArray): RecognizedPhoto? {
        val bitmap = withContext(dispatcher) { decodeBitmap(bytes) } ?: return null
        // A small photo is recognized at up to twice its size (design.md): small dashboard digits that are dropped or misread at their
        // own size read right when larger. The boxes are scaled back to the decoded photo's pixels.
        val scale = recognitionScale(bitmap.width, bitmap.height)
        val input = if (scale == 1.0) bitmap else withContext(dispatcher) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).roundToInt(), (bitmap.height * scale).roundToInt(), true)
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

    private fun decodeBitmap(bytes: ByteArray): Bitmap? = try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            var sample = 1
            val longer = max(info.size.width, info.size.height)
            while (longer / sample > MAX_DECODE_SIDE) sample *= 2
            if (sample > 1) decoder.setTargetSampleSize(sample)
        }
    } catch (e: Exception) {
        // Not an image, or one the platform cannot decode (ImageDecoder throws IOException or a subtype).
        null
    }
}
