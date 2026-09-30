package com.mikonoma.drivinglog.vehicle.eventdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.SubcomposeAsyncImage
import com.mikonoma.drivinglog.locale.DeviceLocale
import com.mikonoma.drivinglog.ui.BackButton
import com.mikonoma.drivinglog.ui.CloseButton
import com.mikonoma.drivinglog.ui.ScreenBottomSpace
import com.mikonoma.drivinglog.ui.theme.HeaderDivider
import com.mikonoma.drivinglog.ui.theme.drivingLogTopAppBarColors
import com.mikonoma.drivinglog.vehicle.distance.NoteEditorContent
import com.mikonoma.drivinglog.vehicle.distance.NoteField
import com.mikonoma.drivinglog.vehicle.distance.PhotoStripField
import com.mikonoma.drivinglog.vehicle.distance.RemovePhotoDialog
import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone
import com.mikonoma.drivinglog.vehicle.domain.RefuelingMileage
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.format.eventRowContent
import com.mikonoma.drivinglog.vehicle.format.formatOdometer

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
    // The "Edit" screen (add-event-editing, extended by add-event-pictures) replaces this screen's own content
    // while open, the same reason the compose-time form's note editor does.
    val edit = state.edit
    if (edit != null) {
        EventEditContent(edit, onIntent = onIntent, onBack = { onIntent(EventDetailsIntent.EditLeft) })
        return
    }

    // Read on every composition so a change of device locale shows the new separators.
    val symbols = deviceLocale.numberSymbols()
    val deviceZone = deviceTimeZone.current()
    val timeFormat = deviceLocale.timeFormat()
    // Reuses the row's own formatting (EventRowContent) so the details screen never drifts from what the row shows.
    val content = state.event?.let { eventRowContent(it, state.unit, symbols, deviceZone, timeFormat) }
    // Only Distance, Odometer reading and Refueling events can carry a note or photos (add-event-notes,
    // add-event-pictures, add-refueling-logging); Initial odometer never can.
    val canEdit = state.event is VehicleEvent.DistanceEntry || state.event is VehicleEvent.OdometerAnchor || state.event is VehicleEvent.Refueling

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                TopAppBar(
                    colors = drivingLogTopAppBarColors(),
                    title = { Text(content?.label ?: "Event", modifier = Modifier.testTag("event_details_title")) },
                    navigationIcon = { BackButton(onBack) },
                    actions = {
                        if (canEdit) {
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
                            // A refueling's own fields (add-refueling-logging): fuel type, fill-up state and, when
                            // it has one, its mileage — none of this is part of EventRowContent's generic figure.
                            (state.event as? VehicleEvent.Refueling)?.let { refueling ->
                                Text("Fuel type", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
                                Text(
                                    refueling.fuelType.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.testTag("event_details_fuel_type"),
                                )
                                Text(
                                    if (refueling.filledUp) "Filled up" else "Partial fill",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.testTag("event_details_filled_up"),
                                )
                                refueling.mileage?.let { mileage ->
                                    val mileageText = when (mileage) {
                                        is RefuelingMileage.Added -> "+" + formatOdometer(mileage.distance, state.unit, symbols)
                                        is RefuelingMileage.Anchor -> formatOdometer(mileage.reading, state.unit, symbols)
                                    }
                                    Text("Mileage", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
                                    Text(mileageText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("event_details_mileage"))
                                }
                            }
                            // Only Distance and Odometer reading events can carry a note (add-event-notes); an
                            // Initial odometer event's note is always null, so this section simply does not render
                            // for it, with no extra branching needed here.
                            state.event.note?.let { note ->
                                Text("Note", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 16.dp))
                                Text(note, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("event_details_note"))
                            }
                            // A thumbnail per attached photo (add-event-pictures); no section at all without one.
                            if (state.photoThumbnailUris.isNotEmpty()) {
                                Text("Photos", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 16.dp))
                                LazyRow(Modifier.testTag("event_details_photos").padding(top = 8.dp)) {
                                    items(state.photoThumbnailUris, key = { it.first }) { (photoId, uri) ->
                                        Box(Modifier.padding(end = 8.dp).size(72.dp)) {
                                            SubcomposeAsyncImage(
                                                model = uri,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable(onClickLabel = "View photo") { onIntent(EventDetailsIntent.PhotoClicked(photoId)) }
                                                    .testTag("event_photo_thumbnail_$photoId"),
                                                loading = {},
                                                error = {},
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.viewingPhotoId != null) {
        PhotoViewerDialog(uri = state.viewingPhotoUri, onDismiss = { onIntent(EventDetailsIntent.PhotoViewerClosed) })
    }
}

/** A full-size viewer for a tapped thumbnail (add-event-pictures): the whole screen, dismissed by tapping it or the
 * system back gesture. Shows a spinner while [uri] is still loading. */
@Composable
private fun PhotoViewerDialog(uri: String?, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClickLabel = "Close") { onDismiss() }
                .testTag("photo_viewer"),
            contentAlignment = Alignment.Center,
        ) {
            if (uri == null) {
                CircularProgressIndicator(color = Color.White)
            } else {
                SubcomposeAsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    loading = { CircularProgressIndicator(color = Color.White) },
                    error = {},
                )
            }
        }
    }
}

/**
 * The "Edit" screen (`add-event-editing`, extended by `add-event-pictures`): the note element and the photo strip,
 * with an explicit "Save" action. Leaving without saving — the toolbar's arrow or the system's own gesture/button,
 * both wired to [onBack] — discards the session (see [EventDetailsIntent.EditLeft]); nothing here reaches the saved
 * event until "Save" is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventEditContent(edit: EventEditState, onIntent: (EventDetailsIntent) -> Unit, onBack: () -> Unit) {
    if (edit.noteEditorText != null) {
        NoteEditorContent(
            text = edit.noteEditorText,
            onTextChanged = { onIntent(EventDetailsIntent.EditNoteTextEdited(it)) },
            onAttach = { onIntent(EventDetailsIntent.EditNoteAttached) },
            onDiscard = { onIntent(EventDetailsIntent.EditNoteDiscarded) },
        )
        return
    }

    // Kept (already-saved) photos first, then newly picked ones, matching the order they will end up in once saved.
    val previewUris = edit.keptPhotos + edit.newPhotoPreviewUris
    val keptIds = edit.keptPhotos.map { it.first }.toSet()
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                TopAppBar(
                    colors = drivingLogTopAppBarColors(),
                    title = { Text("Edit event") },
                    navigationIcon = { CloseButton(onBack) },
                    actions = {
                        TextButton(
                            onClick = { onIntent(EventDetailsIntent.EditSaved) },
                            enabled = !edit.isSaving,
                            modifier = Modifier.testTag("edit_event_save"),
                        ) { Text("Save") }
                    },
                )
                HeaderDivider()
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            NoteField(
                pendingNote = edit.noteDraft.ifBlank { null },
                onOpen = { onIntent(EventDetailsIntent.EditNoteOpened) },
                onRemove = {},
                showRemoveAction = false,
            )
            PhotoStripField(
                isFull = edit.isFull,
                error = edit.newPhotos.error,
                previewUris = previewUris,
                onPhotoPicked = { onIntent(EventDetailsIntent.EditPhotoPicked(it)) },
                onRemoveRequested = { id ->
                    if (id in keptIds) onIntent(EventDetailsIntent.EditSavedPhotoRemoveRequested(id))
                    else onIntent(EventDetailsIntent.EditNewPhotoRemoveRequested(id))
                },
            )
        }
    }

    if (edit.savedPhotoRemovalPendingId != null) {
        RemovePhotoDialog(
            onConfirm = { onIntent(EventDetailsIntent.EditSavedPhotoRemoveConfirmed) },
            onDismiss = { onIntent(EventDetailsIntent.EditSavedPhotoRemoveCancelled) },
        )
    }
    if (edit.newPhotos.removalPendingId != null) {
        RemovePhotoDialog(
            onConfirm = { onIntent(EventDetailsIntent.EditNewPhotoRemoveConfirmed) },
            onDismiss = { onIntent(EventDetailsIntent.EditNewPhotoRemoveCancelled) },
        )
    }
}
