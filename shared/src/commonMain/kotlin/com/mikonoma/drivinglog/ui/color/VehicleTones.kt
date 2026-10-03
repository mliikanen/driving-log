package com.mikonoma.drivinglog.ui.color

import com.mikonoma.drivinglog.vehicle.domain.Rgb

/**
 * The colors a vehicle's icons are drawn in, derived from its color: the [icon] tint on its [container]. They are Material's tones for `primary` on
 * `primaryContainer` (icon tone 40 on container tone 90 in the light scheme, 80 on 30 in the dark one), so a difference of 50 tone steps gives a
 * contrast of about 4.5:1 by construction, whatever the hue and chroma. A grey, white or black vehicle color has no chroma and gives neutral tones.
 */
data class VehicleTones(val icon: Rgb, val container: Rgb) {

    companion object {
        /** The chroma is capped so that a very saturated color does not give a garish container. */
        const val MAX_CHROMA = 48.0

        // Material's primary / primaryContainer tones (see above).
        private const val LIGHT_ICON_TONE = 40.0
        private const val LIGHT_CONTAINER_TONE = 90.0
        private const val DARK_ICON_TONE = 80.0
        private const val DARK_CONTAINER_TONE = 30.0

        fun of(color: Rgb, dark: Boolean): VehicleTones = VehicleTones(
            icon = HctColors.atTone(color, if (dark) DARK_ICON_TONE else LIGHT_ICON_TONE, MAX_CHROMA),
            container = HctColors.atTone(color, if (dark) DARK_CONTAINER_TONE else LIGHT_CONTAINER_TONE, MAX_CHROMA),
        )
    }
}
