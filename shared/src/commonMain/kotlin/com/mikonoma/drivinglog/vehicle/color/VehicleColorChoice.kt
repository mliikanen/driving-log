package com.mikonoma.drivinglog.vehicle.color

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mikonoma.drivinglog.ui.color.rememberAnimatedColor
import com.mikonoma.drivinglog.ui.color.toColor
import com.mikonoma.drivinglog.ui.theme.contrastRatio
import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleColors

/** One preset swatch of the palette: the color, its name (also its accessibility label), its test tag and whether it is the selected one. */
internal data class ColorSwatch(val name: String, val color: Rgb, val tag: String, val selected: Boolean)

/** One segment of the row beneath the palette: a color that is not a preset, with the text that says what it is. */
internal data class ColorSegment(val label: String, val color: Rgb, val tag: String, val selected: Boolean)

/** What the color choice shows: the [palette] of presets and, beneath it, the [segments] that share one row (none: no row). */
internal data class ColorChoiceModel(val palette: List<ColorSwatch>, val segments: List<ColorSegment>)

/**
 * The color choice for the vehicle's current [color]. The palette is the twelve presets. The segments are, in this order, "Old color" (the [savedColor], on the
 * edit screen), "Photo color" (the [pictureColor], after a crop was confirmed) and "Current color" (the [color], when it is none of the presets and equals neither
 * of the other two, such as the photo color after its picture was removed). Exactly one element is selected: the preset's swatch when [color] is that preset,
 * otherwise the first of the segments whose color is [color].
 */
internal fun colorChoice(color: Rgb, pictureColor: Rgb?, savedColor: Rgb?): ColorChoiceModel {
    val preset = VehicleColors.presetOf(color)
    val current = color.takeIf { preset == null && it != pictureColor && it != savedColor }
    val candidates = buildList {
        if (savedColor != null) add(Triple("Old color", savedColor, "vehicle_color_old"))
        if (pictureColor != null) add(Triple("Photo color", pictureColor, "vehicle_color_picture"))
        if (current != null) add(Triple("Current color", current, "vehicle_color_current"))
    }
    // The photo color is the newest thing the user did, so it is shown before the old color when both are there and equal; the order of the row is old, photo, current.
    val selectedLabel = if (preset != null) null else listOf("Photo color", "Old color", "Current color").firstOrNull { label -> candidates.any { it.first == label && it.second == color } }
    return ColorChoiceModel(
        palette = VehicleColors.presets.map { ColorSwatch(it.name, it.color, "vehicle_color_${it.color.hex}", selected = it.color == color) },
        segments = candidates.map { (label, segmentColor, tag) -> ColorSegment(label, segmentColor, tag, selected = label == selectedLabel) },
    )
}

/**
 * The choice of the vehicle's color, used by the add and edit screens: the palette of twelve presets as round swatches and, beneath it, a full-width row of
 * segments for the colors that are not presets, each with its label written on the color. Exactly one element is selected and there is no "none".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleColorChoice(
    color: Rgb,
    pictureColor: Rgb?,
    savedColor: Rgb?,
    onSelect: (Rgb) -> Unit,
    modifier: Modifier = Modifier,
) {
    val choice = colorChoice(color, pictureColor, savedColor)
    Column(modifier.testTag("vehicle_color_choice"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Vehicle color", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (swatch in choice.palette) Swatch(swatch, onSelect = { onSelect(swatch.color) })
        }
        if (choice.segments.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (segment in choice.segments) {
                    // The photo color is the one that changes in real time, and only when a newer photo replaces it; the others never change while the form is open.
                    val shown = if (segment.tag == "vehicle_color_picture") rememberAnimatedColor(segment.color) else segment.color
                    Segment(segment, shown, Modifier.weight(1f), onSelect = { onSelect(segment.color) })
                }
            }
        }
    }
}

/** Black or white, whichever reads better on [fill] (at least 4.5:1 on any color). */
private fun markColor(fill: Color): Color = if (contrastRatio(fill, Color.White) >= contrastRatio(fill, Color.Black)) Color.White else Color.Black

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
        if (swatch.selected) Icon(Icons.Filled.Check, contentDescription = null, tint = markColor(fill), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Segment(segment: ColorSegment, shown: Rgb, modifier: Modifier, onSelect: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val fill = shown.toColor()
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(fill)
            .border(BorderStroke(if (segment.selected) 3.dp else 1.dp, if (segment.selected) colors.primary else colors.outline), shape)
            .selectable(selected = segment.selected, onClick = onSelect, role = Role.RadioButton)
            .semantics { contentDescription = segment.label }
            .testTag(segment.tag),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (segment.selected) Icon(Icons.Filled.Check, contentDescription = null, tint = markColor(fill), modifier = Modifier.size(20.dp))
            // The label is written on the color so that it says what the segment is, in the color that reads best on it.
            Text(segment.label, style = MaterialTheme.typography.labelLarge, color = markColor(fill), maxLines = 1)
        }
    }
}
