package com.mikonoma.drivinglog.vehicle.ocr.ppocr

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// Where the channels sit in a 0xAARRGGBB pixel, and their range.
private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val BITS_PER_CHANNEL = 8
private const val CHANNEL_MASK = 0xFF
private const val MAX_CHANNEL = 255
private const val ROUND_HALF_UP = 0.5
private const val QUARTER_TURNS_PER_TURN = 4

/** A photo as the PP-OCR pipeline works on it: [width] x [height] pixels, each `0xAARRGGBB`, row by row. */
class RgbImage(val width: Int, val height: Int, val pixels: IntArray) {
    init {
        require(width > 0 && height > 0 && pixels.size == width * height) { "An image has a size and its pixels" }
    }

    /** The red, green or blue channel ([channel] 0, 1, 2) of the pixel at ([x], [y]), 0 to 255, the coordinates clamped into the image. */
    fun channel(x: Int, y: Int, channel: Int): Int {
        val p = pixels[y.coerceIn(0, height - 1) * width + x.coerceIn(0, width - 1)]
        return (p shr (RED_SHIFT - BITS_PER_CHANNEL * channel)) and CHANNEL_MASK
    }

    /** The value of [channel] at the fractional point ([x], [y]), interpolated between the four nearest pixels, edges replicated. */
    fun sample(x: Double, y: Double, channel: Int): Double {
        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        val fx = x - x0
        val fy = y - y0
        val top = channel(x0, y0, channel) * (1 - fx) + channel(x0 + 1, y0, channel) * fx
        val bottom = channel(x0, y0 + 1, channel) * (1 - fx) + channel(x0 + 1, y0 + 1, channel) * fx
        return top * (1 - fy) + bottom * fy
    }

    /**
     * This image resized to [newWidth] x [newHeight] by bilinear interpolation with pixel centers aligned, as OpenCV's `resize` does
     * by default, which is what the models were evaluated with.
     */
    fun resized(newWidth: Int, newHeight: Int): RgbImage {
        require(newWidth > 0 && newHeight > 0) { "A resized image has a size" }
        if (newWidth == width && newHeight == height) return this
        val sx = width.toDouble() / newWidth
        val sy = height.toDouble() / newHeight
        val out = IntArray(newWidth * newHeight)
        for (y in 0 until newHeight) {
            val srcY = max(0.0, (y + 0.5) * sy - 0.5)
            for (x in 0 until newWidth) {
                val srcX = max(0.0, (x + 0.5) * sx - 0.5)
                out[y * newWidth + x] = rgb(sample(srcX, srcY, 0), sample(srcX, srcY, 1), sample(srcX, srcY, 2))
            }
        }
        return RgbImage(newWidth, newHeight, out)
    }

    /** This image turned a quarter turn counterclockwise (NumPy's `rot90`). */
    fun turnedCounterclockwise(): RgbImage {
        val out = IntArray(width * height)
        // The new image is height wide and width tall; its row r is the old column width - 1 - r.
        for (r in 0 until width) for (c in 0 until height) out[r * height + c] = pixels[c * width + (width - 1 - r)]
        return RgbImage(height, width, out)
    }

    /**
     * This image turned [quarterTurns] quarter turns clockwise (a multiple of 4 gives it back): how a camera frame whose content is
     * rotated by `rotationDegrees` is made upright, turned `rotationDegrees / 90` times.
     */
    fun turnedClockwise(quarterTurns: Int): RgbImage {
        var image = this
        // Three counterclockwise quarters are one clockwise quarter.
        repeat((QUARTER_TURNS_PER_TURN - quarterTurns.mod(QUARTER_TURNS_PER_TURN)) % QUARTER_TURNS_PER_TURN) { image = image.turnedCounterclockwise() }
        return image
    }

    companion object {
        fun rgb(r: Double, g: Double, b: Double): Int = (CHANNEL_MASK shl ALPHA_SHIFT) or (clamp(r) shl RED_SHIFT) or (clamp(g) shl GREEN_SHIFT) or clamp(b)

        private fun clamp(v: Double): Int = min(MAX_CHANNEL, max(0, (v + ROUND_HALF_UP).toInt()))
    }
}
