package com.mikonoma.drivinglog.vehicle.fueltype

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mikonoma.drivinglog.vehicle.domain.VehicleFuelType

/**
 * The choice of a vehicle's fuel type (`vehicle-fuel-type`), used by the add and edit screens: a plain selectable
 * list, in the order of [VehicleFuelType.entries]. [selected] is null only on the edit screen until the saved
 * vehicle has loaded, when nothing is marked. There is no choice for "none": a vehicle always has a fuel type.
 */
@Composable
fun VehicleFuelTypeChoice(selected: VehicleFuelType?, onSelect: (VehicleFuelType) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.selectableGroup().testTag("vehicle_fuel_type_choice")) {
        Text("Vehicle fuel type", style = MaterialTheme.typography.titleSmall)
        for (type in VehicleFuelType.entries) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = type == selected, onClick = { onSelect(type) }, role = Role.RadioButton)
                    .padding(vertical = 4.dp)
                    .testTag("vehicle_fuel_type_${type.code}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = type == selected, onClick = null)
                Text(type.label, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}
