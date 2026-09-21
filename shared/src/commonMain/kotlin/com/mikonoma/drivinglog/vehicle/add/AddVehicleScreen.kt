package com.mikonoma.drivinglog.vehicle.add

import com.mikonoma.drivinglog.ui.color.rememberAnimatedColor
import com.mikonoma.drivinglog.vehicle.color.VehicleColorChoice
import com.mikonoma.drivinglog.vehicle.type.VehicleTypeChoice
import com.mikonoma.drivinglog.ui.theme.headerTextButtonColors
import com.mikonoma.drivinglog.ui.theme.drivingLogTopAppBarColors
import com.mikonoma.drivinglog.ui.theme.HeaderDivider
import com.mikonoma.drivinglog.ui.ScreenBottomSpace
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.mikonoma.drivinglog.vehicle.picture.PictureField
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
        // Leaving without saving deletes the pending picture files; a rotation is not leaving and keeps them.
        onBack = {
            processor.dispatch(AddVehicleIntent.Left)
            onBack()
        },
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
    // One animated color for the whole screen: everything drawn from the vehicle's color takes it from here, so it all moves together.
    val animatedColor = rememberAnimatedColor(state.color)
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                TopAppBar(
                    colors = drivingLogTopAppBarColors(),
                    title = { Text("Add vehicle") },
                    navigationIcon = { BackButton(onBack) },
                    actions = {
                        TextButton(
                            colors = headerTextButtonColors(),
                            onClick = { onIntent(AddVehicleIntent.Save) },
                            enabled = !state.isSaving,
                            modifier = Modifier.testTag("save_vehicle"),
                        ) { Text("Save") }
                    },
                )
                HeaderDivider()
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + ScreenBottomSpace),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PictureField(
                picture = state.picture,
                previewUri = state.previewUri,
                type = state.type,
                color = animatedColor,
                cropImage = state.cropImage,
                onPhotoPicked = { onIntent(AddVehicleIntent.PhotoPicked(it)) },
                onCropConfirmed = { crop, turns -> onIntent(AddVehicleIntent.CropConfirmed(crop, turns)) },
                onCropCancelled = { onIntent(AddVehicleIntent.CropCancelled) },
                onRemove = { onIntent(AddVehicleIntent.PictureRemoved) },
                onRefresh = { onIntent(AddVehicleIntent.PictureRefresh) },
            )
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
            VehicleTypeChoice(
                selected = state.type,
                onSelect = { onIntent(AddVehicleIntent.TypeSelected(it)) },
                color = animatedColor,
            )
            VehicleColorChoice(
                color = state.color,
                pictureColor = state.pictureColor,
                savedColor = null,
                onSelect = { onIntent(AddVehicleIntent.ColorSelected(it)) },
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
                isError = state.odometerError,
                errorText = "Enter the odometer reading",
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
