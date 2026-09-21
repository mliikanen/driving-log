package com.mikonoma.drivinglog.vehicle.picture

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.mikonoma.drivinglog.vehicle.data.ioDispatcher
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Decodes with [ImageDecoder], which applies the photo's EXIF orientation, and encodes lossy WebP (API 30, and the minimum
 * is 33). The decode is bounded to [MAX_DECODE_SIDE] with a power-of-two sample size, which is the same for [decode] and
 * [encodeSquare], so a crop made on one means the same pixels to the other. A sample size (and not a target size) is used because
 * the decoder reports the size before the orientation is applied, and a target size would be ambiguous for a rotated photo.
 */
class AndroidImageCodec(private val dispatcher: CoroutineDispatcher = ioDispatcher) : ImageCodec {

    private class Decoded(private val bitmap: Bitmap) : DecodedImage {
        override val width: Int get() = bitmap.width
        override val height: Int get() = bitmap.height
        override fun toImageBitmap(): ImageBitmap = bitmap.asImageBitmap()
        override fun turnedClockwise(quarterTurns: Int): DecodedImage = if (quarterTurns.mod(4) == 0) this else Decoded(turned(bitmap, quarterTurns))
    }

    override suspend fun decode(bytes: ByteArray): DecodedImage? =
        withContext(dispatcher) { decodeBitmap(bytes)?.let(::Decoded) }

    override suspend fun encodeSquare(bytes: ByteArray, crop: CropRect, sides: PictureSides, quarterTurns: Int): EncodedPicture? =
        withContext(dispatcher) {
            val bitmap = decodeBitmap(bytes)?.let { turned(it, quarterTurns) } ?: return@withContext null
            val side = crop.side.coerceIn(1, minOf(bitmap.width, bitmap.height))
            val x = crop.x.coerceIn(0, bitmap.width - side)
            val y = crop.y.coerceIn(0, bitmap.height - side)
            val square = Bitmap.createBitmap(bitmap, x, y, side, side)
            EncodedPicture(encode(square, sides.small), encode(square, sides.large))
        }

    override suspend fun sample(bytes: ByteArray, maxSide: Int): PixelSamples? = withContext(dispatcher) {
        try {
            val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                var sample = 1
                val longer = max(info.size.width, info.size.height)
                while (longer / sample > maxSide) sample *= 2
                if (sample > 1) decoder.setTargetSampleSize(sample)
            }
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            PixelSamples(bitmap.width, bitmap.height, pixels)
        } catch (e: Exception) {
            null
        }
    }

    /** A software bitmap (a hardware one cannot be cropped or compressed), or null when the bytes are not an image. */
    private fun decodeBitmap(bytes: ByteArray): Bitmap? = try {
        val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
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

    private fun encode(square: Bitmap, side: Int): EncodedImage {
        val scaled = scaledTo(square, side)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, WEBP_QUALITY, out)
        return EncodedImage(out.toByteArray(), "webp", side, side)
    }

    /** Halves while at least twice the size wanted remains (a single big step aliases), then makes the last, smooth step. */
    private fun scaledTo(square: Bitmap, side: Int): Bitmap {
        var current = square
        for (step in downscaleSteps(square.width, side)) {
            current = Bitmap.createScaledBitmap(current, step, step, true)
        }
        return current
    }

    private companion object {
        const val WEBP_QUALITY = 80

        /** [bitmap] turned [quarterTurns] quarter turns clockwise (the bitmap itself for none). */
        fun turned(bitmap: Bitmap, quarterTurns: Int): Bitmap {
            val turns = quarterTurns.mod(4)
            if (turns == 0) return bitmap
            val matrix = Matrix().apply { postRotate(90f * turns) }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }
}
