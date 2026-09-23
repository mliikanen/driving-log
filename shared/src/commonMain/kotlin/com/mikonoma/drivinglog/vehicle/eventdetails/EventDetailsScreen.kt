package com.mikonoma.drivinglog.vehicle.eventdetails

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.mikonoma.drivinglog.ui.ScreenBottomSpace
import com.mikonoma.drivinglog.ui.theme.HeaderDivider
import com.mikonoma.drivinglog.ui.theme.drivingLogTopAppBarColors
import com.mikonoma.drivinglog.vehicle.distance.NoteEditorContent
import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.format.eventRowContent

@Composable
fun EventDetailsScreen(
    processor: EventDetailsProcessor,
    deviceLocale: DeviceLocale,
    deviceTimeZone: DeviceTimeZone,
    onBack: () -> Unit,
) {
    val state by processor.states.collectAsState()
    EventDetailsContent(state, deviceLocale, deviceTimeZone, onIntent = processor::dispatch, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsContent(
    state: EventDetailsState,
    deviceLocale: DeviceLocale,
    deviceTimeZone: DeviceTimeZone,
    onIntent: (EventDetailsIntent) -> Unit,
    onBack: () -> Unit,
) {
    // add-event-editing: the note editor replaces the whole screen's content in the same window while open, the
    // same reason distance-logging's compose-time form does (see NoteEditorContent's own doc comment).
    if (state.noteDraft != null) {
        NoteEditorContent(
            text = state.noteDraft,
            onTextChanged = { onIntent(EventDetailsIntent.NoteDraftEdited(it)) },
            onAttach = { onIntent(EventDetailsIntent.NoteAttached) },
            onDiscard = { onIntent(EventDetailsIntent.NoteDiscarded) },
        )
        return
    }

    // Read on every composition so a change of device locale shows the new separators.
    val symbols = deviceLocale.numberSymbols()
    val deviceZone = deviceTimeZone.current()
    val timeFormat = deviceLocale.timeFormat()
    // Reuses the row's own formatting (EventRowContent) so the details screen never drifts from what the row shows.
    val content = state.event?.let { eventRowContent(it, state.unit, symbols, deviceZone, timeFormat) }
    // Only Distance and Odometer reading events can carry a note (add-event-notes); Initial odometer never can.
    val canEditNote = state.event is VehicleEvent.DistanceEntry || state.event is VehicleEvent.OdometerAnchor

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                TopAppBar(
                    colors = drivingLogTopAppBarColors(),
                    title = { Text(content?.label ?: "Event", modifier = Modifier.testTag("event_details_title")) },
                    navigationIcon = { BackButton(onBack) },
                    actions = {
                        if (canEditNote) {
                            IconButton(
                                onClick = { onIntent(EventDetailsIntent.EditClicked) },
                                modifier = Modifier.testTag("edit_event"),
                            ) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
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
                state.notFound || content == null -> Text("This event no longer exists.", Modifier.padding(16.dp))
                else -> LazyColumn(
                    Modifier.fillMaxSize().testTag("event_details_content"),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + ScreenBottomSpace),
                ) {
                    item {
                        Column {
                            Text("Date and time", style = MaterialTheme.typography.labelMedium)
                            Text(
                                content.moment,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.testTag("event_details_moment"),
                            )
                            Text("Amount", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
                            Text(
                                content.trailing,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.testTag("event_details_figure"),
                            )
                            content.loggedOdometer?.let { loggedOdometer ->
                                Text(
                                    loggedOdometer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.testTag("event_details_logged_odometer"),
                                )
                            }
                            // Only Distance and Odometer reading events can carry a note (add-event-notes); an
                            // Initial odometer event's note is always null, so this section simply does not render
                            // for it, with no extra branching needed here.
                            state.event.note?.let { note ->
                                Text("Note", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 16.dp))
                                Text(note, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("event_details_note"))
                            }
                        }
                    }
                }
            }
        }
    }
}
