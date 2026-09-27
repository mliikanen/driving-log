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
import platform.CoreGraphics.CGAffineTransformMake
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextConcatCTM
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextSetInterpolationQuality
import platform.CoreGraphics.kCGInterpolationHigh
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererContext
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

        override fun turnedClockwise(quarterTurns: Int): DecodedImage {
            val turns = quarterTurns.mod(4)
            if (turns == 0) return this
            val turned = turnedImage(image, turns)
            return Decoded(turned, turned.size.useContents { width.roundToInt() }, turned.size.useContents { height.roundToInt() })
        }
    }

    override suspend fun decode(bytes: ByteArray): DecodedImage? = withContext(dispatcher) {
        val upright = upright(bytes) ?: return@withContext null
        Decoded(upright, upright.size.useContents { width.roundToInt() }, upright.size.useContents { height.roundToInt() })
    }

    override suspend fun encodeSquare(bytes: ByteArray, crop: CropRect, sides: PictureSides, quarterTurns: Int): EncodedPicture? =
        withContext(dispatcher) {
            val image = upright(bytes)?.let { if (quarterTurns.mod(4) == 0) it else turnedImage(it, quarterTurns) } ?: return@withContext null
            EncodedPicture(encode(image, crop, sides.small), encode(image, crop, sides.large))
        }

    override suspend fun encodeScaled(bytes: ByteArray, caps: List<Int>): List<EncodedImage>? = withContext(dispatcher) {
        val image = upright(bytes) ?: return@withContext null
        val (width, height) = image.size.useContents { width to height }
        caps.map { cap ->
            val (targetWidth, targetHeight) = scaledToFit(width.roundToInt(), height.roundToInt(), cap)
            val scaled = renderImage(targetWidth.toDouble(), targetHeight.toDouble()) { context ->
                CGContextSetInterpolationQuality(context?.CGContext, kCGInterpolationHigh)
                image.drawInRect(CGRectMake(0.0, 0.0, targetWidth.toDouble(), targetHeight.toDouble()))
            }
            EncodedImage(requireNotNull(UIImagePNGRepresentation(scaled)).toByteArray(), "png", targetWidth, targetHeight)
        }
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
        // High interpolation quality: a reduction from up to 3072 pixels to 256 aliases with the default one.
        val scaled = renderImage(side.toDouble(), side.toDouble()) { context ->
            CGContextSetInterpolationQuality(context?.CGContext, kCGInterpolationHigh)
            image.drawInRect(CGRectMake(-crop.x * scale, -crop.y * scale, width * scale, height * scale))
        }
        return EncodedImage(requireNotNull(UIImagePNGRepresentation(scaled)).toByteArray(), "png", side, side)
    }

    private fun render(width: Double, height: Double, draw: () -> Unit): UIImage = renderImage(width, height) { draw() }
}

@OptIn(ExperimentalForeignApi::class)
private fun renderImage(width: Double, height: Double, draw: (UIGraphicsImageRendererContext?) -> Unit): UIImage {
    val format = UIGraphicsImageRendererFormat.defaultFormat().apply {
        scale = 1.0 // pixels, not points
        opaque = false
    }
    return UIGraphicsImageRenderer(size = CGSizeMake(width, height), format = format).imageWithActions { draw(it) }
}

/** [image] turned [quarterTurns] quarter turns clockwise, one turn at a time: a point (x, y) goes to (height - y, x). */
@OptIn(ExperimentalForeignApi::class)
private fun turnedImage(image: UIImage, quarterTurns: Int): UIImage {
    var current = image
    repeat(quarterTurns.mod(4)) {
        val (width, height) = current.size.useContents { width to height }
        val source = current
        current = renderImage(height, width) { context ->
            CGContextConcatCTM(context?.CGContext, CGAffineTransformMake(0.0, 1.0, -1.0, 0.0, height, 0.0))
            source.drawInRect(CGRectMake(0.0, 0.0, width, height))
        }
    }
    return current
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
