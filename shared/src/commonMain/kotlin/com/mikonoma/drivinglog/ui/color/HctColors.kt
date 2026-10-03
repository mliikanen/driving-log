package com.mikonoma.drivinglog.ui.color

import androidx.compose.ui.graphics.Color
import com.materialkolor.hct.Hct
import com.mikonoma.drivinglog.vehicle.domain.Rgb

/** Hues are degrees around the color wheel; tones run from 0 (black) to 100 (white). */
private const val FULL_TURN = 360.0
private const val MAX_TONE = 100.0

/** A color in the HCT color space: hue in degrees (0 up to 360), chroma (0 for greys up to about 120) and tone (0 black to 100 white). */
data class HctValue(val hue: Double, val chroma: Double, val tone: Double)

/**
 * The one place that touches the color library (Material Color Utilities, HCT): everything else in the app uses [Rgb] and [HctValue]. If the
 * library has to be replaced, this file is what changes.
 */
object HctColors {

    /** The hue, chroma and tone of [color]. */
    fun read(color: Rgb): HctValue = Hct.fromInt(color.argb).let { HctValue(it.hue, it.chroma, it.tone) }

    /**
     * The color with [hue], [chroma] and [tone]. A chroma that does not exist at that hue and tone (very light or dark tones cannot be very
     * colorful) gives the most colorful color that does.
     */
    fun build(hue: Double, chroma: Double, tone: Double): Rgb =
        Rgb.fromArgb(Hct.from(hue.mod(FULL_TURN), chroma.coerceAtLeast(0.0), tone.coerceIn(0.0, MAX_TONE)).toInt())

    /** The same color at another tone, with its chroma at most [maxChroma]. */
    fun atTone(color: Rgb, tone: Double, maxChroma: Double): Rgb {
        val hct = read(color)
        return build(hct.hue, minOf(hct.chroma, maxChroma), tone)
    }
}

/** The Compose color of [this]. */
fun Rgb.toColor(): Color = Color(argb)
