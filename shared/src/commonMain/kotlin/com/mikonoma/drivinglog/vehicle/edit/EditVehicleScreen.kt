package com.mikonoma.drivinglog.vehicle.edit

import com.mikonoma.drivinglog.ui.color.rememberAnimatedColor
import com.mikonoma.drivinglog.vehicle.color.VehicleColorChoice
import com.mikonoma.drivinglog.vehicle.domain.VehicleColors
import com.mikonoma.drivinglog.vehicle.type.VehicleTypeChoice
import com.mikonoma.drivinglog.ui.theme.headerTextButtonColors
import com.mikonoma.drivinglog.ui.theme.drivingLogTopAppBarColors
import com.mikonoma.drivinglog.ui.theme.HeaderDivider
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
import com.mikonoma.drivinglog.ui.CloseButton
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
            Column {
                TopAppBar(
                    colors = drivingLogTopAppBarColors(),
                    title = { Text("Edit vehicle") },
                    navigationIcon = { CloseButton(onBack) },
                    actions = {
                        TextButton(
                            colors = headerTextButtonColors(),
                            onClick = { onIntent(EditVehicleIntent.Save) },
                            enabled = state.loaded && !state.notFound && !state.isSaving,
                            modifier = Modifier.testTag("save_vehicle"),
                        ) { Text("Save") }
                    },
                )
                HeaderDivider()
            }
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
                // One animated color for the form: everything drawn from the vehicle's color takes it from here. It is remembered inside the loaded branch, so the
                // form opens in the saved color instead of animating from a placeholder.
                val color = state.color ?: VehicleColors.default
                val animatedColor = rememberAnimatedColor(color)
                PictureField(
                    picture = state.picture,
                    previewUri = state.previewUri,
                    type = state.type,
                    color = animatedColor,
                    cropImage = state.cropImage,
                    onPhotoPicked = { onIntent(EditVehicleIntent.PhotoPicked(it)) },
                    onCropConfirmed = { crop, turns -> onIntent(EditVehicleIntent.CropConfirmed(crop, turns)) },
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
                VehicleTypeChoice(
                    selected = state.type,
                    onSelect = { onIntent(EditVehicleIntent.TypeSelected(it)) },
                    color = animatedColor,
                )
                VehicleColorChoice(
                    color = color,
                    pictureColor = state.pictureColor,
                    savedColor = state.savedColor,
                    onSelect = { onIntent(EditVehicleIntent.ColorSelected(it)) },
                )
            }
        }
    }
}
