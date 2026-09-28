package com.mikonoma.drivinglog.vehicle.distance

import com.mikonoma.drivinglog.ui.theme.headerTextButtonColors
import com.mikonoma.drivinglog.ui.theme.drivingLogTopAppBarColors
import com.mikonoma.drivinglog.ui.theme.HeaderDivider
import com.mikonoma.drivinglog.ui.ScreenBottomSpace
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.mikonoma.drivinglog.locale.DeviceLocale
import com.mikonoma.drivinglog.ui.BackButton
import com.mikonoma.drivinglog.ui.CloseButton
import com.mikonoma.drivinglog.ui.PhotoIcons
import com.mikonoma.drivinglog.ui.RequiredFieldNote
import com.mikonoma.drivinglog.ui.OdometerField
import com.mikonoma.drivinglog.ui.VehiclePicture
import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone
import com.mikonoma.drivinglog.vehicle.format.formatOdometer
import com.mikonoma.drivinglog.vehicle.format.formatTimeOfDay
import com.mikonoma.drivinglog.vehicle.picture.EventPhotoDraft
import com.mikonoma.drivinglog.vehicle.picture.PhotoResult
import com.mikonoma.drivinglog.vehicle.picture.PictureError
import com.mikonoma.drivinglog.vehicle.picture.rememberPhotoPicker
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

    // A restored form's attached photo ids survive, but their thumbnail URIs do not (add-event-pictures).
    LaunchedEffect(processor) { processor.dispatch(LogEventIntent.PhotoPreviewRefresh) }

    LogEventContent(
        state = state,
        deviceLocale = deviceLocale,
        deviceTimeZoneId = deviceTimeZone.current().id,
        onIntent = processor::dispatch,
        // Leaving without saving deletes any attached photo's pending files (add-event-pictures); a rotation is not
        // leaving and keeps them.
        onBack = {
            processor.dispatch(LogEventIntent.Left)
            onBack()
        },
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
    // The note editor (add-event-notes) replaces the whole screen's content in the same window while open, rather than a
    // separate Dialog: a Dialog is a second Android window, and once its one text field has taken and released IME focus,
    // this app's accessibility tree stops exposing that window's content at all (confirmed independently of Maestro, with
    // plain `adb shell uiautomator dump`) — a real platform/tooling limitation, not a bug in the screen. Every other text
    // field in the app lives in the ordinary single-window screens and has never shown this.
    // The scan's review (odometer-ocr-capture) replaces the form's content the same way, for the same reason.
    if (state.isScanning) {
        ScanProgressContent()
        return
    }
    state.scan.review?.let { review ->
        ScanReviewContent(review, state.scanPhotoUri, onIntent)
        return
    }
    if (state.noteDraft != null) {
        NoteEditorContent(
            text = state.noteDraft,
            onTextChanged = { onIntent(LogEventIntent.NoteDraftEdited(it)) },
            onAttach = { onIntent(LogEventIntent.NoteAttached) },
            onDiscard = { onIntent(LogEventIntent.NoteDiscarded) },
        )
        return
    }

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
                    navigationIcon = { CloseButton(onBack) },
                    actions = {
                        TextButton(
                            colors = headerTextButtonColors(),
                            onClick = { onIntent(LogEventIntent.Save) },
                            enabled = !state.isLoading && !state.notFound && !state.isSaving && !state.activeEntry.isEmpty,
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
                        label = if (state.way == LogWay.TRIP_DISTANCE) "Trip distance *" else "New odometer *",
                        isError = state.error != null && state.error !is LogDistanceError.TimeInFuture,
                        errorText = state.error?.takeIf { it !is LogDistanceError.TimeInFuture }?.let { errorMessage(state, symbols) },
                    )
                    if (state.canScan) {
                        ScanReadingAction(
                            error = state.scan.error,
                            onPicked = { onIntent(LogEventIntent.ScanPhotoPicked(it)) },
                            onLaunch = { onIntent(LogEventIntent.ScanErrorDismissed) },
                        )
                    }
                    NoteField(
                        pendingNote = state.pendingNote,
                        onOpen = { onIntent(LogEventIntent.NoteEditorOpened) },
                        onRemove = { onIntent(LogEventIntent.NoteRemoveRequested) },
                    )
                    PhotoStripField(
                        photos = state.photos,
                        previewUris = state.photoPreviewUris,
                        onPhotoPicked = { onIntent(LogEventIntent.PhotoPicked(it)) },
                        onRemoveRequested = { onIntent(LogEventIntent.PhotoRemoveRequested(it)) },
                    )
                    RequiredFieldNote()
                }
            }
        }
    }

    if (state.noteRemovalPending) {
        RemoveNoteDialog(
            onConfirm = { onIntent(LogEventIntent.NoteRemoveConfirmed) },
            onDismiss = { onIntent(LogEventIntent.NoteRemoveCancelled) },
        )
    }

    if (state.photos.removalPendingId != null) {
        RemovePhotoDialog(
            onConfirm = { onIntent(LogEventIntent.PhotoRemoveConfirmed) },
            onDismiss = { onIntent(LogEventIntent.PhotoRemoveCancelled) },
        )
    }

    if (state.lowerOdometerConfirmationPending) {
        val known = state.knownOdometer
        val typed = state.activeEntry.toDistance()
        if (known != null && typed != null) {
            LowerOdometerDialog(
                typedText = formatOdometer(typed, state.vehicleUnit, symbols),
                knownText = formatOdometer(known, state.vehicleUnit, symbols),
                onConfirm = { onIntent(LogEventIntent.LowerOdometerConfirmed) },
                onDismiss = { onIntent(LogEventIntent.LowerOdometerCancelled) },
            )
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

/**
 * The optional note (`add-event-notes`, restyled by `restyle-note-field`): a Material 3 outlined field — the same
 * visual family as [KindSelector]/[VehicleSelector] — with a "Note" label, built from
 * [OutlinedTextFieldDefaults.DecorationBox] rather than a real [OutlinedTextField]: a real field's `value` has no
 * ellipsis support (only clipping), and the two-line end-ellipsized preview is an existing, unchanged requirement.
 * [innerTextField] is a plain [Text] (the prompt or the truncated note), never a real text field, so there is no
 * cursor, focus or keyboard to trigger — tapping only ever opens the full-screen editor. The `value` passed to the
 * decoration box is the same text that is shown, always non-empty, purely to keep the "Note" label minimized and
 * floated (the box never receives real focus, so an empty, never-focused field would otherwise show a large,
 * centered label with no visible content). `DecorationBox` has no `modifier` of its own — in the usual pattern it
 * decorates a `BasicTextField`, which carries one — so the tap target, width and test tag are on the wrapping [Box].
 */
@OptIn(ExperimentalMaterial3Api::class)
/** `internal`, not `private` (add-event-editing): reused as-is by the event details screen's "Edit" action, where
 * [showRemoveAction] is false — that screen's only way to clear a note is opening the editor and blanking the text
 * (see its own spec scenario), not a separate trash-can action. */
@Composable
internal fun NoteField(pendingNote: String?, onOpen: () -> Unit, onRemove: () -> Unit, showRemoveAction: Boolean = true) {
    val shown = pendingNote ?: "Add a note..."
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Add a note", role = Role.Button, onClick = onOpen)
            .testTag("log_note")
            // The "Note" label and the shown text are separate child nodes of the DecorationBox (there is no real
            // OutlinedTextField merging them onto this element's own semantics the way it normally would); adding
            // (not clearing) a contentDescription is what makes this element's own accessible text/label include
            // the shown text, without touching the trailing icon's own, separate semantics below.
            .semantics { contentDescription = shown },
        // DecorationBox has no modifier of its own (see the class doc); propagating this Box's min
        // constraints down is what makes it actually stretch to fillMaxWidth instead of wrapping its content.
        propagateMinConstraints = true,
    ) {
        OutlinedTextFieldDefaults.DecorationBox(
            value = shown,
            innerTextField = {
                Text(
                    text = shown,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (pendingNote != null) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            },
            enabled = true,
            singleLine = false,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            label = { Text("Note") },
            trailingIcon = if (pendingNote != null && showRemoveAction) {
                {
                    IconButton(onClick = onRemove, modifier = Modifier.testTag("log_note_remove")) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove note")
                    }
                }
            } else null,
        )
    }
}

/**
 * Up to 5 photos attached to the entry (`add-event-pictures`): a thumbnail per attached photo, each with its own
 * remove action, and an "Add photo" tile while under the cap. Tapping "Add photo" opens the system's own chooser of
 * image sources, and the chosen photo appears in the strip with no crop step — an event photo is looked at for its
 * content, not shown as a small square avatar the way a vehicle's own picture is.
 */
@Composable
private fun PhotoStripField(
    photos: EventPhotoDraft,
    previewUris: List<Pair<String, String>>,
    onPhotoPicked: (PhotoResult) -> Unit,
    onRemoveRequested: (String) -> Unit,
) = PhotoStripField(isFull = photos.isFull, error = photos.error, previewUris = previewUris, onPhotoPicked = onPhotoPicked, onRemoveRequested = onRemoveRequested)

/**
 * The strip itself, decoupled from [EventPhotoDraft]: `internal`, not `private` (add-event-editing), reused as-is by
 * the event details screen's "Edit" action, whose photos come from two sources (already-saved and newly picked)
 * rather than one [EventPhotoDraft] — the caller merges [previewUris] and computes [isFull]/[error] from whichever
 * source(s) apply, and decides which removal path [onRemoveRequested]'s id means.
 */
@Composable
internal fun PhotoStripField(
    isFull: Boolean,
    error: PictureError?,
    previewUris: List<Pair<String, String>>,
    onPhotoPicked: (PhotoResult) -> Unit,
    onRemoveRequested: (String) -> Unit,
) {
    val picker = rememberPhotoPicker(onPhotoPicked)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Photos", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((id, uri) in previewUris) {
                PhotoThumbnail(uri, id, onRemove = { onRemoveRequested(id) }, modifier = Modifier.testTag("event_photo_$id"))
            }
            if (!isFull) {
                Box(
                    Modifier
                        .size(72.dp)
                        .testTag("add_photo")
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClickLabel = "Add photo", role = Role.Button, onClick = picker.launch),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(PhotoIcons.Camera, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        error?.let {
            Text(
                when (it) {
                    PictureError.COULD_NOT_OPEN -> "The photo could not be opened"
                    PictureError.CAMERA_DENIED -> "Camera access is turned off. You can allow it in the device settings."
                },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("photo_error"),
            )
        }
    }
}

@Composable
internal fun PhotoThumbnail(uri: String, pendingId: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(72.dp)) {
        SubcomposeAsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
            loading = {},
            error = {},
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                .clickable(onClickLabel = "Remove photo", role = Role.Button, onClick = onRemove)
                .testTag("photo_remove_$pendingId"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

/**
 * The full-screen note editor: a plain multi-line text field, seeded with the note pending so far. It replaces the log event
 * form's own content in the same window while open — not a separate [Dialog] (a second Android window): once that window's
 * one text field had taken and released IME focus, the accessibility tree stopped exposing its content at all, confirmed with
 * plain `adb shell uiautomator dump`, independent of any test tooling. Every other text field in the app already lives in an
 * ordinary single-window screen and has never shown this. Back navigation — the toolbar's arrow or the system's own
 * gesture/button, both wired to [onAttach] — attaches what was typed; "Discard" is the one way to leave without attaching it.
 * Not its own navigation destination: there is nothing to restore it into once the form itself is gone.
 *
 * `internal`, not `private` (add-event-editing): reused as-is by the event details screen's "Edit" action, in a
 * mode where [onAttach] saves directly to the stored event instead of attaching to an in-memory pending draft —
 * the only difference, per this requirement's own design.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
internal fun NoteEditorContent(text: String, onTextChanged: (String) -> Unit, onAttach: () -> Unit, onDiscard: () -> Unit) {
    BackHandler(onBack = onAttach)
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                TopAppBar(
                    colors = drivingLogTopAppBarColors(),
                    title = { Text("Note") },
                    navigationIcon = { BackButton(onAttach) },
                    actions = {
                        TextButton(
                            colors = headerTextButtonColors(),
                            onClick = onDiscard,
                            modifier = Modifier.testTag("note_editor_discard"),
                        ) { Text("Discard") }
                    },
                )
                HeaderDivider()
            }
        },
    ) { padding ->
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        // Seeded once per time the editor opens (this composable is only in composition while it is open, so a
        // fresh `rememberTextFieldState` runs each time). Its own default `initialSelection` places the cursor at
        // the end of any existing text — not position 0, `TextFieldValue`'s default for a freshly focused field,
        // which left an edit of a non-empty note (add-event-editing) inserting new text before the old instead of
        // replacing it. `TextFieldValue` itself (even with its default, zero selection) was tried first and
        // rejected: on this Compose Multiplatform version, an `OutlinedTextField` bound to it froze the whole
        // screen's touch and back handling after any edit, reproduced independent of the selection value — a
        // library-level interaction bug, not this screen's own state. `TextFieldState` doesn't share it. Not
        // re-seeded on every keystroke: after this, the field's own edits are the source of truth, fed back up via
        // onTextChanged.
        val fieldState = rememberTextFieldState(initialText = text)
        LaunchedEffect(fieldState) { snapshotFlow { fieldState.text.toString() }.collect(onTextChanged) }
        OutlinedTextField(
            state = fieldState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .focusRequester(focus)
                .testTag("note_editor_field"),
        )
    }
}

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
private fun RemoveNoteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove this note?") },
        confirmButton = { TextButton(onClick = onConfirm, modifier = Modifier.testTag("note_remove_confirm")) { Text("Remove") } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag("note_remove_cancel")) { Text("Cancel") } },
    )
}

/** Before saving a new odometer count lower than the known odometer (`confirm-lower-odometer`). */
@Composable
private fun LowerOdometerDialog(typedText: String, knownText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save $typedText?") },
        text = { Text("That's lower than the vehicle's last known odometer, $knownText.") },
        confirmButton = { TextButton(onClick = onConfirm, modifier = Modifier.testTag("lower_odometer_confirm")) { Text("Save anyway") } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag("lower_odometer_cancel")) { Text("Cancel") } },
    )
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
