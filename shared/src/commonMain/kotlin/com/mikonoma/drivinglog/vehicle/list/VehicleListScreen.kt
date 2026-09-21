package com.mikonoma.drivinglog.vehicle.list

import com.mikonoma.drivinglog.ui.BackButton
import com.mikonoma.drivinglog.ui.theme.drivingLogTopAppBarColors
import com.mikonoma.drivinglog.ui.theme.HeaderDivider
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mikonoma.drivinglog.ui.VehiclePicture
import com.mikonoma.drivinglog.ui.color.rememberAnimatedColor

@Composable
fun VehicleListScreen(
    processor: VehicleListProcessor,
    onShowDetails: (String) -> Unit,
    onShowAdd: () -> Unit,
    onBack: () -> Unit,
) {
    val state by processor.states.collectAsState()

    LaunchedEffect(processor) {
        processor.sideEffects.collect { effect ->
            when (effect) {
                is VehicleListEffect.ShowDetails -> onShowDetails(effect.id)
                VehicleListEffect.ShowAdd -> onShowAdd()
            }
        }
    }

    VehicleListContent(
        state = state,
        onOpen = { processor.dispatch(VehicleListIntent.OpenVehicle(it)) },
        onAdd = { processor.dispatch(VehicleListIntent.AddVehicle) },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListContent(
    state: VehicleListState,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Vehicles") },
                    navigationIcon = { BackButton(onBack) },
                    colors = drivingLogTopAppBarColors(),
                )
                HeaderDivider()
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add vehicle") },
                modifier = Modifier.testTag("add_vehicle"),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> Unit // avoid flashing the empty state before the first load
                state.vehicles.isEmpty() -> EmptyState(Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("vehicle_list"),
                    // Room below the last item for the floating action button.
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    items(state.vehicles, key = { it.id }) { vehicle ->
                        // One animated color per row: the icon is drawn from it, and a saved change of the color reaches the row as one animation.
                        val animatedColor = rememberAnimatedColor(vehicle.color)
                        Column {
                            ListItem(
                                headlineContent = { Text(vehicle.name) },
                                supportingContent = vehicle.licensePlate?.let { plate -> { Text(plate) } },
                                leadingContent = {
                                    VehiclePicture(vehicle.pictureUri, vehicle.type, animatedColor, Modifier.size(56.dp).testTag("vehicle_picture"))
                                },
                                modifier = Modifier.clickable { onOpen(vehicle.id) }.testTag("vehicle_item"),
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp).testTag("empty_state"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No vehicles yet", style = MaterialTheme.typography.titleMedium)
        Text("Add your first vehicle to start logging.", style = MaterialTheme.typography.bodyMedium)
    }
}
