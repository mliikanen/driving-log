package com.mikonoma.drivinglog.vehicle.color

import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.picture.PixelSamples
import kotlin.math.exp
import kotlin.math.roundToLong

/** Picks the one color that represents a photo. Behind an interface so the way it is picked can change without touching its users. */
interface ColorExtractor {
    /** The color of [samples], or null when the photo has no opaque pixel to take one from. The same samples always give the same color. */
    fun extract(samples: PixelSamples): Rgb?
}

/**
 * The representative color is the heaviest color of the middle of the photo:
 * 1. Only the central 80% of the square counts (a margin of 10% on each side), and inside it a pixel weighs less the farther it is from the middle
 *    (a Gaussian, about 0.135 at the edge of the region), because the vehicle is what the user centered the crop on.
 * 2. A vivid pixel weighs more than a grey one: the weight is multiplied by `1 + 6 x chroma` (chroma is the spread of the pixel's channels, 0 for a grey
 *    up to 1), so a red car on grey asphalt gives red although the asphalt covers more of the photo. A photo that is mostly white or grey still gives white
 *    or grey, because nothing vivid competes.
 * 3. Pixels with an alpha below 128 are not colors; when fewer than 1% of the pixels are opaque there is no color.
 * 4. The opaque pixels are counted in a histogram of 16 levels per channel (4096 bins) with those weights. The heaviest bin wins (a tie goes to the lower
 *    `RRGGBB` bin, so the result is deterministic) and the color is the average of the pixels in it, which is exactly the color for a flat one.
 * The weights are kept as whole numbers so that equal weights are exactly equal and a tie is a tie. (The library's quantizer was tried first and left out:
 * for an image with few distinct colors it can merge a minority color into the majority.) The constants were tuned on a small set of street photos of cars
 * and motorcycles (a vehicle in the middle, asphalt, sky and buildings around): the plain histogram gave the background for most of them.
 */
class HistogramColorExtractor : ColorExtractor {

    override fun extract(samples: PixelSamples): Rgb? {
        val marginX = samples.width / 10
        val marginY = samples.height / 10
        val halfX = (samples.width - 2 * marginX) / 2.0
        val halfY = (samples.height - 2 * marginY) / 2.0
        val centerX = (samples.width - 1) / 2.0
        val centerY = (samples.height - 1) / 2.0
        val weights = LongArray(BINS)
        val counts = LongArray(BINS)
        val sums = Array(3) { LongArray(BINS) }
        var region = 0
        var opaque = 0
        for (y in marginY until samples.height - marginY) {
            for (x in marginX until samples.width - marginX) {
                region++
                val pixel = samples.argb[y * samples.width + x]
                if ((pixel ushr 24) < MIN_ALPHA) continue
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val dx = (x - centerX) / halfX
                val dy = (y - centerY) / halfY
                val chroma = (maxOf(r, g, b) - minOf(r, g, b)) / 255.0
                val weight = exp(-CENTER_FALLOFF * (dx * dx + dy * dy)) * (1.0 + CHROMA_BOOST * chroma)
                val bin = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
                weights[bin] += (weight * WEIGHT_SCALE).roundToLong()
                counts[bin]++
                sums[0][bin] += r.toLong()
                sums[1][bin] += g.toLong()
                sums[2][bin] += b.toLong()
                opaque++
            }
        }
        if (opaque == 0 || opaque * 100 < region) return null
        var best = -1
        for (bin in 0 until BINS) if (counts[bin] > 0 && (best < 0 || weights[bin] > weights[best])) best = bin
        val n = counts[best]
        fun mean(channel: Int) = ((sums[channel][best] + n / 2) / n).toInt()
        return Rgb((mean(0) shl 16) or (mean(1) shl 8) or mean(2))
    }

    companion object {
        private const val BINS = 4096
        private const val WEIGHT_SCALE = 1_000_000.0
        const val MIN_ALPHA = 128

        /** How fast the weight falls towards the edge of the counted region: `exp(-2)`, about 0.135, at the middle of an edge. */
        const val CENTER_FALLOFF = 2.0

        /** How much more a fully vivid pixel weighs than a grey one (7 times, with the 1 the grey has). */
        const val CHROMA_BOOST = 6.0
    }
}
