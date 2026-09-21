package com.mikonoma.drivinglog.vehicle.picture

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.mikonoma.drivinglog.vehicle.data.ioDispatcher
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

/**
 * Decodes with `UIImage` and encodes PNG: the image APIs on iOS read WebP but cannot write it, so the versions are stored as PNG
 * until a WebP encoder is added (see the design). Every image is first drawn once through a renderer, which bakes the photo's
 * orientation into the pixels and bounds the longer side to [MAX_DECODE_SIDE], so [decode] and [encodeSquare] see the same pixels.
 */
@OptIn(ExperimentalForeignApi::class)
class IosImageCodec(private val dispatcher: CoroutineDispatcher = ioDispatcher) : ImageCodec {

    private class Decoded(private val image: UIImage, override val width: Int, override val height: Int) : DecodedImage {
        // Skia decodes the PNG of the upright image; it applies no orientation of its own.
        override fun toImageBitmap(): ImageBitmap =
            Image.makeFromEncoded(requireNotNull(UIImagePNGRepresentation(image)).toByteArray()).toComposeImageBitmap()
    }

    override suspend fun decode(bytes: ByteArray): DecodedImage? = withContext(dispatcher) {
        val upright = upright(bytes) ?: return@withContext null
        Decoded(upright, upright.size.useContents { width.roundToInt() }, upright.size.useContents { height.roundToInt() })
    }

    override suspend fun encodeSquare(bytes: ByteArray, crop: CropRect, sides: PictureSides): EncodedPicture? =
        withContext(dispatcher) {
            val image = upright(bytes) ?: return@withContext null
            EncodedPicture(encode(image, crop, sides.small), encode(image, crop, sides.large))
        }

    override suspend fun sample(bytes: ByteArray, maxSide: Int): PixelSamples? = withContext(dispatcher) {
        val image = upright(bytes) ?: return@withContext null
        val (width, height) = image.size.useContents { width to height }
        val scale = minOf(1.0, maxSide / max(width, height))
        val w = max(1, (width * scale).roundToInt())
        val h = max(1, (height * scale).roundToInt())
        val cgImage = image.CGImage ?: return@withContext null
        // Draw into an RGBA buffer of the sample's size, then turn each premultiplied pixel into a plain ARGB int.
        val rgba = ByteArray(w * h * 4)
        val space = CGColorSpaceCreateDeviceRGB()
        rgba.usePinned { pinned ->
            val context = CGBitmapContextCreate(
                pinned.addressOf(0), w.toULong(), h.toULong(), 8u, (w * 4).toULong(), space, CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
            )
            CGContextDrawImage(context, CGRectMake(0.0, 0.0, w.toDouble(), h.toDouble()), cgImage)
            CGContextRelease(context)
        }
        CGColorSpaceRelease(space)
        val pixels = IntArray(w * h) { i ->
            val a = rgba[i * 4 + 3].toInt() and 0xFF
            fun channel(offset: Int): Int {
                val value = rgba[i * 4 + offset].toInt() and 0xFF
                return if (a == 0 || a == 255) value else minOf(255, value * 255 / a)
            }
            (a shl 24) or (channel(0) shl 16) or (channel(1) shl 8) or channel(2)
        }
        PixelSamples(w, h, pixels)
    }

    /** The photo upright and no larger than [MAX_DECODE_SIDE] on its longer side, or null when it is not an image. */
    private fun upright(bytes: ByteArray): UIImage? {
        if (bytes.isEmpty()) return null
        val image = UIImage.imageWithData(bytes.toNSData()) ?: return null
        val (width, height) = image.size.useContents { width to height }
        if (width <= 0.0 || height <= 0.0) return null
        val scale = minOf(1.0, MAX_DECODE_SIDE / max(width, height))
        return render(width * scale, height * scale) { image.drawInRect(CGRectMake(0.0, 0.0, width * scale, height * scale)) }
    }

    /** The [crop] of the upright photo, scaled to [side] pixels, as PNG. */
    private fun encode(image: UIImage, crop: CropRect, side: Int): EncodedImage {
        val scale = side.toDouble() / crop.side
        val (width, height) = image.size.useContents { width to height }
        val scaled = render(side.toDouble(), side.toDouble()) {
            image.drawInRect(CGRectMake(-crop.x * scale, -crop.y * scale, width * scale, height * scale))
        }
        return EncodedImage(requireNotNull(UIImagePNGRepresentation(scaled)).toByteArray(), "png", side, side)
    }

    private fun render(width: Double, height: Double, draw: () -> Unit): UIImage {
        val format = UIGraphicsImageRendererFormat.defaultFormat().apply {
            scale = 1.0 // pixels, not points
            opaque = false
        }
        return UIGraphicsImageRenderer(size = CGSizeMake(width, height), format = format).imageWithActions { draw() }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isNotEmpty()) result.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return result
}
