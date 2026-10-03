package com.mikonoma.drivinglog.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// WCAG 2.x relative luminance and contrast ratio (https://www.w3.org/TR/WCAG21/#dfn-relative-luminance): the sRGB transfer
// function's linear segment and gamma curve, the channels' luminance weights, and the 0.05 flare term of the contrast ratio.
private const val SRGB_LINEAR_LIMIT = 0.03928
private const val SRGB_LINEAR_SLOPE = 12.92
private const val SRGB_OFFSET = 0.055
private const val SRGB_SCALE = 1.055
private const val SRGB_GAMMA = 2.4
private const val LUMINANCE_RED = 0.2126
private const val LUMINANCE_GREEN = 0.7152
private const val LUMINANCE_BLUE = 0.0722
private const val CONTRAST_FLARE = 0.05

/** The WCAG relative luminance of an opaque sRGB color, from 0 (black) to 1 (white). */
fun relativeLuminance(color: Color): Double {
    fun channel(value: Float): Double {
        val c = value.toDouble()
        return if (c <= SRGB_LINEAR_LIMIT) c / SRGB_LINEAR_SLOPE else ((c + SRGB_OFFSET) / SRGB_SCALE).pow(SRGB_GAMMA)
    }
    return LUMINANCE_RED * channel(color.red) + LUMINANCE_GREEN * channel(color.green) + LUMINANCE_BLUE * channel(color.blue)
}

/** The WCAG contrast ratio of two opaque colors, from 1 (equal) to 21 (black on white). Text needs 4.5, outlines and controls 3. */
fun contrastRatio(a: Color, b: Color): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    return (max(la, lb) + CONTRAST_FLARE) / (min(la, lb) + CONTRAST_FLARE)
}
