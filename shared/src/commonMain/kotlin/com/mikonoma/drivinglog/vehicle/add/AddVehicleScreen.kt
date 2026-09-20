package com.mikonoma.drivinglog.vehicle.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mikonoma.drivinglog.locale.DeviceLocale
import com.mikonoma.drivinglog.ui.BackButton
import com.mikonoma.drivinglog.ui.OdometerField
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.ui.label

@Composable
fun AddVehicleScreen(
    processor: AddVehicleProcessor,
    deviceLocale: DeviceLocale,
    onBack: () -> Unit,
) {
    val state by processor.states.collectAsState()

    LaunchedEffect(processor) {
        processor.sideEffects.collect { effect ->
            when (effect) {
                AddVehicleEffect.Saved -> onBack()
            }
        }
    }

    AddVehicleContent(
        state = state,
        deviceLocale = deviceLocale,
        onIntent = processor::dispatch,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleContent(
    state: AddVehicleState,
    deviceLocale: DeviceLocale,
    onIntent: (AddVehicleIntent) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add vehicle") },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    TextButton(
                        onClick = { onIntent(AddVehicleIntent.Save) },
                        enabled = !state.isSaving,
                        modifier = Modifier.testTag("save_vehicle"),
                    ) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { onIntent(AddVehicleIntent.NameChanged(it)) },
                modifier = Modifier.fillMaxWidth().testTag("vehicle_name"),
                label = { Text("Name") },
                singleLine = true,
                isError = state.nameError,
                supportingText = if (state.nameError) ({ Text("Enter a name") }) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                value = state.licensePlate,
                onValueChange = { onIntent(AddVehicleIntent.LicensePlateChanged(it)) },
                modifier = Modifier.fillMaxWidth().testTag("vehicle_plate"),
                label = { Text("License plate (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            UnitChoice(
                selected = state.entry.unit,
                onSelect = { onIntent(AddVehicleIntent.UnitSelected(it)) },
            )
            OdometerField(
                entry = state.entry,
                symbols = deviceLocale.numberSymbols(),
                onEdit = { onIntent(AddVehicleIntent.OdometerEdited(it)) },
                onClear = { onIntent(AddVehicleIntent.OdometerCleared) },
                label = "Current odometer",
            )
        }
    }
}

@Composable
private fun UnitChoice(selected: OdometerUnit, onSelect: (OdometerUnit) -> Unit) {
    Column(Modifier.selectableGroup().testTag("unit_choice")) {
        Text("Odometer unit", style = MaterialTheme.typography.titleSmall)
        for (unit in OdometerUnit.entries) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = unit == selected, onClick = { onSelect(unit) }, role = Role.RadioButton)
                    .padding(vertical = 4.dp)
                    .testTag("unit_${unit.code}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = unit == selected, onClick = null)
                Text(unit.label, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}
