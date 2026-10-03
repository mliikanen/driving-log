package com.mikonoma.drivinglog.vehicle.distance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mikonoma.drivinglog.locale.DeviceLocale
import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.ui.CloseButton
import com.mikonoma.drivinglog.ui.OdometerField
import com.mikonoma.drivinglog.ui.RequiredFieldNote
import com.mikonoma.drivinglog.ui.ScreenBottomSpace
import com.mikonoma.drivinglog.ui.theme.HeaderDivider
import com.mikonoma.drivinglog.ui.theme.drivingLogTopAppBarColors
import com.mikonoma.drivinglog.ui.theme.headerTextButtonColors
import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone
import com.mikonoma.drivinglog.vehicle.format.formatOdometer
import com.mikonoma.drivinglog.vehicle.ocr.LiveScanner
import com.mikonoma.drivinglog.vehicle.ocr.ui.LiveScannerContent
import com.mikonoma.drivinglog.vehicle.ocr.ui.ScanCallbacks
import com.mikonoma.drivinglog.vehicle.ocr.ui.ScanProgressContent
import com.mikonoma.drivinglog.vehicle.ocr.ui.ScanReadingAction
import com.mikonoma.drivinglog.vehicle.ocr.ui.ScanReviewContent

@Composable
fun LogEventScreen(processor: LogEventProcessor, deviceLocale: DeviceLocale, deviceTimeZone: DeviceTimeZone, onBack: () -> Unit) {
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
        newLiveScanner = processor::liveScanner,
        // Leaving without saving deletes any attached photo's pending files (add-event-pictures); a rotation is not
        // leaving and keeps them.
        onBack = {
            processor.dispatch(LogEventIntent.Left)
            onBack()
        },
    )
}

@Composable
fun LogEventContent(
    state: LogEventState,
    deviceLocale: DeviceLocale,
    deviceTimeZoneId: String,
    onIntent: (LogEventIntent) -> Unit,
    onBack: () -> Unit,
    newLiveScanner: () -> LiveScanner = { error("No live scanner") },
) {
    val scanCallbacks = remember(onIntent) {
        ScanCallbacks(
            onClose = { onIntent(LogEventIntent.ScannerClosed) },
            onReadingTapped = { onIntent(LogEventIntent.LiveReadingTapped(it)) },
            onPhotoPicked = { onIntent(LogEventIntent.ScanPhotoPicked(it)) },
            onCandidateSelected = { onIntent(LogEventIntent.ScanCandidateSelected(it)) },
            onConfirm = { onIntent(LogEventIntent.ScanConfirmed) },
            onCancel = { onIntent(LogEventIntent.ScanCancelled) },
            onErrorDismissed = { onIntent(LogEventIntent.ScanErrorDismissed) },
        )
    }
    // The note editor (add-event-notes) replaces the whole screen's content in the same window while open, rather than a
    // separate Dialog: a Dialog is a second Android window, and once its one text field has taken and released IME focus,
    // this app's accessibility tree stops exposing that window's content at all (confirmed independently of Maestro, with
    // plain `adb shell uiautomator dump`) — a real platform/tooling limitation, not a bug in the screen. Every other text
    // field in the app lives in the ordinary single-window screens and has never shown this.
    // The scan's review (odometer-ocr-capture) replaces the form's content the same way, for the same reason.
    val review = state.scan.review
    when {
        state.isScanning -> ScanProgressContent()

        // The photo review opens on top of the live scanner and returns to it when left (add-live-scanner).
        review != null -> ScanReviewContent(review, state.scanPhotoUri, scanCallbacks)

        state.scan.scannerOpen -> LiveScannerContent(state.scan.error, newLiveScanner, scanCallbacks)

        state.noteDraft != null -> NoteEditorContent(
            text = state.noteDraft,
            onTextChanged = { onIntent(LogEventIntent.NoteDraftEdited(it)) },
            onAttach = { onIntent(LogEventIntent.NoteAttached) },
            onDiscard = { onIntent(LogEventIntent.NoteDiscarded) },
        )

        else -> LogEventForm(state, deviceLocale, deviceTimeZoneId, onIntent, onBack)
    }
}

/** The form itself, with its dialogs: what [LogEventContent] shows when no full-screen editor or scanner is open. */
@Composable
private fun LogEventForm(state: LogEventState, deviceLocale: DeviceLocale, deviceTimeZoneId: String, onIntent: (LogEventIntent) -> Unit, onBack: () -> Unit) {
    // Read on every composition so a change of device locale shows the new separators.
    val symbols = deviceLocale.numberSymbols()
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var showZone by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { LogEventTopBar(state, onIntent, onBack) },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + ScreenBottomSpace),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                state.isLoading -> Unit

                state.notFound -> Text("This vehicle no longer exists.")

                else -> LogEventFormFields(
                    state,
                    deviceLocale,
                    symbols,
                    MomentCallbacks(onDate = { showDate = true }, onTime = { showTime = true }, onZone = { showZone = true }),
                    onIntent,
                )
            }
        }
    }

    LogEventConfirmationDialogs(state, symbols, onIntent)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogEventTopBar(state: LogEventState, onIntent: (LogEventIntent) -> Unit, onBack: () -> Unit) {
    Column {
        TopAppBar(
            colors = drivingLogTopAppBarColors(),
            title = { Text("Log event") },
            navigationIcon = { CloseButton(onBack) },
            actions = {
                TextButton(
                    colors = headerTextButtonColors(),
                    onClick = { onIntent(LogEventIntent.Save) },
                    enabled = !state.isLoading && !state.notFound && !state.isSaving && state.hasRequiredField,
                    modifier = Modifier.testTag("save_entry"),
                ) { Text("Save") }
            },
        )
        HeaderDivider()
    }
}

