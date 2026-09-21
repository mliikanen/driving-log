package com.mikonoma.drivinglog.vehicle.color

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mikonoma.drivinglog.ui.color.toColor
import com.mikonoma.drivinglog.ui.theme.contrastRatio
import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleColors

/** One swatch of the color choice: the color, its name (also its accessibility label) and its test tag. */
internal data class ColorSwatch(val name: String, val color: Rgb, val tag: String, val selected: Boolean)

/**
 * The swatches to offer for the vehicle's current [color]: the [pictureColor] first when there is one ("Picture color"), then the current color when
 * it is neither a preset nor the picture color ("Current color"), then the twelve presets. Exactly one is selected: the picture color when it is the
 * current color, else the current color's own swatch, else its preset.
 */
internal fun colorSwatches(color: Rgb, pictureColor: Rgb?): List<ColorSwatch> {
    val current = color.takeIf { it != pictureColor && VehicleColors.presetOf(it) == null }
    val pictureSelected = pictureColor != null && pictureColor == color
    return buildList {
        if (pictureColor != null) add(ColorSwatch("Picture color", pictureColor, "vehicle_color_picture", selected = pictureSelected))
        if (current != null) add(ColorSwatch("Current color", current, "vehicle_color_current", selected = true))
        for (preset in VehicleColors.presets) {
            add(ColorSwatch(preset.name, preset.color, "vehicle_color_${preset.color.hex}", selected = !pictureSelected && preset.color == color))
        }
    }
}

/**
 * The choice of the vehicle's color, used by the add and edit screens: the swatches of [colorSwatches] in a wrapping row. Exactly one is selected and
 * there is no "none".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleColorChoice(
    color: Rgb,
    pictureColor: Rgb?,
    onSelect: (Rgb) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.testTag("vehicle_color_choice"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Vehicle color", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (swatch in colorSwatches(color, pictureColor)) Swatch(swatch, onSelect = { onSelect(swatch.color) })
        }
    }
}

@Composable
private fun Swatch(swatch: ColorSwatch, onSelect: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val fill = swatch.color.toColor()
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(fill)
            // The ring keeps a swatch that is close to the background (white, silver) visible, and marks the selected one.
            .border(BorderStroke(if (swatch.selected) 3.dp else 1.dp, if (swatch.selected) colors.primary else colors.outline), CircleShape)
            .selectable(selected = swatch.selected, onClick = onSelect, role = Role.RadioButton)
            .semantics { contentDescription = swatch.name }
            .testTag(swatch.tag),
        contentAlignment = Alignment.Center,
    ) {
        if (swatch.selected) {
            // A check in whichever of black and white reads better on this swatch.
            val mark = if (contrastRatio(fill, Color.White) >= contrastRatio(fill, Color.Black)) Color.White else Color.Black
            Icon(Icons.Filled.Check, contentDescription = null, tint = mark, modifier = Modifier.size(22.dp))
        }
    }
}
