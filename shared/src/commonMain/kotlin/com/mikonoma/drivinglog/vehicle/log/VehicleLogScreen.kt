package com.mikonoma.drivinglog.vehicle.log

import androidx.compose.foundation.layout.Column
import com.mikonoma.drivinglog.ui.theme.drivingLogTopAppBarColors
import com.mikonoma.drivinglog.ui.theme.HeaderDivider
import com.mikonoma.drivinglog.ui.ScreenBottomSpace
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mikonoma.drivinglog.locale.DeviceLocale
import com.mikonoma.drivinglog.ui.BackButton
import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone
import com.mikonoma.drivinglog.vehicle.ui.EventRow

@Composable
fun VehicleLogScreen(processor: VehicleLogProcessor, deviceLocale: DeviceLocale, deviceTimeZone: DeviceTimeZone, onBack: () -> Unit) {
    val state by processor.states.collectAsState()
    VehicleLogContent(state, deviceLocale, deviceTimeZone, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleLogContent(state: VehicleLogState, deviceLocale: DeviceLocale, deviceTimeZone: DeviceTimeZone, onBack: () -> Unit) {
    val symbols = deviceLocale.numberSymbols()
    val deviceZone = deviceTimeZone.current()
    val timeFormat = deviceLocale.timeFormat()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                TopAppBar(
                    colors = drivingLogTopAppBarColors(),
                    title = { Text(if (state.vehicleName.isEmpty()) "Log" else "${state.vehicleName} log") },
                    navigationIcon = { BackButton(onBack) },
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
                    Modifier.fillMaxSize().testTag("log_list"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = ScreenBottomSpace),
                ) {
                    items(state.events, key = { it.id }) { event ->
                        EventRow(event, state.unit, symbols, deviceZone, timeFormat, Modifier.testTag("log_event"))
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
