package com.mikonoma.drivinglog.ui.color

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mikonoma.drivinglog.vehicle.domain.Rgb

/** How long a change of a vehicle's color takes to show. */
const val COLOR_ANIMATION_MILLIS = 300

/**
 * The one animated color of a screen. It shows [target] at once the first time (no animation when a screen is opened) and, whenever [target]
 * changes, moves from the color that is being shown at that moment (also when it interrupts an earlier move) to the new one in a single animation of
 * [COLOR_ANIMATION_MILLIS], through the HCT color space ([lerpHct]). Compose's animation follows the system's animation setting, so with animations turned off
 * the change is immediate.
 *
 * **Call it once per screen** (once per row for items of a list) and pass the returned color down: everything that is derived from a vehicle's color is
 * derived from this one, so all of it moves together and nothing animates a color of its own that could run out of step.
 */
@Composable
fun rememberAnimatedColor(target: Rgb): Rgb {
    val animation = remember { AnimatedColorState(target) }
    LaunchedEffect(target) { animation.animateTo(target) }
    return animation.shown
}

private class AnimatedColorState(initial: Rgb) {
    private val progress = Animatable(1f)
    private var from = initial
    private var to = initial

    /** The color shown now: every frame of a move sets it. */
    var shown by mutableStateOf(initial)
        private set

    suspend fun animateTo(target: Rgb) {
        if (target == to) return
        from = shown
        to = target
        progress.snapTo(0f)
        progress.animateTo(1f, tween(COLOR_ANIMATION_MILLIS, easing = FastOutSlowInEasing)) {
            shown = lerpHct(from, to, value)
        }
        // A move that was cut short by another one never gets here; one that finished shows the exact target.
        shown = to
    }
}
