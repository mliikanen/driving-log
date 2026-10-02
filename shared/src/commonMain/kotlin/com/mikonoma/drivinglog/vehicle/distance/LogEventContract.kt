package com.mikonoma.drivinglog.vehicle.distance

import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.FuelType
import com.mikonoma.drivinglog.vehicle.domain.FuelUnit
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.VehicleFuelType
import com.mikonoma.drivinglog.vehicle.domain.VehicleType
import com.mikonoma.drivinglog.vehicle.domain.allowedFuelTypes
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import com.mikonoma.drivinglog.vehicle.domain.currentOdometer
import com.mikonoma.drivinglog.vehicle.domain.knownOdometerAt
import com.mikonoma.drivinglog.vehicle.input.FuelAmountEntry
import com.mikonoma.drivinglog.vehicle.input.OdometerEntry
import com.mikonoma.drivinglog.vehicle.ocr.LiveReading
import com.mikonoma.drivinglog.vehicle.ocr.ScanDraft
import com.mikonoma.drivinglog.vehicle.picture.EventPhotoDraft
import com.mikonoma.drivinglog.vehicle.picture.PhotoResult
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.fuusio.kide.presentation.SideEffect
import org.fuusio.kide.presentation.ViewIntent
import org.fuusio.kide.presentation.ViewState

/**
 * A kind of event the form can log. The Kind selector is disabled only while [entries] has one member — kept that
 * way by [LogEventScreen]'s `KindSelector` reading it, not a hand-written flag — which no longer happens now that
 * [REFUELING] exists (`add-refueling-logging`).
 */
enum class LogKind(val label: String) {
    DISTANCE("Distance"),
    REFUELING("Refueling"),
}

/** Which field a scan is for (`add-fuel-amount-ocr`): the mileage section's own "Scan a reading" action, or the
 * refueling form's fuel amount field's. The two share one [LogEventState.scan], so this says which classify
 * function the next photo or live frame runs through. */
enum class ScanTarget { MILEAGE, FUEL_AMOUNT }

/** One vehicle as the selector draws it: its picture or icon, its name and, when it has one, its plate. In the order of [com.mikonoma.drivinglog.vehicle.domain.VehicleNameOrder]. */
data class VehicleChoice(
    val id: String,
    val name: String,
    val licensePlate: String?,
    val type: VehicleType,
    val color: Rgb,
    val pictureUri: String?,
)

/**
 * The log distance form. What the user typed and chose is saved so it survives rotation and process death; what comes from
 * the repository or is only a message ([Transient]) is rebuilt.
 */
