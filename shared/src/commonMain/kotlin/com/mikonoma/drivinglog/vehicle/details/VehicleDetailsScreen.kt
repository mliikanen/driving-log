package com.mikonoma.drivinglog.vehicle.details

import com.mikonoma.drivinglog.ui.theme.drivingLogTopAppBarColors
import com.mikonoma.drivinglog.ui.theme.HeaderDivider
import com.mikonoma.drivinglog.ui.ScreenBottomSpace
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
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
import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone
import com.mikonoma.drivinglog.ui.BackButton
import com.mikonoma.drivinglog.ui.VehiclePicture
import com.mikonoma.drivinglog.ui.color.rememberAnimatedColor
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.format.formatOdometer
import com.mikonoma.drivinglog.vehicle.ui.EventRow

@Composable
fun VehicleDetailsScreen(
    processor: VehicleDetailsProcessor,
    deviceLocale: DeviceLocale,
    deviceTimeZone: DeviceTimeZone,
    onShowEdit: (String) -> Unit,
    onShowLog: (String) -> Unit,
    onShowLogEvent: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by processor.states.collectAsState()

    LaunchedEffect(processor) {
        processor.sideEffects.collect { effect ->
            when (effect) {
                is VehicleDetailsEffect.ShowEdit -> onShowEdit(effect.vehicleId)
                is VehicleDetailsEffect.ShowLog -> onShowLog(effect.vehicleId)
                is VehicleDetailsEffect.ShowLogEvent -> onShowLogEvent(effect.vehicleId)
            }
        }
    }

    VehicleDetailsContent(
        state = state,
        deviceLocale = deviceLocale,
        deviceTimeZone = deviceTimeZone,
        onEdit = { processor.dispatch(VehicleDetailsIntent.EditClicked) },
        onViewLog = { processor.dispatch(VehicleDetailsIntent.ViewLogClicked) },
        onLogEvent = { processor.dispatch(VehicleDetailsIntent.LogEventClicked) },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailsContent(
    state: VehicleDetailsState,
    deviceLocale: DeviceLocale,
    deviceTimeZone: DeviceTimeZone,
    onEdit: () -> Unit,
    onViewLog: () -> Unit,
    onLogEvent: () -> Unit,
    onBack: () -> Unit,
) {
    // Read on every composition so a change of device locale shows the new separators.
    val symbols = deviceLocale.numberSymbols()
    val deviceZone = deviceTimeZone.current()
    val timeFormat = deviceLocale.timeFormat()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                TopAppBar(
                    colors = drivingLogTopAppBarColors(),
                    title = { Text(state.name, modifier = Modifier.testTag("vehicle_title")) },
                    navigationIcon = { BackButton(onBack) },
                    actions = {
                        IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_vehicle")) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit vehicle")
                        }
                    },
                )
                HeaderDivider()
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> Unit
                state.notFound -> Text("This vehicle no longer exists.", Modifier.padding(16.dp))
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + ScreenBottomSpace),
                ) {
                    item {
                        // One animated color for the screen, remembered once the vehicle has loaded, so it opens in the vehicle's color.
                        val animatedColor = rememberAnimatedColor(state.color)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Large, but not the whole screen: the odometer and the actions stay near the top.
                            Box(Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
                                VehiclePicture(
                                    state.pictureUri,
                                    state.type,
                                    animatedColor,
                                    Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(1f).testTag("vehicle_picture_large"),
                                    contentDescription = "Picture of ${state.name}",
                                )
                            }
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
                            Button(
                                onClick = onLogEvent,
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag("log_event"),
                            ) { Text("Log event") }
                            Text(
                                "Recent activity",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                            )
                        }
                    }
                    items(state.recentEvents, key = { it.id }) { event ->
                        EventRow(event, state.unit, symbols, deviceZone, timeFormat, Modifier.testTag("recent_event"))
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
