package com.mikonoma.drivinglog.vehicle.distance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/** "Remove this photo?" before a thumbnail's remove action takes effect (`add-event-pictures`). `internal`, not
 * `private` (add-event-editing): reused as-is by the event details screen's "Edit" action, for either photo source. */
@Composable
internal fun RemovePhotoDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove this photo?") },
        confirmButton = { TextButton(onClick = onConfirm, modifier = Modifier.testTag("photo_remove_confirm")) { Text("Remove") } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag("photo_remove_cancel")) { Text("Cancel") } },
    )
}

/** "Remove this note?" before the trash-can action on [NoteField] takes effect. */
@Composable
internal fun RemoveNoteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove this note?") },
        confirmButton = { TextButton(onClick = onConfirm, modifier = Modifier.testTag("note_remove_confirm")) { Text("Remove") } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag("note_remove_cancel")) { Text("Cancel") } },
    )
}

/** Before saving a new odometer count lower than the known odometer (`confirm-lower-odometer`). */
@Composable
internal fun LowerOdometerDialog(typedText: String, knownText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save $typedText?") },
        text = { Text("That's lower than the vehicle's last known odometer, $knownText.") },
        confirmButton = { TextButton(onClick = onConfirm, modifier = Modifier.testTag("lower_odometer_confirm")) { Text("Save anyway") } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag("lower_odometer_cancel")) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateDialog(initialDateMillis: Long, onDismiss: () -> Unit, onPicked: (Long) -> Unit) {
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
internal fun TimeDialog(hour: Int, minute: Int, is24Hour: Boolean, onDismiss: () -> Unit, onPicked: (Int, Int) -> Unit) {
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
internal fun TimeZoneDialog(at: Instant, deviceZoneId: String, selectedId: String, onDismiss: () -> Unit, onPicked: (String) -> Unit) {
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
                            } else {
                                null
                            },
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