@Serializable
data class LogEventState(
    /** The kind of event being logged. Only [LogKind.DISTANCE] exists today, and the selector that shows it is disabled; persisted (not [Transient]) so a later
     * kind's choice survives rotation and process death the way the vehicle choice does. */
    val kind: LogKind = LogKind.DISTANCE,
    val way: LogWay = LogWay.TRIP_DISTANCE,
    /** What is typed for the "Trip distance" way. Each way keeps its own number. */
    val tripDistance: OdometerEntry,
    /** What is typed for the "New odometer" way. */
    val newOdometer: OdometerEntry,
    /** The wall-clock date and time of the entry, in [zoneId]. Starts as the moment the form was opened. */
    val localDateTime: LocalDateTime,
    val zoneId: String,
    /**
     * The vehicle the entry is for. Empty until the form (opened from the Home screen, with a selector) has picked one: the
     * restored choice if that vehicle still exists, else the vehicle last logged for, else the first by name. From a vehicle's
     * details screen this is that vehicle's id from the moment the form opens, and the selector is not shown.
     */
    val selectedVehicleId: String = "",
    /** The id of the vehicle the unit was last set from, so switching to another vehicle (or, once, opening the form) sets it again but a restored state keeps what the user chose. */
    val unitFor: String = "",
    /** The id of the vehicle [fuelType] was last resolved against (`vehicle-fuel-type`'s filtering), the same
     * mechanism as [unitFor]: switching to another vehicle re-resolves it, a restored state keeps what the user chose. */
    val fuelTypeFor: String = "",
    /** The note attached to the event so far (`add-event-notes`), or null when none has been added. Persisted, not [Transient], so it
     * survives rotation and process death, and so it is kept when the vehicle is changed, like every other field on this form. */
    val pendingNote: String? = null,
    /**
     * The full-screen note editor's own state: null while it is closed, or the text being edited (including `""`) while it is open,
     * seeded from [pendingNote] when opened. Persisted like [PictureEditState.cropSourceId] is, for the same reason: rotating the
     * device or losing the process while the editor is open must not lose what was typed.
     */
    val noteDraft: String? = null,
    @Transient val isLoading: Boolean = true,
    @Transient val notFound: Boolean = false,
    @Transient val vehicleUnit: OdometerUnit = OdometerUnit.KILOMETERS,
    /** The currently-selected vehicle's own fuel type (`vehicle-fuel-type`), a placeholder until it loads. Narrows [allowedFuelTypes]. */
    @Transient val vehicleFuelType: VehicleFuelType = VehicleFuelType.PETROL,
    /** Every vehicle the selector offers, in order. Empty when the form has a fixed vehicle (opened from its details screen). */
    @Transient val vehicles: List<VehicleChoice> = emptyList(),
    /** The vehicle's whole log, newest first. */
    @Transient val log: List<VehicleEvent> = emptyList(),
    @Transient val error: LogDistanceError? = null,
    @Transient val isSaving: Boolean = false,
    /** True while the "Remove this note?" confirmation is shown. A confirmation mid-flight is not worth surviving process death. */
    @Transient val noteRemovalPending: Boolean = false,
    /** True while the "save a lower odometer count anyway?" confirmation is shown (`confirm-lower-odometer`). A confirmation
     * mid-flight is not worth surviving process death. */
    @Transient val lowerOdometerConfirmationPending: Boolean = false,
    /** Up to 5 photos attached so far (`add-event-pictures`), for a "Distance" or "Odometer reading" entry. Persisted,
     * not [Transient], like [PictureEditState.cropSourceId] is: rotating the device or losing the process while a
     * photo is attached must not lose it. */
    val photos: EventPhotoDraft = EventPhotoDraft(),
    /** [photos]' thumbnail URIs, paired with each photo's pending id: rebuilt from the store, not persisted. */
    @Transient val photoPreviewUris: List<Pair<String, String>> = emptyList(),
    /** The scan's review screen while it is open, and the scan accepted last (`odometer-ocr-capture`). Persisted like [photos]. */
    val scan: ScanDraft = ScanDraft(),
    /** Which field the currently-open (or last-opened) scan is for (`add-fuel-amount-ocr`): the mileage section's
     * own action and the refueling form's fuel amount field share this one [scan], so this says which classify
     * function the next photo or live frame is run through. Persisted, not [Transient], for the same reason
     * [scan] is: set when [LogEventIntent.ScannerOpened] is dispatched, read again if the process is lost mid-scan. */
    val scanTarget: ScanTarget = ScanTarget.MILEAGE,
    /** True while a chosen photo is being recognized. */
    @Transient val isScanning: Boolean = false,
    /** Where the review screen loads the scanned photo from: rebuilt from the capture store, not persisted. */
    @Transient val scanPhotoUri: String? = null,
    /** False on a platform with no text recognizer (iOS): the "Scan a reading" action is not shown. Persisted, not [Transient]: it
     * is set once when the form opens, and a restored form must not lose it. */
    val canScan: Boolean = false,
    /** A "Refueling" entry's fuel amount (`add-refueling-logging`). Persisted, like [tripDistance]. */
    val fuelAmount: FuelAmountEntry = FuelAmountEntry(),
    /** The unit [fuelAmount] is entered in: starts as the last one chosen anywhere, once loaded (see [fuelPreferencesLoaded]). */
    val fuelUnit: FuelUnit = FuelUnit.LITERS,
    /** A "Refueling" entry's fuel type: starts as the last one chosen anywhere, once loaded. */
    val fuelType: FuelType = FuelType.REGULAR_PETROL,
    /** A "Refueling" entry's "filled up" checkbox: always starts checked, per-event, never remembered across events. */
    val filledUp: Boolean = true,
    /** True once [fuelUnit]/[fuelType] have been seeded from the remembered global preference (`add-refueling-logging`'s
     * "remembers the last choice" requirements), so a restored mid-edit choice is never overwritten by it loading again. */
    val fuelPreferencesLoaded: Boolean = false,
) : ViewState {

    /** The unit both fields are entered in. */
    val unit: OdometerUnit get() = tripDistance.unit

    val activeEntry: OdometerEntry get() = if (way == LogWay.TRIP_DISTANCE) tripDistance else newOdometer

    /** Whether Save can be tapped, besides the loading/saving/vehicle checks every kind shares: a "Distance" entry needs
     * its active field filled in; a refueling needs only the fuel amount — its optional mileage section never gates
     * Save (`refueling-logging`'s "The fuel amount must be entered and above zero"). */
    val hasRequiredField: Boolean
        get() = when (kind) {
            LogKind.DISTANCE -> !activeEntry.isEmpty
            LogKind.REFUELING -> !fuelAmount.isEmpty
        }

    /** The selector shows this vehicle as chosen, or null before [vehicles] has loaded. */
    val selectedVehicle: VehicleChoice? get() = vehicles.firstOrNull { it.id == selectedVehicleId }

    /** The refueling fuel types the currently-selected vehicle offers (`vehicle-fuel-type`'s filtering). */
    val allowedFuelTypes: Set<FuelType> get() = vehicleFuelType.allowedFuelTypes()

    /** The instant and zone the wall-clock time and zone id mean. */
    val moment: ZonedMoment
        get() = runCatching { momentOf(localDateTime, zoneId) }.getOrElse { momentOf(localDateTime, "UTC") }

    /** The previous known odometer at the chosen moment, or null when none is known at that time. */
    val knownOdometer: Distance? get() = knownOdometerAt(log.asReversed(), moment.instant)

    /** The vehicle's actual current odometer, independent of the chosen moment (`confirm-lower-odometer`) — distinct
     * from [knownOdometer] when the chosen moment is backdated before a later logged event. */
    val mostRecentKnownOdometer: Distance? get() = currentOdometer(log.asReversed())

    /** For "New odometer": the distance the typed count means, once something is typed and it is higher than the known one. */
    val previewDistance: Distance?
        get() {
            val known = knownOdometer ?: return null
            val typed = newOdometer.toDistance() ?: return null
            return distanceByOdometer(typed, known)
        }
}

