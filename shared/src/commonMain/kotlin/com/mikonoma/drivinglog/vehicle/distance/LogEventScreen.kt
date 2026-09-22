package com.mikonoma.drivinglog.vehicle.distance

import com.mikonoma.drivinglog.ui.theme.headerTextButtonColors
import com.mikonoma.drivinglog.ui.theme.drivingLogTopAppBarColors
import com.mikonoma.drivinglog.ui.theme.HeaderDivider
import com.mikonoma.drivinglog.ui.ScreenBottomSpace
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mikonoma.drivinglog.locale.DeviceLocale
import com.mikonoma.drivinglog.ui.BackButton
import com.mikonoma.drivinglog.ui.OdometerField
import com.mikonoma.drivinglog.ui.VehiclePicture
import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone
import com.mikonoma.drivinglog.vehicle.format.formatOdometer
import com.mikonoma.drivinglog.vehicle.format.formatTimeOfDay
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

@Composable
fun LogEventScreen(
    processor: LogEventProcessor,
    deviceLocale: DeviceLocale,
    deviceTimeZone: DeviceTimeZone,
    onBack: () -> Unit,
) {
    val state by processor.states.collectAsState()

    LaunchedEffect(processor) {
        processor.sideEffects.collect { effect ->
            when (effect) {
                LogEventEffect.Saved -> onBack()
            }
        }
    }

    LogEventContent(
        state = state,
        deviceLocale = deviceLocale,
        deviceTimeZoneId = deviceTimeZone.current().id,
        onIntent = processor::dispatch,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogEventContent(
    state: LogEventState,
    deviceLocale: DeviceLocale,
    deviceTimeZoneId: String,
    onIntent: (LogEventIntent) -> Unit,
    onBack: () -> Unit,
) {
    // Read on every composition so a change of device locale shows the new separators.
    val symbols = deviceLocale.numberSymbols()
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var showZone by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                TopAppBar(
                    colors = drivingLogTopAppBarColors(),
                    title = { Text("Log event") },
                    navigationIcon = { BackButton(onBack) },
                    actions = {
                        TextButton(
                            colors = headerTextButtonColors(),
                            onClick = { onIntent(LogEventIntent.Save) },
                            enabled = !state.isLoading && !state.notFound && !state.isSaving,
                            modifier = Modifier.testTag("save_entry"),
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
            when {
                state.isLoading -> Unit
                state.notFound -> Text("This vehicle no longer exists.")
                else -> {
                    // The Kind selector is always shown; the vehicle selector only when the form was opened without a vehicle (from the Home
                    // screen — a vehicle's details screen fixes it and shows no selector). With both, they share one row, split in half.
                    if (state.vehicles.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KindSelector(state.kind, Modifier.weight(1f)) { onIntent(LogEventIntent.KindSelected(it)) }
                            VehicleSelector(state.vehicles, state.selectedVehicle, Modifier.weight(1f)) { onIntent(LogEventIntent.VehicleSelected(it)) }
                        }
                    } else {
                        KindSelector(state.kind, Modifier.fillMaxWidth()) { onIntent(LogEventIntent.KindSelected(it)) }
                    }
                    WayChoice(state.way) { onIntent(LogEventIntent.WayChanged(it)) }
                    MomentRow(state, deviceLocale, onDate = { showDate = true }, onTime = { showTime = true }, onZone = { showZone = true })
                    if (state.error is LogDistanceError.TimeInFuture) ErrorText(errorMessage(state, symbols))
                    UnitChoice(state, onIntent)
                    if (state.way == LogWay.NEW_ODOMETER) KnownOdometerInfo(state, symbols)
                    OdometerField(
                        entry = state.activeEntry,
                        symbols = symbols,
                        onEdit = { onIntent(LogEventIntent.OdometerEdited(it)) },
                        onClear = { onIntent(LogEventIntent.OdometerCleared) },
                        label = if (state.way == LogWay.TRIP_DISTANCE) "Trip distance" else "New odometer",
                        isError = state.error != null && state.error !is LogDistanceError.TimeInFuture,
                        errorText = state.error?.takeIf { it !is LogDistanceError.TimeInFuture }?.let { errorMessage(state, symbols) },
                    )
                }
            }
        }
    }

    if (showDate) {
        DateDialog(
            initialDateMillis = dateToPicker(state.localDateTime.date),
            onDismiss = { showDate = false },
            onPicked = { onIntent(LogEventIntent.DateChanged(dateFromPicker(it))) },
        )
    }
    if (showTime) {
        TimeDialog(
            hour = state.localDateTime.hour,
            minute = state.localDateTime.minute,
            is24Hour = deviceLocale.timeFormat().is24Hour,
            onDismiss = { showTime = false },
            onPicked = { hour, minute -> onIntent(LogEventIntent.TimeChanged(hour, minute)) },
        )
    }
    if (showZone) {
        TimeZoneDialog(
            at = state.moment.instant,
            deviceZoneId = deviceTimeZoneId,
            selectedId = state.zoneId,
            onDismiss = { showZone = false },
            onPicked = { onIntent(LogEventIntent.ZoneChanged(it)) },
        )
    }
}

/**
 * The kind of event being logged: Material 3's own dropdown pattern, like [VehicleSelector], with one item, "Distance". [enabled]
 * is `false` while [LogKind] has one entry, so the field's own disabled colors, semantics and blocked tap are Material's disabled
 * treatment (the same one the Home screen's not-yet-available tiles use), and none of it is written here; it becomes a real choice
 * once a later change adds a second kind.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KindSelector(kind: LogKind, modifier: Modifier = Modifier, onSelect: (LogKind) -> Unit = {}) {
    val enabled = LogKind.entries.size > 1
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = kind.label,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Kind") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable).testTag("log_kind_selector"),
        )
        DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            for (option in LogKind.entries) {
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = { onSelect(option); expanded = false },
                    modifier = Modifier.testTag("log_kind_option_${option.name.lowercase()}"),
                )
            }
        }
    }
}

/**
 * The vehicle to log for, when the form was opened without one: Material 3's own dropdown pattern
 * ([ExposedDropdownMenuBox], [DropdownMenuItem], the trailing icon Material gives it), so its expansion, focus, keyboard and
 * accessibility behavior and its colors are Material's, none of it re-implemented here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleSelector(vehicles: List<VehicleChoice>, selected: VehicleChoice?, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Vehicle") },
            leadingIcon = { VehicleChoiceIcon(selected) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable).testTag("log_vehicle_selector"),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (vehicle in vehicles) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(vehicle.name)
                            vehicle.licensePlate?.let { SubtleText(it) }
                        }
                    },
                    leadingIcon = { VehicleChoiceIcon(vehicle) },
                    onClick = { onSelect(vehicle.id); expanded = false },
                    modifier = Modifier.testTag("log_vehicle_option_${vehicle.id}"),
                )
            }
        }
    }
}

@Composable
private fun VehicleChoiceIcon(vehicle: VehicleChoice?) {
    VehiclePicture(vehicle?.pictureUri, vehicle?.type, vehicle?.color ?: com.mikonoma.drivinglog.vehicle.domain.VehicleColors.default, Modifier.size(24.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WayChoice(selected: LogWay, onSelect: (LogWay) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = selected == LogWay.TRIP_DISTANCE,
            onClick = { onSelect(LogWay.TRIP_DISTANCE) },
            shape = SegmentedButtonDefaults.itemShape(0, 2),
            modifier = Modifier.testTag("log_way_distance"),
        ) { Text("Trip distance") }
        SegmentedButton(
            selected = selected == LogWay.NEW_ODOMETER,
            onClick = { onSelect(LogWay.NEW_ODOMETER) },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
            modifier = Modifier.testTag("log_way_odometer"),
        ) { Text("New odometer") }
    }
}

@Composable
private fun MomentRow(
    state: LogEventState,
    deviceLocale: DeviceLocale,
    onDate: () -> Unit,
    onTime: () -> Unit,
    onZone: () -> Unit,
) {
    val local = state.localDateTime
    val offset = state.moment.zone?.offsetSeconds ?: 0
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Date, time and time zone", style = MaterialTheme.typography.titleSmall)
        // Wraps only when the three buttons do not fit on one row; all share one height either way.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MomentButton(onDate, "log_date") {
                Text(local.date.toString())
                SubtleText(deviceLocale.weekdayName(local.date.dayOfWeek))
            }
            MomentButton(onTime, "log_time") {
                Text(formatTimeOfDay(local.hour, local.minute, deviceLocale.timeFormat()))
            }
            MomentButton(onZone, "log_zone") {
                Text(state.zoneId)
                SubtleText(formatUtcOffset(offset))
            }
        }
    }
}

private val MomentButtonHeight = 64.dp

@Composable
private fun MomentButton(onClick: () -> Unit, testTag: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.height(MomentButtonHeight).testTag(testTag)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitChoice(state: LogEventState, onIntent: (LogEventIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Unit", style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !state.unit.isMiles,
                onClick = { onIntent(LogEventIntent.UnitFamilySelected(miles = false)) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                modifier = Modifier.testTag("log_unit_km"),
            ) { Text("Kilometers") }
            SegmentedButton(
                selected = state.unit.isMiles,
                onClick = { onIntent(LogEventIntent.UnitFamilySelected(miles = true)) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                modifier = Modifier.testTag("log_unit_mi"),
            ) { Text("Miles") }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Include tenths")
            Switch(
                checked = state.unit.hasTenths,
                onCheckedChange = { onIntent(LogEventIntent.TenthsChanged(it)) },
                modifier = Modifier.testTag("log_tenths"),
            )
        }
    }
}

@Composable
private fun KnownOdometerInfo(state: LogEventState, symbols: com.mikonoma.drivinglog.locale.NumberSymbols) {
    val known = state.knownOdometer
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (known == null) {
            // Not an error: the count is saved as an odometer anchor, a new starting point for the odometer.
            Text(
                "No odometer is known at this time. The count will be saved as a new odometer starting point.",
                modifier = Modifier.testTag("log_no_known_odometer"),
            )
        } else {
            Text(
                "Previous known odometer: " + formatOdometer(known, state.vehicleUnit, symbols),
                modifier = Modifier.testTag("log_known_odometer"),
            )
            state.previewDistance?.let { distance ->
                Text("Distance: " + formatOdometer(distance, state.vehicleUnit, symbols), modifier = Modifier.testTag("log_live_distance"))
            }
        }
    }
}

/** A secondary line under a button's main text: smaller and lighter, so the main text stays the first thing read. */
@Composable
private fun SubtleText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    )
}

