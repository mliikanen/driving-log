package com.mikonoma.drivinglog.vehicle.add

import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.input.OdometerEntry
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
    val nameError: Boolean = false,
    /** Shown when the user tries to save without entering an odometer reading. */
    val odometerError: Boolean = false,
    val isSaving: Boolean = false,
    /** The picture: the draft, the photo being cropped and whether the last photo could not be opened. Names files, holds no pixels. */
    val picture: PictureEditState = PictureEditState(),
    /** A URI of the small version of the draft picture, for the preview; null for none. Rebuilt from [picture]. */
    @Transient val previewUri: String? = null,
    /** The photo being cropped, decoded for the crop screen. Rebuilt from [picture] after a restore. */
    @Transient val cropImage: DecodedImage? = null,
) : ViewState

sealed interface AddVehicleIntent : ViewIntent {
    data class NameChanged(val text: String) : AddVehicleIntent
    data class LicensePlateChanged(val text: String) : AddVehicleIntent
    data class UnitSelected(val unit: OdometerUnit) : AddVehicleIntent

    /** The odometer field's new text from the system keyboard. */
    data class OdometerEdited(val text: String) : AddVehicleIntent
    data object OdometerCleared : AddVehicleIntent
    data object Save : AddVehicleIntent

    /** The system's chooser of where the photo comes from gave [result]. */
    class PhotoPicked(val result: PhotoResult) : AddVehicleIntent

    /** Rebuilds the preview and the crop image from the picture state, e.g. after a restore. */
    data object PictureRefresh : AddVehicleIntent
    data class CropConfirmed(val crop: CropRect) : AddVehicleIntent
    data object CropCancelled : AddVehicleIntent
    data object PictureRemoved : AddVehicleIntent
    data object PictureErrorDismissed : AddVehicleIntent

    /** The user left the screen without saving: the pending picture files are deleted. */
    data object Left : AddVehicleIntent
}

sealed interface AddVehicleEffect : SideEffect {
    /** The vehicle was saved; go back to the list. */
    data object Saved : AddVehicleEffect
}