sealed interface LogEventIntent : ViewIntent {
    data class WayChanged(val way: LogWay) : LogEventIntent
    data class UnitFamilySelected(val miles: Boolean) : LogEventIntent
    data class TenthsChanged(val included: Boolean) : LogEventIntent

    /** The active field's new text from the system keyboard. */
    data class OdometerEdited(val text: String) : LogEventIntent
    data object OdometerCleared : LogEventIntent

    /** A "Refueling" entry's fuel amount field, mirroring [OdometerEdited]/[OdometerCleared] (`add-refueling-logging`). */
    data class FuelAmountEdited(val text: String) : LogEventIntent
    data object FuelAmountCleared : LogEventIntent

    /** Changes the unit the fuel amount is entered in; keeps the digits typed, like [UnitFamilySelected] does. */
    data class FuelUnitSelected(val unit: FuelUnit) : LogEventIntent
    data class FuelTypeSelected(val type: FuelType) : LogEventIntent
    data class FilledUpChanged(val checked: Boolean) : LogEventIntent

    data class DateChanged(val date: LocalDate) : LogEventIntent
    data class TimeChanged(val hour: Int, val minute: Int) : LogEventIntent

    /** Keeps the wall-clock date and time and changes the zone they are in. */
    data class ZoneChanged(val zoneId: String) : LogEventIntent

    /** Chooses another vehicle in the selector (only offered when the form was opened without one). */
    data class VehicleSelected(val vehicleId: String) : LogEventIntent

