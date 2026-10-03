package com.mikonoma.drivinglog.vehicle.add

import com.mikonoma.drivinglog.locale.DeviceLocale
import com.mikonoma.drivinglog.util.undoOnFailure
import com.mikonoma.drivinglog.vehicle.color.ColorExtractor
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.domain.defaultOdometerUnit
import com.mikonoma.drivinglog.vehicle.input.OdometerEntry
import com.mikonoma.drivinglog.vehicle.input.VehicleFieldsResult
import com.mikonoma.drivinglog.vehicle.input.validateVehicleFields
import com.mikonoma.drivinglog.vehicle.ocr.CaptureStore
import com.mikonoma.drivinglog.vehicle.ocr.Detection
import com.mikonoma.drivinglog.vehicle.ocr.LiveScanner
import com.mikonoma.drivinglog.vehicle.ocr.ScanDraft
import com.mikonoma.drivinglog.vehicle.ocr.ScanEditor
import com.mikonoma.drivinglog.vehicle.ocr.TextRecognizer
import com.mikonoma.drivinglog.vehicle.ocr.detectInitialOdometer
import com.mikonoma.drivinglog.vehicle.picture.ColorStep
import com.mikonoma.drivinglog.vehicle.picture.ImageCodec
import com.mikonoma.drivinglog.vehicle.picture.PhotoResult
import com.mikonoma.drivinglog.vehicle.picture.PictureDraft
import com.mikonoma.drivinglog.vehicle.picture.PictureDraftEditor
import com.mikonoma.drivinglog.vehicle.picture.PictureEditState
import com.mikonoma.drivinglog.vehicle.picture.PictureError
import com.mikonoma.drivinglog.vehicle.picture.PictureStore
import com.mikonoma.drivinglog.vehicle.picture.forAdd
import dev.zacsweers.metro.Inject
import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor
import org.fuusio.kide.presentation.async
import org.fuusio.kide.presentation.reduce
import kotlin.time.Clock

