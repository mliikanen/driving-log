package com.mikonoma.drivinglog.vehicle.edit

import com.mikonoma.drivinglog.ui.ScreenBottomSpace
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.mikonoma.drivinglog.vehicle.picture.PictureField

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

    EditVehicleContent(
        state = state,
        onIntent = processor::dispatch,
        // Leaving without saving deletes the pending picture files; a rotation is not leaving and keeps them.
        onBack = {
            processor.dispatch(EditVehicleIntent.Left)
            onBack()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVehicleContent(
    state: EditVehicleState,
    onIntent: (EditVehicleIntent) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
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
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + ScreenBottomSpace),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.notFound) {
                Text("This vehicle no longer exists.")
            } else if (state.loaded) {
                PictureField(
                    picture = state.picture,
                    previewUri = state.previewUri,
                    cropImage = state.cropImage,
                    onPhotoPicked = { onIntent(EditVehicleIntent.PhotoPicked(it)) },
                    onCropConfirmed = { onIntent(EditVehicleIntent.CropConfirmed(it)) },
                    onCropCancelled = { onIntent(EditVehicleIntent.CropCancelled) },
                    onRemove = { onIntent(EditVehicleIntent.PictureRemoved) },
                    onRefresh = { onIntent(EditVehicleIntent.PictureRefresh) },
                )
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
