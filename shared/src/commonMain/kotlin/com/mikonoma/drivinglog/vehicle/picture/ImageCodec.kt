package com.mikonoma.drivinglog.vehicle.picture

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.min

/** A small version is at most this many pixels on a side. */
const val SMALL_SIDE = 256

/** A large version is at most this many pixels on a side. */
const val LARGE_SIDE = 1024

/** A photo is decoded no larger than this on its longer side, which bounds the memory the crop uses (about 28 MB as a bitmap). */
const val MAX_DECODE_SIDE = 3072

/** The sides, in pixels, of the two versions stored for a picture. */
data class PictureSides(val small: Int, val large: Int)

/** The sides of the versions of a crop whose side is [cropSide]: scaled down to 256 and 1024 pixels, never enlarged. */
fun pictureSides(cropSide: Int): PictureSides {
    require(cropSide > 0) { "A crop has a size" }
    return PictureSides(min(SMALL_SIDE, cropSide), min(LARGE_SIDE, cropSide))
}

/** The two encoded versions of one crop. */
class EncodedPicture(val small: EncodedImage, val large: EncodedImage)

/** A photo decoded for the crop screen: its size in pixels and, on demand, the bitmap to draw. */
interface DecodedImage {
    val width: Int
    val height: Int
    fun toImageBitmap(): ImageBitmap
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
     * The [crop] of the photo scaled to each of [sides] and encoded in the platform's size-efficient format (lossy WebP where
     * the platform can write it), or null when the bytes cannot be decoded.
     */
    suspend fun encodeSquare(bytes: ByteArray, crop: CropRect, sides: PictureSides): EncodedPicture?

    /**
     * The image reduced to at most [maxSide] pixels on its longer side, as ARGB pixels (the photo's orientation applied), for taking a color from it;
     * or null when the bytes are not an image the platform can decode. Bounded, so it never holds more than [maxSide] squared pixels.
     */
    suspend fun sample(bytes: ByteArray, maxSide: Int): PixelSamples?
}
