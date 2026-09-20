package com.mikonoma.drivinglog.vehicle.distance

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.mikonoma.drivinglog.locale.DeviceLocale
import com.mikonoma.drivinglog.ui.BackButton
import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone

/** Placeholder until the real form is built (task 4.1). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDistanceScreen(
    processor: LogDistanceProcessor,
    deviceLocale: DeviceLocale,
    deviceTimeZone: DeviceTimeZone,
    onBack: () -> Unit,
) {
    LaunchedEffect(processor) {
        processor.sideEffects.collect { effect ->
            when (effect) {
                LogDistanceEffect.Saved -> onBack()
            }
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Log distance") }, navigationIcon = { BackButton(onBack) }) },
    ) { padding -> Text("Log distance form", Modifier.padding(padding).padding(16.dp)) }
}
