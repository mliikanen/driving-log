package com.mikonoma.drivinglog.vehicle.color

import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.picture.PixelSamples

/** Picks the one color that represents a photo. Behind an interface so the way it is picked can change without touching its users. */
interface ColorExtractor {
    /** The color of [samples], or null when the photo has no opaque pixel to take one from. The same samples always give the same color. */
    fun extract(samples: PixelSamples): Rgb?
}

/**
 * The representative color is the most common color of the middle of the photo:
 * 1. Only the central 80% of the square counts (a margin of 10% on each side), a deterministic stand-in for "the middle counts more".
 * 2. Pixels with an alpha below 128 are not colors; when fewer than 1% of the pixels are opaque there is no color.
 * 3. The opaque pixels are counted in a histogram of 16 levels per channel (4096 bins), with no chroma filter, so white, black and grey vehicles keep
 *    their color. The most populated bin wins (a tie goes to the lower `RRGGBB` bin, so the result is deterministic) and the color is the average of
 *    the pixels in it, which is exactly the color for a flat one.
 * (The library's quantizer was tried first and left out: for an image with few distinct colors it can merge a minority color into the majority.)
 */
class HistogramColorExtractor : ColorExtractor {

    override fun extract(samples: PixelSamples): Rgb? {
        val marginX = samples.width / 10
        val marginY = samples.height / 10
        val counts = IntArray(BINS)
        val sums = Array(3) { LongArray(BINS) }
        var region = 0
        var opaque = 0
        for (y in marginY until samples.height - marginY) for (x in marginX until samples.width - marginX) {
            region++
            val pixel = samples.argb[y * samples.width + x]
            if ((pixel ushr 24) < MIN_ALPHA) continue
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val bin = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
            counts[bin]++
            sums[0][bin] += r.toLong()
            sums[1][bin] += g.toLong()
            sums[2][bin] += b.toLong()
            opaque++
        }
        if (opaque == 0 || opaque * 100 < region) return null
        var best = 0
        for (bin in 1 until BINS) if (counts[bin] > counts[best]) best = bin
        val n = counts[best].toLong()
        fun mean(channel: Int) = ((sums[channel][best] + n / 2) / n).toInt()
        return Rgb((mean(0) shl 16) or (mean(1) shl 8) or mean(2))
    }

    companion object {
        private const val BINS = 4096
        const val MIN_ALPHA = 128
    }
}
