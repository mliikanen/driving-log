package com.mikonoma.drivinglog.vehicle.picture

import androidx.compose.ui.graphics.ImageBitmap
import com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage
import kotlin.math.min
import kotlin.math.roundToInt

/** A small version is at most this many pixels on a side. */
const val SMALL_SIDE = 256

/** A large version is at most this many pixels on a side. */
const val LARGE_SIDE = 1024

/**
 * An event photo's large version (`add-event-pictures`) is at most this many pixels on its longer side — larger than
 * [LARGE_SIDE], since an event photo (a dashboard, a fuel pump, a receipt) may be pinch-zoomed into for a detail,
 * unlike a vehicle's own small, always-square avatar (see design.md).
 */
const val EVENT_PHOTO_LARGE_SIDE = 2048

/** A photo is decoded no larger than this on its longer side, which bounds the memory the crop uses (about 28 MB as a bitmap). */
const val MAX_DECODE_SIDE = 3072

/** The sides, in pixels, of the two versions stored for a picture. */
data class PictureSides(val small: Int, val large: Int)

/** The sides of the versions of a crop whose side is [cropSide]: scaled down to 256 and 1024 pixels, never enlarged. */
fun pictureSides(cropSide: Int): PictureSides {
    require(cropSide > 0) { "A crop has a size" }
    return PictureSides(min(SMALL_SIDE, cropSide), min(LARGE_SIDE, cropSide))
}

/**
 * The sizes a square of [from] pixels is scaled through to reach [to] without aliasing: it is halved while at least twice the size wanted remains
 * (a single big step samples too few source pixels and aliases), then made [to] in one last, smooth step. Empty when [from] is not larger than [to]
 * (a picture is never enlarged). For 3000 to 256 that is 1500, 750, 375 and 256.
 */
fun downscaleSteps(from: Int, to: Int): List<Int> {
    require(from > 0 && to > 0) { "A picture has a size" }
    if (from <= to) return emptyList()
    val steps = mutableListOf<Int>()
    var current = from
    while (current >= to * 2) {
        current /= 2
        steps += current
    }
    if (current != to) steps += to
    return steps
}

/** The two encoded versions of one crop. */
class EncodedPicture(val small: EncodedImage, val large: EncodedImage)

/** [width] x [height], scaled so its longer side is at most [maxSide], keeping the aspect ratio; never enlarged — a
 * photo already no larger than [maxSide] on its longer side keeps its own size. Used for an event photo
 * (`add-event-pictures`), which is not cropped to a square, unlike [pictureSides]. */
fun scaledToFit(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
    require(width > 0 && height > 0) { "A photo has a size" }
    require(maxSide > 0) { "A cap has a size" }
    val longer = maxOf(width, height)
    if (longer <= maxSide) return width to height
    val scale = maxSide.toDouble() / longer
    return maxOf(1, (width * scale).roundToInt()) to maxOf(1, (height * scale).roundToInt())
}

/** A photo decoded for the crop screen: its size in pixels and, on demand, the bitmap to draw. */
interface DecodedImage {
    val width: Int
    val height: Int
    fun toImageBitmap(): ImageBitmap

    /** The photo turned [quarterTurns] quarter turns clockwise (0 gives this photo; a multiple of 4 too), so that a point (x, y) is at (height - y, x) after one turn. */
    fun turnedClockwise(quarterTurns: Int): DecodedImage
}

/**
 * Decoding and encoding photos, which only the platform can do. Both functions decode the bytes the same way (the photo's
 * orientation applied, the longer side at most [MAX_DECODE_SIDE] pixels), so a [CropRect] made on what [decode] returned means the
 * same pixels to [encodeSquare].
 */
interface ImageCodec {
    /** The photo, or null when the bytes are not an image the platform can decode. */
    suspend fun decode(bytes: ByteArray): DecodedImage?

    /**
     * The [crop] of the photo, as turned [quarterTurns] quarter turns clockwise (the crop is in the pixels of the turned photo, which is
     * what the crop screen showed), scaled to each of [sides] and encoded in the platform's size-efficient format (lossy WebP where
     * the platform can write it), or null when the bytes cannot be decoded.
     */
    suspend fun encodeSquare(bytes: ByteArray, crop: CropRect, sides: PictureSides, quarterTurns: Int = 0): EncodedPicture?

    /**
     * The whole photo (no crop), scaled to each of [caps] on its longer side (see [scaledToFit]), respecting its
     * orientation, and encoded in the platform's size-efficient format — the path an event photo takes instead of
     * [encodeSquare] (`add-event-pictures`). One [EncodedImage] per cap, in the order given; null when the bytes
     * cannot be decoded.
     */
    suspend fun encodeScaled(bytes: ByteArray, caps: List<Int>): List<EncodedImage>?

    /**
     * An already decoded image (a camera frame of the live scanner, `add-live-scanner`) encoded as it is, in the platform's
     * size-efficient format; null where the platform does not encode frames (iOS, which has no live scanner).
     */
    suspend fun encode(image: RgbImage): EncodedImage?

    /**
     * The image reduced to at most [maxSide] pixels on its longer side, as ARGB pixels (the photo's orientation applied), for taking a color from it;
     * or null when the bytes are not an image the platform can decode. Bounded, so it never holds more than [maxSide] squared pixels.
     */
    suspend fun sample(bytes: ByteArray, maxSide: Int): PixelSamples?
}
