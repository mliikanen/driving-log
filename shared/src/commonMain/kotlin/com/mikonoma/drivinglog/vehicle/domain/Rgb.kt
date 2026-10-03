package com.mikonoma.drivinglog.vehicle.domain

import kotlinx.serialization.Serializable

/**
 * An opaque color: the 24 bits `0xRRGGBB` (alpha is dropped, a vehicle's color has none). It is what a vehicle's color is stored as, in its [hex]
 * form, which does not depend on the language, the device or the theme.
 */
@Serializable
data class Rgb(val rgb: Int) {

    /** The color as an opaque ARGB int, the form the color libraries and Compose take. */
    val argb: Int get() = OPAQUE or rgb

    /** `RRGGBB`, upper case, without a `#`. */
    val hex: String get() = rgb.toString(HEX_RADIX).uppercase().padStart(HEX_DIGITS, '0')

    init {
        require(rgb in 0..RGB_MASK) { "Not a 24-bit color: $rgb" }
    }

    companion object {
        private const val OPAQUE = 0xFF shl 24

        private const val RGB_MASK = 0xFFFFFF
        private const val HEX_RADIX = 16

        /** `RRGGBB`. */
        private const val HEX_DIGITS = 6

        /** The color of any ARGB int, alpha dropped. */
        fun fromArgb(argb: Int): Rgb = Rgb(argb and RGB_MASK)

        /** The color of exactly six hexadecimal digits, either case, or null for any other text (no `#`, no alpha, no spaces). */
        fun parse(text: String?): Rgb? {
            if (text == null || text.length != HEX_DIGITS) return null
            if (!text.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
            return Rgb(text.toInt(HEX_RADIX))
        }
    }
}
