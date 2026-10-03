package com.mikonoma.drivinglog.vehicle.add

import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleColors
import com.mikonoma.drivinglog.vehicle.domain.VehicleFuelType
import com.mikonoma.drivinglog.vehicle.domain.VehicleType
import com.mikonoma.drivinglog.vehicle.input.OdometerEntry
import com.mikonoma.drivinglog.vehicle.ocr.LiveReading
import com.mikonoma.drivinglog.vehicle.ocr.ScanDraft
import com.mikonoma.drivinglog.vehicle.picture.CropRect
import com.mikonoma.drivinglog.vehicle.picture.DecodedImage
import com.mikonoma.drivinglog.vehicle.picture.PhotoResult
import com.mikonoma.drivinglog.vehicle.picture.PictureEditState
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.fuusio.kide.presentation.SideEffect
import org.fuusio.kide.presentation.ViewIntent
import org.fuusio.kide.presentation.ViewState

/** The add form. Serializable so typed text survives process death. */
@Serializable
data class AddVehicleState(
    val name: String = "",
    val licensePlate: String = "",
    /**
     * The odometer unit and the value typed so far; the unit starts at the default for the device region and the
     * entry starts empty.
     */
    val entry: OdometerEntry,
    /** The chosen type: Car is preselected, and there is no way to have none, so a saved vehicle always has a type. */
    val type: VehicleType = VehicleType.CAR,
    /** The chosen fuel type (`vehicle-fuel-type`): Petrol is preselected, and there is no way to have none, so a saved vehicle always has one. */
    val fuelType: VehicleFuelType = VehicleFuelType.PETROL,
    /** The chosen color: the default at first, and there is no way to have none, so a saved vehicle always has a color. */
    val color: Rgb = VehicleColors.default,
    /** The color taken from the confirmed crop of the picture, offered as a swatch of its own; null until a crop is confirmed (and after the picture is removed). */
    val pictureColor: Rgb? = null,
    val isSaving: Boolean = false,
    /** The picture: the draft, the photo being cropped and whether the last photo could not be opened. Names files, holds no pixels. */
    val picture: PictureEditState = PictureEditState(),
    /** A URI of the small version of the draft picture, for the preview; null for none. Rebuilt from [picture]. */
    @Transient val previewUri: String? = null,
    /** The photo being cropped, decoded for the crop screen. Rebuilt from [picture] after a restore. */
    @Transient val cropImage: DecodedImage? = null,
    /** The odometer's scan (`scan-initial-odometer`): the live scanner, the photo review and the accepted scan, as on the log event form. */
    val scan: ScanDraft = ScanDraft(),
    /** True while a chosen photo is being recognized. */
    @Transient val isScanning: Boolean = false,
    /** Where the photo review loads the scanned photo from: rebuilt from the capture store, not persisted. */
    @Transient val scanPhotoUri: String? = null,
    /** False on a platform with no text recognizer (iOS): the "Scan a reading" action is not shown. Persisted: set once when the form opens. */
    val canScan: Boolean = false,
) : ViewState

sealed interface AddVehicleIntent : ViewIntent {
    data class NameChanged(val text: String) : AddVehicleIntent
    data class LicensePlateChanged(val text: String) : AddVehicleIntent
    data class UnitSelected(val unit: OdometerUnit) : AddVehicleIntent
    data class TypeSelected(val type: VehicleType) : AddVehicleIntent
    data class FuelTypeSelected(val fuelType: VehicleFuelType) : AddVehicleIntent
    data class ColorSelected(val color: Rgb) : AddVehicleIntent

    /** The odometer field's new text from the system keyboard. */
    data class OdometerEdited(val text: String) : AddVehicleIntent
    data object OdometerCleared : AddVehicleIntent
    data object Save : AddVehicleIntent

    /** The system's chooser of where the photo comes from gave [result]. */
    class PhotoPicked(val result: PhotoResult) : AddVehicleIntent

    /** Rebuilds the preview and the crop image from the picture state, e.g. after a restore. */
    data object PictureRefresh : AddVehicleIntent
    data class CropConfirmed(val crop: CropRect, val quarterTurns: Int = 0) : AddVehicleIntent
    data object CropCancelled : AddVehicleIntent
    data object PictureRemoved : AddVehicleIntent
    data object PictureErrorDismissed : AddVehicleIntent

    /** "Scan a reading" under the odometer, after the camera permission was asked for when needed (`scan-initial-odometer`). */
    data object ScannerOpened : AddVehicleIntent

    /** The live scanner's close action or back: the form as it was, nothing kept. */
    data object ScannerClosed : AddVehicleIntent

    /** A reading was tapped in the live scanner: it becomes the odometer, and its frame is kept. */
    class LiveReadingTapped(val reading: LiveReading) : AddVehicleIntent

    /** The system chooser gave back a photo, or nothing, to scan for the odometer. */
    class ScanPhotoPicked(val result: PhotoResult) : AddVehicleIntent

    /** A candidate was tapped on the photo review. */
    data class ScanCandidateSelected(val index: Int) : AddVehicleIntent

    /** The photo review's "Use": the selected candidate becomes the odometer. */
    data object ScanConfirmed : AddVehicleIntent

    /** Back out of the photo review, or its "Leave": back to the live scanner. */
    data object ScanCancelled : AddVehicleIntent

    data object ScanErrorDismissed : AddVehicleIntent

    /** The user left the screen without saving: the pending picture files are deleted, and any scan's. */
    data object Left : AddVehicleIntent
}

sealed interface AddVehicleEffect : SideEffect {
    /** The vehicle was saved; go back to the list. */
    data object Saved : AddVehicleEffect
}