@Inject
class AddVehicleProcessor(
    private val repository: VehicleRepository,
    deviceLocale: DeviceLocale,
    pictures: PictureStore,
    codec: ImageCodec,
    private val colors: ColorExtractor,
    private val recognizer: TextRecognizer,
    captures: CaptureStore,
    private val clock: Clock,
) : PresentationProcessor<AddVehicleIntent, AddVehicleState, AddVehicleEffect>(
    AddVehicleState(entry = OdometerEntry(defaultOdometerUnit(deviceLocale.regionCode)), canScan = recognizer.isAvailable),
) {

    private val editor = PictureDraftEditor(pictures, codec, PictureDraft.None)

    private val scanEditor = ScanEditor(codec, recognizer, captures)

    override suspend fun map(intent: AddVehicleIntent): Action<AddVehicleState, AddVehicleEffect>? = when (intent) {
        is AddVehicleIntent.NameChanged -> reduce { copy(name = intent.text) }

        is AddVehicleIntent.LicensePlateChanged -> reduce { copy(licensePlate = intent.text) }

        is AddVehicleIntent.UnitSelected -> reduce { copy(entry = entry.withUnit(intent.unit)) }

        is AddVehicleIntent.TypeSelected -> reduce { copy(type = intent.type) }

        is AddVehicleIntent.FuelTypeSelected -> reduce { copy(fuelType = intent.fuelType) }

        is AddVehicleIntent.ColorSelected -> reduce { copy(color = intent.color) }

        is AddVehicleIntent.OdometerEdited -> reduce { copy(entry = entry.applyEdit(intent.text)) }

        AddVehicleIntent.OdometerCleared -> reduce { copy(entry = entry.clear()) }

        AddVehicleIntent.Save -> save()

        is AddVehicleIntent.PhotoPicked -> pictureStep { editor.photoPicked(it, intent.result) }

        AddVehicleIntent.PictureRefresh -> pictureStep { it }

        is AddVehicleIntent.CropConfirmed -> pictureStep(ColorStep.FromConfirmedCrop) { editor.cropConfirmed(it, intent.crop, intent.quarterTurns) }

        AddVehicleIntent.CropCancelled -> pictureStep { editor.cropCancelled(it) }

        AddVehicleIntent.PictureRemoved -> pictureStep(ColorStep.ClearPictureColor) { editor.removed(it) }

        AddVehicleIntent.PictureErrorDismissed -> pictureStep { editor.errorDismissed(it) }

        AddVehicleIntent.ScannerOpened -> reduce { copy(scan = scanEditor.scannerOpened(scan)) }

        AddVehicleIntent.ScannerClosed -> scanStep { scanEditor.scannerClosed(it) }

        is AddVehicleIntent.LiveReadingTapped -> async("scan") {
            val next = scanEditor.liveAccepted(state.scan, intent.reading) ?: return@async
            reduce { withScannedOdometer(intent.reading.detection).copy(scan = next, scanPhotoUri = null) }
        }

        is AddVehicleIntent.ScanPhotoPicked -> scanPicked(intent.result)

        is AddVehicleIntent.ScanCandidateSelected -> reduce { copy(scan = scanEditor.selected(scan, intent.index)) }

        AddVehicleIntent.ScanConfirmed -> async("scan") {
            val (next, reading) = scanEditor.confirmed(state.scan) ?: return@async
            reduce { withScannedOdometer(reading).copy(scan = next, scanPhotoUri = null) }
        }

        AddVehicleIntent.ScanCancelled -> scanStep { scanEditor.cancelled(it) }

        AddVehicleIntent.ScanErrorDismissed -> reduce { copy(scan = scanEditor.errorDismissed(scan)) }

        AddVehicleIntent.Left -> async("leave") {
            editor.discardAll(state.picture)
            scanEditor.discardAll(state.scan)
        }
    }

    /** A fresh live scanner for one opening of the scanner, offering readings as a new vehicle's odometer (`scan-initial-odometer`). */
    fun liveScanner(): LiveScanner = LiveScanner(recognizer, clock, ::detectInitialOdometer)

    /** A photo to scan for the odometer: recognized and classified as a new vehicle's odometer, and the review opens. */
    private fun scanPicked(result: PhotoResult): Action<AddVehicleState, AddVehicleEffect> = async("scan") {
        reduce { copy(isScanning = true) }
        undoOnFailure(
            undo = {
                reduce { copy(isScanning = false) }
            },
        ) {
            val next = scanEditor.photoPicked(state.scan, result, ::detectInitialOdometer)
            val uri = scanEditor.reviewPhotoUri(next)
            reduce { copy(scan = next, scanPhotoUri = uri, isScanning = false) }
        }
    }

    /** Applies a scan change that may close the review, then rebuilds the review photo's URI. */
    private fun scanStep(change: suspend (ScanDraft) -> ScanDraft): Action<AddVehicleState, AddVehicleEffect> = async("scan") {
        val next = change(state.scan)
        val uri = scanEditor.reviewPhotoUri(next)
        reduce { copy(scan = next, scanPhotoUri = uri) }
    }

    /**
     * Applies a picture change, then rebuilds what is derived from it: the preview, the photo being cropped and, as [colorStep] says, the
     * color; and the scanned photo's URI, which a restored form (`PictureRefresh`) needs as much as the picture's.
     */
    private fun pictureStep(
        colorStep: ColorStep = ColorStep.Keep,
        change: suspend (PictureEditState) -> PictureEditState,
    ): Action<AddVehicleState, AddVehicleEffect> = async("picture") {
        var next = change(state.picture)
        var cropImage = if (next.isCropping) editor.cropImage(next) else null
        // A photo that is gone (or cannot be decoded any more) closes the crop with the error instead of leaving it waiting.
        if (next.isCropping && cropImage == null) {
            next = editor.cropCancelled(next).copy(error = PictureError.COULD_NOT_OPEN)
            cropImage = null
        }
        val preview = editor.previewUri(next, savedPictureId = null)
        // A confirmed crop gives the color of its picture (null: the picture has none, so the color stays as it was).
        val confirmed = colorStep == ColorStep.FromConfirmedCrop && next.error == null && next.draft is PictureDraft.Pending
        val extracted = if (confirmed) editor.sampleColor(next, colors) else null
        val scanUri = scanEditor.reviewPhotoUri(state.scan)
        reduce {
            copy(
                picture = next,
                previewUri = preview,
                cropImage = cropImage,
                scanPhotoUri = scanUri,
                pictureColor = when {
                    confirmed -> extracted
                    colorStep == ColorStep.ClearPictureColor -> null
                    else -> pictureColor
                },
                color = extracted ?: color,
            )
        }
    }

    private fun save(): Action<AddVehicleState, AddVehicleEffect>? {
        if (state.isSaving) return null
        val fields = validateVehicleFields(state.name, state.licensePlate)
        val initialOdometer = state.entry.toDistance()
        // disable-invalid-save: the screen disables Save while either is missing, so this is a defense-in-depth
        // no-op against a direct dispatch, not a path a tap can reach.
        if (fields !is VehicleFieldsResult.Valid || initialOdometer == null) return null
        return async("save") {
            reduce { copy(isSaving = true) }
            undoOnFailure(
                undo = {
                    // Let the user try again; Kide logs the rethrown error.
                    reduce { copy(isSaving = false) }
                },
            ) {
                repository.addVehicle(
                    fields.fields.name, fields.fields.licensePlate, state.type, state.color, state.entry.unit, initialOdometer, state.picture.draft.forAdd(),
                    capture = state.scan.accepted, fuelType = state.fuelType,
                )
            }
            emit(AddVehicleEffect.Saved)
        }
    }

    private companion object {
        /**
         * The form with [reading] as the odometer (`scan-initial-odometer`): a reading with a tenth switches the unit to the one with
         * tenths of the same family (kilometers or miles), never the family itself; a whole reading in a unit with tenths gets a zero
         * tenth. A reading the field cannot hold leaves the form as it was.
         */
        fun AddVehicleState.withScannedOdometer(reading: Detection): AddVehicleState {
            val tenth = reading.tenth
            val unit = if (tenth != null && !entry.unit.hasTenths) entry.unit.withTenths() else entry.unit
            val steps = if (unit.hasTenths) reading.whole * 10 + (tenth ?: 0) else reading.whole
            if (steps > unit.maxSteps) return this
            return copy(entry = OdometerEntry(unit, steps))
        }

        fun OdometerUnit.withTenths(): OdometerUnit = if (isMiles) OdometerUnit.MILES_TENTHS else OdometerUnit.KILOMETERS_TENTHS
    }
}