@Composable
private fun ErrorText(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("log_error"))
}

private fun errorMessage(state: LogEventState, symbols: com.mikonoma.drivinglog.locale.NumberSymbols): String =
    when (val error = state.error) {
        null -> ""
        LogDistanceError.FieldEmpty ->
            if (state.way == LogWay.TRIP_DISTANCE) "Enter the trip distance" else "Enter the odometer reading"
        LogDistanceError.TimeInFuture -> "The time cannot be in the future"
        LogDistanceError.DistanceNotPositive -> "The distance must be more than zero"
        is LogDistanceError.OdometerNotHigher -> "Enter a reading higher than " + formatOdometer(error.known, state.vehicleUnit, symbols)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(initialDateMillis: Long, onDismiss: () -> Unit, onPicked: (Long) -> Unit) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let(onPicked)
                    onDismiss()
                },
                modifier = Modifier.testTag("date_ok"),
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(pickerState) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(hour: Int, minute: Int, is24Hour: Boolean, onDismiss: () -> Unit, onPicked: (Int, Int) -> Unit) {
    val pickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = is24Hour)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onPicked(pickerState.hour, pickerState.minute)
                    onDismiss()
                },
                modifier = Modifier.testTag("time_ok"),
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { TimePicker(pickerState) } },
    )
}

@Composable
private fun TimeZoneDialog(
    at: Instant,
    deviceZoneId: String,
    selectedId: String,
    onDismiss: () -> Unit,
    onPicked: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val choices = remember(query, at, deviceZoneId) { timeZoneChoices(TimeZone.availableZoneIds, deviceZoneId, at, query) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Time zone") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().testTag("zone_search"),
                    label = { Text("Search") },
                    singleLine = true,
                )
                if (choices.isEmpty()) Text("No time zone matches")
                LazyColumn(Modifier.heightIn(max = 360.dp).testTag("zone_list")) {
                    items(choices, key = { it.id }) { choice ->
                        ListItem(
                            headlineContent = { Text(choice.id) },
                            supportingContent = { SubtleText(formatUtcOffset(choice.offsetSeconds)) },
                            trailingContent = if (choice.id == selectedId) {
                                { Icon(Icons.Filled.Check, contentDescription = "Selected") }
                            } else null,
                            modifier = Modifier
                                .clickable {
                                    onPicked(choice.id)
                                    onDismiss()
                                }
                                .testTag("zone_item"),
                        )
                    }
                }
            }
        },
    )
}
