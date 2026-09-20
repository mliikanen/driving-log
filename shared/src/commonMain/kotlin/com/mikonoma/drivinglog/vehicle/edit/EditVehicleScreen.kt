package com.mikonoma.drivinglog.vehicle.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mikonoma.drivinglog.ui.BackButton

@Composable
fun EditVehicleScreen(processor: EditVehicleProcessor, onBack: () -> Unit) {
    val state by processor.states.collectAsState()

    LaunchedEffect(processor) {
        processor.sideEffects.collect { effect ->
            when (effect) {
                EditVehicleEffect.Saved -> onBack()
            }
        }
    }

    EditVehicleContent(state = state, onIntent = processor::dispatch, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVehicleContent(
    state: EditVehicleState,
    onIntent: (EditVehicleIntent) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit vehicle") },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    TextButton(
                        onClick = { onIntent(EditVehicleIntent.Save) },
                        enabled = state.loaded && !state.notFound && !state.isSaving,
                        modifier = Modifier.testTag("save_vehicle"),
                    ) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.notFound) {
                Text("This vehicle no longer exists.")
            } else if (state.loaded) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onIntent(EditVehicleIntent.NameChanged(it)) },
                    modifier = Modifier.fillMaxWidth().testTag("vehicle_name"),
                    label = { Text("Name") },
                    singleLine = true,
                    isError = state.nameError,
                    supportingText = if (state.nameError) ({ Text("Enter a name") }) else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = state.licensePlate,
                    onValueChange = { onIntent(EditVehicleIntent.LicensePlateChanged(it)) },
                    modifier = Modifier.fillMaxWidth().testTag("vehicle_plate"),
                    label = { Text("License plate (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
            }
        }
    }
}
