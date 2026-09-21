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

        fun of(color: Rgb, dark: Boolean): VehicleTones = VehicleTones(
            icon = HctColors.atTone(color, if (dark) 80.0 else 40.0, MAX_CHROMA),
            container = HctColors.atTone(color, if (dark) 30.0 else 90.0, MAX_CHROMA),
        )
    }
}
