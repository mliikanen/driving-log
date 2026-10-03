package com.mikonoma.drivinglog.vehicle.picture

import androidx.compose.ui.graphics.ImageBitmap

/**
 * A codec for tests that never touches a bitmap: empty bytes are "not an image", any other bytes decode to an image of
 * [width] x [height], and an encoded crop's bytes are the crop and the sides, so a test can see what was asked for.
 */
class FakeImageCodec(var width: Int = 4000, var height: Int = 3000) : ImageCodec {

    class Encode(val bytes: ByteArray, val crop: CropRect, val sides: PictureSides, val quarterTurns: Int = 0)
    class ScaledEncode(val bytes: ByteArray, val caps: List<Int>)

    val encodes = mutableListOf<Encode>()
    val scaledEncodes = mutableListOf<ScaledEncode>()
    var decodeCount = 0

    /** What [sample] gives for bytes that are an image: a flat mid grey unless a test sets something else. */
    var samples: PixelSamples? = PixelSamples(4, 4, IntArray(16) { 0xFF808080.toInt() })
    val sampledBytes = mutableListOf<ByteArray>()

    /** When set, [encodeSquare] throws it. */
    var encodeFailure: Throwable? = null

    private class Decoded(override val width: Int, override val height: Int) : DecodedImage {
        override fun toImageBitmap(): ImageBitmap = error("A fake image has no bitmap")
        override fun turnedClockwise(quarterTurns: Int): DecodedImage = if (quarterTurns.mod(2) == 0) this else Decoded(height, width)
    }

    override suspend fun decode(bytes: ByteArray): DecodedImage? {
        decodeCount++
        return if (bytes.isEmpty()) null else Decoded(width, height)
    }

    override suspend fun encodeSquare(bytes: ByteArray, crop: CropRect, sides: PictureSides, quarterTurns: Int): EncodedPicture? {
        encodeFailure?.let { throw it }
        if (bytes.isEmpty()) return null
        encodes += Encode(bytes, crop, sides, quarterTurns)
        return EncodedPicture(version(crop, sides.small, 0), version(crop, sides.large, 1))
    }

    val encodedFrames = mutableListOf<com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage>()

    override suspend fun encode(image: com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage): EncodedImage {
        encodedFrames += image
        return EncodedImage(byteArrayOf(7), "webp", image.width, image.height)
    }

    override suspend fun encodeScaled(bytes: ByteArray, caps: List<Int>): List<EncodedImage>? {
        encodeFailure?.let { throw it }
        if (bytes.isEmpty()) return null
        scaledEncodes += ScaledEncode(bytes, caps)
        return caps.map { cap ->
            val (w, h) = scaledToFit(width, height, cap)
            EncodedImage(byteArrayOf(cap.toByte()), "webp", w, h)
        }
    }

    override suspend fun sample(bytes: ByteArray, maxSide: Int): PixelSamples? {
        sampledBytes += bytes
        return if (bytes.isEmpty()) null else samples
    }

    private fun version(crop: CropRect, side: Int, marker: Int) =
        EncodedImage(byteArrayOf(marker.toByte(), crop.x.toByte(), crop.y.toByte(), crop.side.toByte()), "webp", side, side)
}
