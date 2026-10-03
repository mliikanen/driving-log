package com.mikonoma.drivinglog.vehicle.distance

import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone
import com.mikonoma.drivinglog.vehicle.domain.FuelUnit
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.RefuelingMileage
import com.mikonoma.drivinglog.vehicle.domain.Vehicle
import com.mikonoma.drivinglog.vehicle.domain.VehicleDetails
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.VehicleNameOrder
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.domain.allowedFuelTypes
import com.mikonoma.drivinglog.vehicle.input.FuelAmountEntry
import com.mikonoma.drivinglog.vehicle.input.OdometerEntry
import com.mikonoma.drivinglog.vehicle.ocr.CaptureStore
import com.mikonoma.drivinglog.vehicle.ocr.Detection
import com.mikonoma.drivinglog.vehicle.ocr.LiveScanner
import com.mikonoma.drivinglog.vehicle.ocr.ReadingKind
import com.mikonoma.drivinglog.vehicle.ocr.RecognizedPhoto
import com.mikonoma.drivinglog.vehicle.ocr.ScanDraft
import com.mikonoma.drivinglog.vehicle.ocr.ScanEditor
import com.mikonoma.drivinglog.vehicle.ocr.TextRecognizer
import com.mikonoma.drivinglog.vehicle.ocr.detectFuelAmount
import com.mikonoma.drivinglog.vehicle.ocr.detectReadings
import com.mikonoma.drivinglog.vehicle.picture.EventPhotoDraft
import com.mikonoma.drivinglog.vehicle.picture.EventPhotoDraftEditor
import com.mikonoma.drivinglog.vehicle.picture.ImageCodec
import com.mikonoma.drivinglog.vehicle.picture.PhotoResult
import com.mikonoma.drivinglog.vehicle.picture.PictureSize
import com.mikonoma.drivinglog.vehicle.picture.PictureStore
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.Named
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor
import org.fuusio.kide.presentation.async
import org.fuusio.kide.presentation.reduce
import kotlin.time.Clock