    /** Chooses another kind of event in the Kind selector. The selector is disabled while [LogKind] has one entry, so nothing dispatches this yet. */
    data class KindSelected(val kind: LogKind) : LogEventIntent
    data object Save : LogEventIntent

    /** Opens the full-screen note editor, seeding its draft from the note pending so far. */
    data object NoteEditorOpened : LogEventIntent

    /** The editor's text field changed. */
    data class NoteDraftEdited(val text: String) : LogEventIntent

    /** Back navigation out of the editor: attaches the draft (trimmed; blank becomes no note) as the pending note. */
    data object NoteAttached : LogEventIntent

    /** The editor's "Discard" action: closes it, leaving the note pending before it opened untouched. */
    data object NoteDiscarded : LogEventIntent

    /** The note element's trash-can action: asks for confirmation before removing the pending note. */
    data object NoteRemoveRequested : LogEventIntent

    /** Confirms the removal dialog: clears the pending note. */
    data object NoteRemoveConfirmed : LogEventIntent

    /** Cancels or dismisses the removal dialog: leaves the pending note as it was. */
    data object NoteRemoveCancelled : LogEventIntent

    /** Confirms saving a new odometer count lower than the known odometer (`confirm-lower-odometer`). */
    data object LowerOdometerConfirmed : LogEventIntent

    /** Cancels or dismisses the lower-odometer confirmation dialog: saves nothing. */
    data object LowerOdometerCancelled : LogEventIntent

    /** Rebuilds the photo strip's thumbnail URIs from the store: dispatched once when the screen opens, since a
     * restored form's photo ids survive but their URIs ([LogEventState.photoPreviewUris]) do not (`add-event-pictures`,
     * mirroring [com.mikonoma.drivinglog.vehicle.add.AddVehicleIntent.PictureRefresh]). */
    data object PhotoPreviewRefresh : LogEventIntent

    /** The system chooser gave back a photo, or nothing, for the photo strip (`add-event-pictures`). */
    data class PhotoPicked(val result: PhotoResult) : LogEventIntent

    /** A thumbnail's remove action: asks for confirmation before removing that photo. */
    data class PhotoRemoveRequested(val pendingId: String) : LogEventIntent

    /** Confirms the removal dialog: the photo is deleted and leaves the strip. */
    data object PhotoRemoveConfirmed : LogEventIntent

    /** Cancels or dismisses the removal dialog: the photo stays attached. */
    data object PhotoRemoveCancelled : LogEventIntent

    /** The system chooser gave back a photo, or nothing, to scan for a reading (`odometer-ocr-capture`). */
    data class ScanPhotoPicked(val result: PhotoResult) : LogEventIntent

    /** A candidate's box or label was tapped on the review screen; [index] is its index in the review's detections. */
    data class ScanCandidateSelected(val index: Int) : LogEventIntent

    /** The review screen's confirm action: the selected candidate fills the field and sets the way. */
    data object ScanConfirmed : LogEventIntent

    /** Back navigation out of the review screen, or its "Leave": nothing changes on the form and the photo is dropped. */
    data object ScanCancelled : LogEventIntent

    data object ScanErrorDismissed : LogEventIntent

    /** "Scan a reading", after the camera permission was asked for when needed: the live scanner opens
     * (`add-live-scanner`), for [target] (`add-fuel-amount-ocr`). */
    data class ScannerOpened(val target: ScanTarget) : LogEventIntent

    /** The live scanner's close action or back: the form as it was, nothing kept. */
    data object ScannerClosed : LogEventIntent

    /** A reading was tapped in the live scanner: it is applied like a confirmed candidate, and its frame kept. */
    class LiveReadingTapped(val reading: LiveReading) : LogEventIntent

    /** The user left the form without saving (add-event-pictures): every attached photo's pending files are deleted, and any scan's. */
    data object Left : LogEventIntent
}

sealed interface LogEventEffect : SideEffect {
    /** The entry was saved; go back to whatever opened the form (a vehicle's details, or the Home screen). */
    data object Saved : LogEventEffect
}
