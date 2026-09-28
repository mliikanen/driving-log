package com.mikonoma.drivinglog.vehicle.ocr

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage
import com.mikonoma.drivinglog.vehicle.picture.MAX_DECODE_SIDE
import java.nio.ByteBuffer
import kotlin.math.max

/**
 * The photo decoded like `AndroidImageCodec` does (orientation applied, bounded to [MAX_DECODE_SIDE] with a power-of-two sample size),
 * so every recognizer's boxes are in the same pixels a screen decoding it shows; or null when the bytes are not an image.
 */
internal fun decodeForRecognition(bytes: ByteArray): Bitmap? = try {
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

/** [bitmap]'s pixels, for the PP-OCR pipeline. */
internal fun Bitmap.toRgbImage(): RgbImage {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    return RgbImage(width, height, pixels)
}