class LogEventProcessor @AssistedInject constructor(
    @Assisted private val vehicleId: String,
    private val repository: VehicleRepository,
    private val pictures: PictureStore,
    @Named("event") private val eventPictures: PictureStore,
    private val codec: ImageCodec,
    private val clock: Clock,
    deviceTimeZone: DeviceTimeZone,
    private val recognizer: TextRecognizer,
    captures: CaptureStore,
) : PresentationProcessor<LogEventIntent, LogEventState, LogEventEffect>(openedState(clock, deviceTimeZone, vehicleId, recognizer.isAvailable)) {

    @AssistedFactory
    fun interface Factory {
        fun create(vehicleId: String): LogEventProcessor
    }

    /** True when the form was opened from the Home screen (no vehicle given): it offers the selector and picks a vehicle to start on. False from a vehicle's details screen, which fixes it and never shows the selector. */
    private val chooseVehicle = vehicleId.isEmpty()

    private val photoEditor = EventPhotoDraftEditor(eventPictures, codec)

    private val scanEditor = ScanEditor(codec, recognizer, captures)

    init {
        // The fuel unit/type default to the last one chosen anywhere (add-refueling-logging), loaded once: the
        // reduce below checks fuelPreferencesLoaded itself, so a later emission (or a restored mid-edit choice)
        // is never applied a second time.
        val fuelPreferences = combine(repository.observeLastFuelUnit(), repository.observeLastFuelType()) { unit, type -> unit to type }
        observe("fuel-preferences", fuelPreferences) { (unit, type) ->
            reduce {
                if (fuelPreferencesLoaded) {
                    this
                } else if (fuelTypeFor.isNotEmpty()) {
                    // The fuel type was already resolved — by the vehicle loading, or by a manual choice that
                    // raced ahead of this one-time load — before the remembered preference arrived: never
                    // reconsider it here, only fuelUnit (which has no equivalent per-vehicle filter to race against).
                    copy(fuelUnit = unit ?: fuelUnit, fuelPreferencesLoaded = true)
                } else {
                    // Narrowed to the current vehicle's fuel type filter (vehicle-fuel-type): the remembered choice
                    // when it offers it, else the filter's first entry — the same fallback the vehicle observer
                    // below re-applies whenever the selected vehicle actually changes.
                    val remembered = type ?: fuelType
                    val allowed = vehicleFuelType.allowedFuelTypes()
                    copy(fuelUnit = unit ?: fuelUnit, fuelType = if (remembered in allowed) remembered else allowed.first(), fuelPreferencesLoaded = true)
                }
            }
        }

        if (chooseVehicle) {
            // Every vehicle, for the selector, and the one last logged for, to preselect it. The pick below only replaces the chosen id while
            // nothing has been chosen yet, or the chosen vehicle is gone, so it never overrides a restored choice or one the user just made.
            val vehiclesAndRemembered = combine(repository.observeVehicles(), repository.observeLastLoggedVehicleId()) { vehicles, remembered ->
                vehicles.sortedWith(VehicleNameOrder) to remembered
            }
            observe("vehicles", vehiclesAndRemembered) { (ordered, remembered) ->
                val choices = ordered.map { it.toChoice() }
                reduce {
                    val stillThere = ordered.any { it.id == selectedVehicleId }
                    val next = if (stillThere) {
                        selectedVehicleId
                    } else {
                        (remembered?.let { id -> ordered.firstOrNull { it.id == id } } ?: ordered.firstOrNull())?.id.orEmpty()
                    }
                    copy(vehicles = choices, selectedVehicleId = next)
                }
            }
        }

        // The vehicle and its log follow whichever id is chosen now (the constructor's, when fixed; the selector's choice otherwise), so a
        // change of vehicle needs no second code path. Before a choice has been made (chooseVehicle, still loading), this observes nothing.
        val selected: Flow<Pair<VehicleDetails?, List<VehicleEvent>>> = states.map { it.selectedVehicleId }.distinctUntilChanged()
            .flatMapLatest { id ->
                if (id.isEmpty()) {
                    flowOf(null to emptyList())
                } else {
                    combine(repository.observeVehicle(id), repository.observeLog(id)) { details, log -> details to log }
                }
            }
        observe("vehicle", selected) { (details, log) ->
            if (details == null) {
                if (!chooseVehicle) reduce { copy(isLoading = false, notFound = true) }
                // Choosing, with no id yet (or a vehicle that vanished mid-choice): stay loading until the vehicles observer picks one.
            } else {
                val vehicleUnit = details.vehicle.odometerUnit
                // The family starts as the vehicle's; the tenths choice as the one remembered for it, else the vehicle's own.
                val startUnit = unitOf(vehicleUnit.isMiles, details.vehicle.logDistanceTenths ?: vehicleUnit.hasTenths)
                val vehicleFuelType = details.vehicle.fuelType
                reduce {
                    // The fuel type is re-resolved against the filter of the vehicle now shown, the same way the unit is
                    // re-initialised below (vehicle-fuel-type): kept when still offered, else the filter's first entry.
                    val resolvedFuelType = if (fuelTypeFor == details.vehicle.id) {
                        fuelType
                    } else {
                        val allowed = vehicleFuelType.allowedFuelTypes()
                        if (fuelType in allowed) fuelType else allowed.first()
                    }
                    if (unitFor == details.vehicle.id) {
                        copy(
                            isLoading = false,
                            notFound = false,
                            vehicleUnit = vehicleUnit,
                            vehicleFuelType = vehicleFuelType,
                            log = log,
                            fuelType = resolvedFuelType,
                            fuelTypeFor = details.vehicle.id,
                        )
                    } else {
                        // The unit is (re)initialised from the vehicle now shown; the digits already typed are kept, converted to it,
                        // as when the unit is changed by hand.
                        copy(
                            isLoading = false,
                            notFound = false,
                            vehicleUnit = vehicleUnit,
                            vehicleFuelType = vehicleFuelType,
                            log = log,
                            tripDistance = tripDistance.withUnit(startUnit),
                            newOdometer = newOdometer.withUnit(startUnit),
                            unitFor = details.vehicle.id,
                            fuelType = resolvedFuelType,
                            fuelTypeFor = details.vehicle.id,
                        )
                    }
                }
            }
        }
    }

    private suspend fun Vehicle.toChoice() = VehicleChoice(id, name, licensePlate, type, color, pictureId?.let { pictures.uri(it, PictureSize.SMALL) })

    override suspend fun map(intent: LogEventIntent): Action<LogEventState, LogEventEffect>? = when (intent) {
        is LogEventIntent.WayChanged -> reduce { copy(way = intent.way, error = null) }

        is LogEventIntent.UnitFamilySelected -> reduce { withUnit(unitOf(intent.miles, unit.hasTenths)) }

        is LogEventIntent.TenthsChanged -> reduce { withUnit(unitOf(unit.isMiles, intent.included)) }

        is LogEventIntent.OdometerEdited -> reduce { withActiveEntry(activeEntry.applyEdit(intent.text)) }

        LogEventIntent.OdometerCleared -> reduce { withActiveEntry(activeEntry.clear(), keepError = true) }

        is LogEventIntent.FuelAmountEdited -> reduce { withFuelAmount(fuelAmount.applyEdit(intent.text)) }

        LogEventIntent.FuelAmountCleared -> reduce { withFuelAmount(fuelAmount.clear(), keepError = true) }

        is LogEventIntent.FuelUnitSelected -> reduce { copy(fuelUnit = intent.unit) }

        // Pins fuelTypeFor to the vehicle chosen for, same as a resolved load would: a manual choice is never
        // reconsidered by a same-vehicle resolution that only hasn't run yet (e.g. one still queued behind this
        // dispatch), only by an actual later vehicle change.
        is LogEventIntent.FuelTypeSelected -> reduce { copy(fuelType = intent.type, fuelTypeFor = selectedVehicleId) }

        is LogEventIntent.FilledUpChanged -> reduce { copy(filledUp = intent.checked) }

        is LogEventIntent.DateChanged -> reduce { copy(localDateTime = withDate(localDateTime, intent.date), error = null) }

        is LogEventIntent.TimeChanged -> reduce { copy(localDateTime = withTime(localDateTime, intent.hour, intent.minute), error = null) }

        is LogEventIntent.ZoneChanged -> reduce { copy(zoneId = intent.zoneId, error = null) }

        is LogEventIntent.VehicleSelected -> reduce {
            if (chooseVehicle && vehicles.any { it.id == intent.vehicleId }) copy(selectedVehicleId = intent.vehicleId, error = null) else this
        }

        is LogEventIntent.KindSelected -> reduce { copy(kind = intent.kind, error = null) }

        LogEventIntent.NoteEditorOpened -> reduce { copy(noteDraft = pendingNote ?: "") }

        is LogEventIntent.NoteDraftEdited -> reduce { copy(noteDraft = intent.text) }

        LogEventIntent.NoteAttached -> reduce { copy(pendingNote = noteDraft?.trim()?.ifBlank { null }, noteDraft = null) }

        LogEventIntent.NoteDiscarded -> reduce { copy(noteDraft = null) }

        LogEventIntent.NoteRemoveRequested -> reduce { copy(noteRemovalPending = true) }

        LogEventIntent.NoteRemoveConfirmed -> reduce { copy(pendingNote = null, noteRemovalPending = false) }

        LogEventIntent.NoteRemoveCancelled -> reduce { copy(noteRemovalPending = false) }

        LogEventIntent.LowerOdometerConfirmed -> saveConfirmedLowerOdometer()

        LogEventIntent.LowerOdometerCancelled -> reduce { copy(lowerOdometerConfirmationPending = false) }

        LogEventIntent.PhotoPreviewRefresh -> async("refresh") {
            val previews = photoEditor.previewUris(state.photos)
            val scanUri = scanEditor.reviewPhotoUri(state.scan)
            reduce { copy(photoPreviewUris = previews, scanPhotoUri = scanUri) }
        }

        is LogEventIntent.PhotoPicked -> photoStep { photoEditor.photoPicked(it, intent.result) }

        is LogEventIntent.PhotoRemoveRequested -> reduce { copy(photos = photoEditor.removeRequested(photos, intent.pendingId)) }

        LogEventIntent.PhotoRemoveConfirmed -> photoStep { photoEditor.removeConfirmed(it) }

        LogEventIntent.PhotoRemoveCancelled -> reduce { copy(photos = photoEditor.removeCancelled(photos)) }

        is LogEventIntent.ScanPhotoPicked -> scanPicked(intent.result)

        is LogEventIntent.ScanCandidateSelected -> reduce { copy(scan = scanEditor.selected(scan, intent.index)) }

        LogEventIntent.ScanConfirmed -> scanConfirmed()

        LogEventIntent.ScanCancelled -> scanStep { scanEditor.cancelled(it) }

        LogEventIntent.ScanErrorDismissed -> reduce { copy(scan = scanEditor.errorDismissed(scan)) }

        is LogEventIntent.ScannerOpened -> reduce { copy(scan = scanEditor.scannerOpened(scan), scanTarget = intent.target) }

        LogEventIntent.ScannerClosed -> scanStep { scanEditor.scannerClosed(it) }

        is LogEventIntent.LiveReadingTapped -> async("scan") {
            val next = scanEditor.liveAccepted(state.scan, intent.reading) ?: return@async
            reduce { withScannedReading(intent.reading.detection).copy(scan = next, scanPhotoUri = null) }
        }

        LogEventIntent.Left -> async("leave") {
            photoEditor.discardAll(state.photos)
            scanEditor.discardAll(state.scan)
        }

        LogEventIntent.Save -> save()
    }

    /** Applies a photo-strip change, then rebuilds the thumbnail URIs it means (`add-event-pictures`). */
    private fun photoStep(change: suspend (EventPhotoDraft) -> EventPhotoDraft): Action<LogEventState, LogEventEffect> = async("photo") {
        val next = change(state.photos)
        val previews = photoEditor.previewUris(next)
        reduce { copy(photos = next, photoPreviewUris = previews) }
    }

    /** A fresh live scanner for one opening of the scanner screen (`add-live-scanner`), for whichever field
     * [LogEventState.scanTarget] currently names (`add-fuel-amount-ocr`), reading frames with the same recognizers. */
    fun liveScanner(): LiveScanner = LiveScanner(recognizer, clock, classifierFor(state.scanTarget))

    /** The classify function for [target]: the mileage section's own, against the known odometer at the entry's
     * time, or the fuel amount field's, by label only (`add-fuel-amount-ocr`, design.md). */
    private fun classifierFor(target: ScanTarget): (RecognizedPhoto) -> List<Detection> = when (target) {
        ScanTarget.MILEAGE -> ::classify
        ScanTarget.FUEL_AMOUNT -> ::detectFuelAmount
    }

    /** The log event form's mileage classification: against the known odometer at the entry's time, read afresh for each photo or frame. */
    private fun classify(photo: RecognizedPhoto): List<Detection> = detectReadings(photo, knownOdometerInUnit)

    /** The known odometer at the entry's time in the vehicle's unit, as a scan classifies against it; null when none is known then. */
    val knownOdometerInUnit: Double?
        get() = state.knownOdometer?.let { it.meters / (if (state.unit.isMiles) METERS_PER_MILE else METERS_PER_KILOMETER) }

    /** A photo to scan: classified by whichever field [LogEventState.scanTarget] is currently for, and the review opens. */
    private fun scanPicked(result: PhotoResult): Action<LogEventState, LogEventEffect> = async("scan") {
        reduce { copy(isScanning = true) }
        try {
            val next = scanEditor.photoPicked(state.scan, result, classifierFor(state.scanTarget))
            val uri = scanEditor.reviewPhotoUri(next)
            reduce { copy(scan = next, scanPhotoUri = uri, isScanning = false) }
        } catch (throwable: Throwable) {
            reduce { copy(isScanning = false) }
            throw throwable
        }
    }

    /** Applies a scan change that may close the review, then rebuilds the review photo's URI. */
    private fun scanStep(change: suspend (ScanDraft) -> ScanDraft): Action<LogEventState, LogEventEffect> = async("scan") {
        val next = change(state.scan)
        val uri = scanEditor.reviewPhotoUri(next)
        reduce { copy(scan = next, scanPhotoUri = uri) }
    }

    /** The selected candidate fills the field of the way its kind means, switching the way to it, as if typed there by hand. */
    private fun scanConfirmed(): Action<LogEventState, LogEventEffect> = async("scan") {
        val (next, reading) = scanEditor.confirmed(state.scan) ?: return@async
        reduce { withScannedReading(reading).copy(scan = next, scanPhotoUri = null) }
    }

    private fun save(): Action<LogEventState, LogEventEffect>? {
        val form = state
        val vehicleId = form.selectedVehicleId
        if (form.isSaving || form.isLoading || form.notFound || vehicleId.isEmpty()) return null
        return when (form.kind) {
            LogKind.DISTANCE -> saveDistance(form, vehicleId)
            LogKind.REFUELING -> saveRefueling(form, vehicleId)
        }
    }

    private fun saveDistance(form: LogEventState, vehicleId: String): Action<LogEventState, LogEventEffect>? {
        val moment = form.moment
        return when (
            val result = validateLogDistance(form.way, form.activeEntry, moment, clock.now(), form.knownOdometer, form.mostRecentKnownOdometer)
        ) {
            is LogDistanceResult.Invalid -> reduce { copy(error = result.error) }

            LogDistanceResult.NeedsLowerOdometerConfirmation -> reduce { copy(lowerOdometerConfirmationPending = true) }

            is LogDistanceResult.Valid -> saving {
                repository.addDistanceEntry(
                    vehicleId,
                    moment,
                    result.distance,
                    result.loggedOdometer,
                    tenthsIncluded = form.unit.hasTenths,
                    note = form.pendingNote,
                    photos = photoEditor.toPending(form.photos),
                    capture = form.scan.accepted,
                )
            }

            is LogDistanceResult.Anchor -> saving {
                repository.addOdometerAnchor(
                    vehicleId,
                    moment,
                    result.reading,
                    tenthsIncluded = form.unit.hasTenths,
                    note = form.pendingNote,
                    photos = photoEditor.toPending(form.photos),
                    capture = form.scan.accepted,
                )
            }
        }
    }

    /**
     * A refueling's mileage is optional (`refueling-logging`'s "Mileage is optional for a refueling"): Save is
     * gated only by the fuel amount, so an empty mileage section here means "save with no mileage," not an error —
     * unlike [saveDistance], where an empty active field is itself the [LogDistanceError.FieldEmpty] case.
     */
    private fun saveRefueling(form: LogEventState, vehicleId: String): Action<LogEventState, LogEventEffect>? {
        val moment = form.moment
        val amount = form.fuelAmount.toVolume(form.fuelUnit) ?: return reduce { copy(error = LogDistanceError.FuelAmountEmpty) }
        if (moment.instant > clock.now()) return reduce { copy(error = LogDistanceError.TimeInFuture) }
        if (amount.milliliters <= 0) return reduce { copy(error = LogDistanceError.FuelAmountNotPositive) }
        if (form.activeEntry.isEmpty) {
            return saving {
                repository.addRefueling(
                    vehicleId, moment, amount, form.fuelUnit, form.fuelType, form.filledUp,
                    note = form.pendingNote, photos = photoEditor.toPending(form.photos), capture = form.scan.accepted,
                )
            }
        }
        return when (
            val result = validateLogDistance(form.way, form.activeEntry, moment, clock.now(), form.knownOdometer, form.mostRecentKnownOdometer)
        ) {
            is LogDistanceResult.Invalid -> reduce { copy(error = result.error) }

            LogDistanceResult.NeedsLowerOdometerConfirmation -> reduce { copy(lowerOdometerConfirmationPending = true) }

            is LogDistanceResult.Valid -> saving {
                repository.addRefueling(
                    vehicleId, moment, amount, form.fuelUnit, form.fuelType, form.filledUp,
                    mileage = RefuelingMileage.Added(result.distance, result.loggedOdometer),
                    tenthsIncluded = form.unit.hasTenths, note = form.pendingNote, photos = photoEditor.toPending(form.photos),
                    capture = form.scan.accepted,
                )
            }

            is LogDistanceResult.Anchor -> saving {
                repository.addRefueling(
                    vehicleId, moment, amount, form.fuelUnit, form.fuelType, form.filledUp,
                    mileage = RefuelingMileage.Anchor(result.reading),
                    tenthsIncluded = form.unit.hasTenths, note = form.pendingNote, photos = photoEditor.toPending(form.photos),
                    capture = form.scan.accepted,
                )
            }
        }
    }

    /** Re-validates with confirmation, in case the moment or field changed underneath an open dialog; on anything but
     * an anchor, clears the pending flag and surfaces whatever validation now says instead of saving unexpectedly. */
    private fun saveConfirmedLowerOdometer(): Action<LogEventState, LogEventEffect>? {
        val form = state
        val vehicleId = form.selectedVehicleId
        if (form.isSaving || form.isLoading || form.notFound || vehicleId.isEmpty()) return null
        return when (form.kind) {
            LogKind.DISTANCE -> saveConfirmedLowerOdometerDistance(form, vehicleId)
            LogKind.REFUELING -> saveConfirmedLowerOdometerRefueling(form, vehicleId)
        }
    }

    private fun saveConfirmedLowerOdometerDistance(form: LogEventState, vehicleId: String): Action<LogEventState, LogEventEffect>? {
        val moment = form.moment
        return when (
            val result = validateLogDistance(
                form.way,
                form.activeEntry,
                moment,
                clock.now(),
                form.knownOdometer,
                form.mostRecentKnownOdometer,
                lowerOdometerConfirmed = true,
            )
        ) {
            is LogDistanceResult.Anchor -> saving {
                repository.addOdometerAnchor(
                    vehicleId,
                    moment,
                    result.reading,
                    tenthsIncluded = form.unit.hasTenths,
                    note = form.pendingNote,
                    photos = photoEditor.toPending(form.photos),
                    capture = form.scan.accepted,
                )
            }

            is LogDistanceResult.Invalid -> reduce { copy(lowerOdometerConfirmationPending = false, error = result.error) }

            LogDistanceResult.NeedsLowerOdometerConfirmation, is LogDistanceResult.Valid -> reduce { copy(lowerOdometerConfirmationPending = false) }
        }
    }

    private fun saveConfirmedLowerOdometerRefueling(form: LogEventState, vehicleId: String): Action<LogEventState, LogEventEffect>? {
        val moment = form.moment
        val amount = form.fuelAmount.toVolume(form.fuelUnit)
            ?: return reduce { copy(lowerOdometerConfirmationPending = false, error = LogDistanceError.FuelAmountEmpty) }
        return when (
            val result = validateLogDistance(
                form.way,
                form.activeEntry,
                moment,
                clock.now(),
                form.knownOdometer,
                form.mostRecentKnownOdometer,
                lowerOdometerConfirmed = true,
            )
        ) {
            is LogDistanceResult.Anchor -> saving {
                repository.addRefueling(
                    vehicleId, moment, amount, form.fuelUnit, form.fuelType, form.filledUp,
                    mileage = RefuelingMileage.Anchor(result.reading),
                    tenthsIncluded = form.unit.hasTenths, note = form.pendingNote, photos = photoEditor.toPending(form.photos),
                    capture = form.scan.accepted,
                )
            }

            is LogDistanceResult.Invalid -> reduce { copy(lowerOdometerConfirmationPending = false, error = result.error) }

            LogDistanceResult.NeedsLowerOdometerConfirmation, is LogDistanceResult.Valid -> reduce { copy(lowerOdometerConfirmationPending = false) }
        }
    }

    private fun saving(write: suspend () -> Unit): Action<LogEventState, LogEventEffect> = async("save") {
        // Clearing this here too (besides LowerOdometerCancelled/the defensive branches above) covers the confirmed-anchor
        // path, which reaches this same helper the no-known-odometer anchor path already uses.
        reduce { copy(isSaving = true, error = null, lowerOdometerConfirmationPending = false) }
        try {
            write()
        } catch (throwable: Throwable) {
            // Let the user try again; Kide logs the rethrown error.
            reduce { copy(isSaving = false) }
            throw throwable
        }
        emit(LogEventEffect.Saved)
    }

    private companion object {
        /** The form as it is when opened: the time is now, in the device's zone, the unit a placeholder until the vehicle loads, and the
         * vehicle [vehicleId] when the details screen opened it, or none yet (the selector picks one) when the Home screen did. */
        fun openedState(clock: Clock, deviceTimeZone: DeviceTimeZone, vehicleId: String, canScan: Boolean): LogEventState {
            val zone = deviceTimeZone.current()
            return LogEventState(
                tripDistance = OdometerEntry(OdometerUnit.KILOMETERS),
                newOdometer = OdometerEntry(OdometerUnit.KILOMETERS),
                localDateTime = openedAt(clock.now(), zone),
                zoneId = zone.id,
                selectedVehicleId = vehicleId,
                canScan = canScan,
            )
        }

        const val METERS_PER_KILOMETER = 1000.0
        const val METERS_PER_MILE = 1609.344

        /**
         * The form with [reading] accepted: the way its kind means ("New odometer" for an odometer reading, "Trip distance" for a trip),
         * and that way's field holding its value. A reading with a tenth switches both fields to the unit with tenths (of the same
         * family, as the tenths toggle would); one without keeps the unit, a whole number in a unit with tenths getting a zero tenth.
         */
        fun LogEventState.withScannedReading(reading: Detection): LogEventState {
            val kind = reading.kind ?: return this
            if (kind == ReadingKind.FUEL_AMOUNT) {
                // Always hundredths (refueling-logging's FuelAmountEntry), whatever the reading's own decimal
                // places: a single fraction digit is a tenth, padded with a trailing zero (design.md).
                val hundredths = reading.value.substringAfter('.', "").padEnd(2, '0').take(2).toLong()
                val steps = reading.whole * 100 + hundredths
                if (steps > FuelAmountEntry.MAX_STEPS) return this
                // The label that made this a candidate also says which unit it was in (design.md, "A recognized
                // label also preselects the fuel unit"); kept as it was when the label doesn't say either way.
                return copy(fuelAmount = FuelAmountEntry(steps = steps), fuelUnit = fuelUnitOf(reading.label) ?: fuelUnit, error = null)
            }
            val tenth = reading.tenth
            val form = if (tenth != null && !unit.hasTenths) withUnit(unitOf(unit.isMiles, tenths = true)) else this
            val steps = if (form.unit.hasTenths) reading.whole * 10 + (tenth ?: 0) else reading.whole
            if (steps > form.unit.maxSteps) return this
            val entry = OdometerEntry(form.unit, steps)
            return when (kind) {
                ReadingKind.ODOMETER -> form.copy(way = LogWay.NEW_ODOMETER, newOdometer = entry, error = null)
                ReadingKind.TRIP -> form.copy(way = LogWay.TRIP_DISTANCE, tripDistance = entry, error = null)
                ReadingKind.FUEL_AMOUNT -> form // unreachable: handled above
            }
        }

        /** The fuel unit a recognized volume label implies (`add-fuel-amount-ocr`, design.md's evaluation): liters
         * for every non-English word or symbol actually measured, gallons for the untested English/US hypothesis;
         * null (the form's unit is left as it was) when the label says neither, or there was none. */
        fun fuelUnitOf(label: String?): FuelUnit? = when (label?.uppercase()) {
            "LITRAA", "LITARA", "LITROV", "DM^3", "DM", "L", "LITERS", "LITRES" -> FuelUnit.LITERS
            "GAL", "GALLON", "GALLONS" -> FuelUnit.GALLONS
            else -> null
        }

        /** The four units are the combinations of kilometers or miles, with or without tenths. */
        fun unitOf(miles: Boolean, tenths: Boolean): OdometerUnit = when {
            !miles && !tenths -> OdometerUnit.KILOMETERS
            !miles -> OdometerUnit.KILOMETERS_TENTHS
            !tenths -> OdometerUnit.MILES
            else -> OdometerUnit.MILES_TENTHS
        }

        /** Both fields change unit together, keeping their digits as when adding a vehicle. */
        fun LogEventState.withUnit(newUnit: OdometerUnit): LogEventState =
            copy(tripDistance = tripDistance.withUnit(newUnit), newOdometer = newOdometer.withUnit(newUnit), error = null)

        /** Replaces the active way's entry; a typed digit clears the error, unless [keepError]. */
        fun LogEventState.withActiveEntry(entry: OdometerEntry, keepError: Boolean = false): LogEventState {
            val error = if (keepError || entry.isEmpty) error else null
            return if (way == LogWay.TRIP_DISTANCE) copy(tripDistance = entry, error = error) else copy(newOdometer = entry, error = error)
        }

        /** Replaces the refueling fuel amount entry; a typed digit clears the error, unless [keepError]. */
        fun LogEventState.withFuelAmount(entry: FuelAmountEntry, keepError: Boolean = false): LogEventState {
            val error = if (keepError || entry.isEmpty) error else null
            return copy(fuelAmount = entry, error = error)
        }
    }
}
