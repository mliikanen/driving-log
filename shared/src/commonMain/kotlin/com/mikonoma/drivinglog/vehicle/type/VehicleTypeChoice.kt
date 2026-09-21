package com.mikonoma.drivinglog.vehicle.type

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikonoma.drivinglog.ui.VehicleIcons
import com.mikonoma.drivinglog.ui.color.VehicleTones
import com.mikonoma.drivinglog.ui.color.toColor
import com.mikonoma.drivinglog.ui.theme.DrivingLogTheme
import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleType

/**
 * The choice of a vehicle's type, used by the add and edit screens: a wrapping row of selectable tiles (the type's icon over its name, in the order
 * of [VehicleType.entries]), the selected one marked with a filled container and a check. [selected] is null only on the edit screen until the
 * saved vehicle has loaded, when nothing is marked. There is no tile for "none": a vehicle always has a type.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleTypeChoice(
    selected: VehicleType?,
    onSelect: (VehicleType) -> Unit,
    /** The vehicle's color, already animated by the screen: the tiles' icons and backgrounds are drawn from it. */
    color: Rgb,
    modifier: Modifier = Modifier,
) {
    Column(modifier.testTag("vehicle_type_choice"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Vehicle type", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val tones = VehicleTones.of(color, DrivingLogTheme.isDark)
            for (type in VehicleType.entries) TypeTile(type, selected = type == selected, tones = tones, onSelect = { onSelect(type) })
        }
    }
}

@Composable
private fun TypeTile(type: VehicleType, selected: Boolean, tones: VehicleTones, onSelect: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .width(82.dp)
            .clip(shape)
            .background(tones.container.toColor())
            .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant), shape)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .testTag("vehicle_type_${type.code}"),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp).align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = VehicleIcons.of(type),
                contentDescription = null,
                tint = tones.icon.toColor(),
                modifier = Modifier.size(48.dp),
            )
            Text(
                type.label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        if (selected) {
            // The check sits in the tile's corner, clear of the icon.
            Box(
                Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).clip(CircleShape).background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(12.dp))
            }
        }
    }
}
