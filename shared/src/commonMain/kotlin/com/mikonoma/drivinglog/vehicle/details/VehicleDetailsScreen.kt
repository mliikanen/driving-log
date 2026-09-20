package com.mikonoma.drivinglog.vehicle.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mikonoma.drivinglog.locale.DeviceLocale
import com.mikonoma.drivinglog.ui.BackButton
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.format.formatDateTime
import com.mikonoma.drivinglog.vehicle.format.formatOdometer
import com.mikonoma.drivinglog.vehicle.ui.label

@Composable
fun VehicleDetailsScreen(
    processor: VehicleDetailsProcessor,
    deviceLocale: DeviceLocale,
    onShowEdit: (String) -> Unit,
    onShowLog: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by processor.states.collectAsState()

    LaunchedEffect(processor) {
        processor.sideEffects.collect { effect ->
            when (effect) {
                is VehicleDetailsEffect.ShowEdit -> onShowEdit(effect.vehicleId)
                is VehicleDetailsEffect.ShowLog -> onShowLog(effect.vehicleId)
            }
        }
    }

    VehicleDetailsContent(
        state = state,
        deviceLocale = deviceLocale,
        onEdit = { processor.dispatch(VehicleDetailsIntent.EditClicked) },
        onViewLog = { processor.dispatch(VehicleDetailsIntent.ViewLogClicked) },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailsContent(
    state: VehicleDetailsState,
    deviceLocale: DeviceLocale,
    onEdit: () -> Unit,
    onViewLog: () -> Unit,
    onBack: () -> Unit,
) {
    // Read on every composition so a change of device locale shows the new separators.
    val symbols = deviceLocale.numberSymbols()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.name, modifier = Modifier.testTag("vehicle_title")) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_vehicle")) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit vehicle")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> Unit
                state.notFound -> Text("This vehicle no longer exists.", Modifier.padding(16.dp))
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            state.licensePlate?.let { plate ->
                                Text("License plate", style = MaterialTheme.typography.labelMedium)
                                Text(plate, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("vehicle_plate"))
                            }
                            Text("Current odometer", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
                            Text(
                                formatOdometer(state.currentOdometer ?: Distance.ZERO, state.unit, symbols),
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.testTag("current_odometer"),
                            )
                            Text(
                                "Recent activity",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                            )
                        }
                    }
                    items(state.recentEvents, key = { it.id }) { event ->
                        ListItem(
                            headlineContent = { Text(event.label) },
                            supportingContent = { Text(formatDateTime(event.occurredAt)) },
                            trailingContent = event.odometer?.let { reading ->
                                { Text(formatOdometer(reading, state.unit, symbols)) }
                            },
                            modifier = Modifier.testTag("recent_event"),
                        )
                        HorizontalDivider()
                    }
                    item {
                        OutlinedButton(
                            onClick = onViewLog,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag("view_log"),
                        ) { Text("View full log") }
                    }
                }
            }
        }
    }
}
