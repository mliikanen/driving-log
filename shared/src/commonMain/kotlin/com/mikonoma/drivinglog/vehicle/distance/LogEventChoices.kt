package com.mikonoma.drivinglog.vehicle.distance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mikonoma.drivinglog.ui.VehiclePicture
import com.mikonoma.drivinglog.vehicle.domain.FuelType
import com.mikonoma.drivinglog.vehicle.domain.FuelUnit

/**
 * The kind of event being logged: Material 3's own dropdown pattern, like [VehicleSelector], with one item, "Distance". [enabled]
 * is `false` while [LogKind] has one entry, so the field's own disabled colors, semantics and blocked tap are Material's disabled
 * treatment (the same one the Home screen's not-yet-available tiles use), and none of it is written here; it becomes a real choice
 * once a later change adds a second kind.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KindSelector(kind: LogKind, modifier: Modifier = Modifier, onSelect: (LogKind) -> Unit = {}) {
    val enabled = LogKind.entries.size > 1
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = kind.label,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Kind") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).testTag("log_kind_selector"),
        )
        DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            for (option in LogKind.entries) {
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    modifier = Modifier.testTag("log_kind_option_${option.name.lowercase()}"),
                )
            }
        }
    }
}

/**
 * The vehicle to log for, when the form was opened without one: Material 3's own dropdown pattern
 * ([ExposedDropdownMenuBox], [DropdownMenuItem], the trailing icon Material gives it), so its expansion, focus, keyboard and
 * accessibility behavior and its colors are Material's, none of it re-implemented here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VehicleSelector(vehicles: List<VehicleChoice>, selected: VehicleChoice?, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Vehicle") },
            leadingIcon = { VehicleChoiceIcon(selected) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).testTag("log_vehicle_selector"),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (vehicle in vehicles) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(vehicle.name)
                            vehicle.licensePlate?.let { SubtleText(it) }
                        }
                    },
                    leadingIcon = { VehicleChoiceIcon(vehicle) },
                    onClick = {
                        onSelect(vehicle.id)
                        expanded = false
                    },
                    modifier = Modifier.testTag("log_vehicle_option_${vehicle.id}"),
                )
            }
        }
    }
}

@Composable
internal fun VehicleChoiceIcon(vehicle: VehicleChoice?) {
    VehiclePicture(vehicle?.pictureUri, vehicle?.type, vehicle?.color ?: com.mikonoma.drivinglog.vehicle.domain.VehicleColors.default, Modifier.size(24.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WayChoice(selected: LogWay, onSelect: (LogWay) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = selected == LogWay.TRIP_DISTANCE,
            onClick = { onSelect(LogWay.TRIP_DISTANCE) },
            shape = SegmentedButtonDefaults.itemShape(0, 2),
            modifier = Modifier.testTag("log_way_distance"),
        ) { Text("Trip distance") }
        SegmentedButton(
            selected = selected == LogWay.NEW_ODOMETER,
            onClick = { onSelect(LogWay.NEW_ODOMETER) },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
            modifier = Modifier.testTag("log_way_odometer"),
        ) { Text("New odometer") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnitChoice(state: LogEventState, onIntent: (LogEventIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Unit", style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !state.unit.isMiles,
                onClick = { onIntent(LogEventIntent.UnitFamilySelected(miles = false)) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                modifier = Modifier.testTag("log_unit_km"),
            ) { Text("Kilometers") }
            SegmentedButton(
                selected = state.unit.isMiles,
                onClick = { onIntent(LogEventIntent.UnitFamilySelected(miles = true)) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                modifier = Modifier.testTag("log_unit_mi"),
            ) { Text("Miles") }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Include tenths")
            Switch(
                checked = state.unit.hasTenths,
                onCheckedChange = { onIntent(LogEventIntent.TenthsChanged(it)) },
                modifier = Modifier.testTag("log_tenths"),
            )
        }
    }
}

/** Liters or gallons (`add-refueling-logging`): the same segmented-button pattern [UnitChoice] uses, without a
 * tenths switch — a fuel amount is always two decimal places, whatever unit is chosen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FuelUnitChoice(selected: FuelUnit, onSelect: (FuelUnit) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Unit", style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selected == FuelUnit.LITERS,
                onClick = { onSelect(FuelUnit.LITERS) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                modifier = Modifier.testTag("fuel_unit_liters"),
            ) { Text("Liters") }
            SegmentedButton(
                selected = selected == FuelUnit.GALLONS,
                onClick = { onSelect(FuelUnit.GALLONS) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                modifier = Modifier.testTag("fuel_unit_gallons"),
            ) { Text("Gallons") }
        }
    }
}

/** The fuel-type list, narrowed to the current vehicle's own fuel type (`vehicle-fuel-type`): Material 3's own
 * dropdown pattern, like [KindSelector]/[VehicleSelector]. [allowed] is rendered in [FuelType.entries]' own order. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FuelTypeSelector(selected: FuelType, allowed: Set<FuelType>, onSelect: (FuelType) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Fuel type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).testTag("fuel_type_selector"),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (option in FuelType.entries) {
                if (option !in allowed) continue
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    modifier = Modifier.testTag("fuel_type_option_${option.name.lowercase()}"),
                )
            }
        }
    }
}

/** The "filled up" checkbox (`add-refueling-logging`), checked by default. */
@Composable
internal fun FilledUpChoice(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text("Filled up")
        Checkbox(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag("filled_up"))
    }
}
