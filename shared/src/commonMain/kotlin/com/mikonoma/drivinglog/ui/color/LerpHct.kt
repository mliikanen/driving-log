package com.mikonoma.drivinglog.ui.color

import com.mikonoma.drivinglog.vehicle.domain.Rgb
import kotlin.math.abs

/** A chroma below this is a grey, whose hue means nothing: the other color's hue is used instead. */
private const val GREY_CHROMA = 2.0

/**
 * The color [progress] of the way (0 to 1) from [from] to [to], moving through HCT: the hue by the shortest way around the color wheel, chroma and tone
 * in a straight line. Moving through hue, chroma and tone keeps an intermediate color looking like a color on the way (no muddy grey in the middle of red
 * to blue, as a straight line in RGB gives). The ends are exact: 0 gives [from] and 1 gives [to].
 */
fun lerpHct(from: Rgb, to: Rgb, progress: Float): Rgb {
    if (progress <= 0f || from == to) return from
    if (progress >= 1f) return to
    val mixed = lerpHctValues(HctColors.read(from), HctColors.read(to), progress.toDouble())
    return HctColors.build(mixed.hue, mixed.chroma, mixed.tone)
}

/** The pure part of [lerpHct], on hue, chroma and tone. */
fun lerpHctValues(from: HctValue, to: HctValue, t: Double): HctValue {
    val fromHue = if (from.chroma < GREY_CHROMA) to.hue else from.hue
    val toHue = if (to.chroma < GREY_CHROMA) from.hue else to.hue
    var delta = (toHue - fromHue).mod(360.0)
    if (delta > 180.0) delta -= 360.0
    return HctValue(
        hue = (fromHue + delta * t).mod(360.0),
        chroma = from.chroma + (to.chroma - from.chroma) * t,
        tone = from.tone + (to.tone - from.tone) * t,
    )
}

/** The distance in degrees between two hues, the short way around (0 to 180). */
internal fun hueDistance(a: Double, b: Double): Double = abs((a - b + 540.0).mod(360.0) - 180.0)