/** What opens the date, time and time zone pickers of the moment row. */
internal class MomentCallbacks(val onDate: () -> Unit, val onTime: () -> Unit, val onZone: () -> Unit)

/** The form's fields, once its vehicle is loaded. */
@Composable
private fun LogEventFormFields(
    state: LogEventState,
    deviceLocale: DeviceLocale,
    symbols: NumberSymbols,
    moment: MomentCallbacks,
    onIntent: (LogEventIntent) -> Unit,
) {
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
    when (state.kind) {
        LogKind.DISTANCE -> DistanceFields(state, deviceLocale, symbols, moment, onIntent)
        LogKind.REFUELING -> RefuelingFields(state, deviceLocale, symbols, moment, onIntent)
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

@Composable
private fun DistanceFields(
    state: LogEventState,
    deviceLocale: DeviceLocale,
    symbols: NumberSymbols,
    moment: MomentCallbacks,
    onIntent: (LogEventIntent) -> Unit,
) {
    WayChoice(state.way) { onIntent(LogEventIntent.WayChanged(it)) }
    MomentRow(state, deviceLocale, onDate = moment.onDate, onTime = moment.onTime, onZone = moment.onZone)
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
        ScanReadingAction(onOpen = { onIntent(LogEventIntent.ScannerOpened(ScanTarget.MILEAGE)) })
    }
}

@Composable
private fun RefuelingFields(
    state: LogEventState,
    deviceLocale: DeviceLocale,
    symbols: NumberSymbols,
    moment: MomentCallbacks,
    onIntent: (LogEventIntent) -> Unit,
) {
    MomentRow(state, deviceLocale, onDate = moment.onDate, onTime = moment.onTime, onZone = moment.onZone)
    if (state.error is LogDistanceError.TimeInFuture) ErrorText(errorMessage(state, symbols))
    FuelAmountField(
        entry = state.fuelAmount,
        unit = state.fuelUnit,
        symbols = symbols,
        onEdit = { onIntent(LogEventIntent.FuelAmountEdited(it)) },
        onClear = { onIntent(LogEventIntent.FuelAmountCleared) },
        isError = state.error.isFuelAmountError(),
        errorText = state.error?.takeIf { it.isFuelAmountError() }?.let { errorMessage(state, symbols) },
    )
    if (state.canScan) {
        ScanReadingAction(onOpen = { onIntent(LogEventIntent.ScannerOpened(ScanTarget.FUEL_AMOUNT)) })
    }
    FuelUnitChoice(state.fuelUnit) { onIntent(LogEventIntent.FuelUnitSelected(it)) }
    FuelTypeSelector(state.fuelType, state.allowedFuelTypes) { onIntent(LogEventIntent.FuelTypeSelected(it)) }
    FilledUpChoice(state.filledUp) { onIntent(LogEventIntent.FilledUpChanged(it)) }
    // Mileage is optional for a refueling (refueling-logging): the same Way/unit/field
    // machinery a "Distance" entry uses, but its own field carries no "*" and never gates Save.
    Text("Mileage (optional)", style = MaterialTheme.typography.titleSmall)
    WayChoice(state.way) { onIntent(LogEventIntent.WayChanged(it)) }
    UnitChoice(state, onIntent)
    if (state.way == LogWay.NEW_ODOMETER) KnownOdometerInfo(state, symbols)
    // Only an error from the mileage section's own validation (never FieldEmpty: an
    // empty mileage means "no mileage," not an error) — never the fuel amount's.
    val mileageError = state.error?.takeIf { it !is LogDistanceError.TimeInFuture && !it.isFuelAmountError() }
    OdometerField(
        entry = state.activeEntry,
        symbols = symbols,
        onEdit = { onIntent(LogEventIntent.OdometerEdited(it)) },
        onClear = { onIntent(LogEventIntent.OdometerCleared) },
        label = if (state.way == LogWay.TRIP_DISTANCE) "Trip distance" else "New odometer",
        isError = mileageError != null,
        errorText = mileageError?.let { errorMessage(state, symbols) },
    )
    if (state.canScan) {
        ScanReadingAction(onOpen = { onIntent(LogEventIntent.ScannerOpened(ScanTarget.MILEAGE)) })
    }
}

private fun LogDistanceError?.isFuelAmountError(): Boolean = this is LogDistanceError.FuelAmountEmpty || this is LogDistanceError.FuelAmountNotPositive

/** The dialogs that ask the user to confirm a removal or a lower odometer. */
@Composable
private fun LogEventConfirmationDialogs(state: LogEventState, symbols: NumberSymbols, onIntent: (LogEventIntent) -> Unit) {
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
}
