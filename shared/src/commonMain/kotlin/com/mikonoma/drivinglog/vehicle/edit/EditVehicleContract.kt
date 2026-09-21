package com.mikonoma.drivinglog.vehicle.edit

import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleType
import com.mikonoma.drivinglog.vehicle.picture.CropRect
import com.mikonoma.drivinglog.vehicle.picture.DecodedImage
import com.mikonoma.drivinglog.vehicle.picture.PhotoResult
import com.mikonoma.drivinglog.vehicle.picture.PictureDraft
import com.mikonoma.drivinglog.vehicle.picture.PictureEditState
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.fuusio.kide.presentation.SideEffect
import org.fuusio.kide.presentation.ViewIntent
import org.fuusio.kide.presentation.ViewState

/** The edit form. The name, plate, type, color and picture can be edited: there is no odometer or unit here. */
@Serializable
data class EditVehicleState(
    /** True once the saved values have been copied into the form, so later database changes never overwrite typing. */
    val loaded: Boolean = false,
    val notFound: Boolean = false,
    val name: String = "",
    val licensePlate: String = "",
    /** The vehicle's type: null only until the saved vehicle has loaded, its saved type from then on. It can be changed but never cleared. */
    val type: VehicleType? = null,
    /** The vehicle's color: null only until the saved vehicle has loaded, its saved color from then on. It can be changed but never cleared. */
    val color: Rgb? = null,
    /** The color taken from a crop confirmed in this form, offered as a swatch of its own; null until then (and after the picture is removed). */
    val pictureColor: Rgb? = null,
    val nameError: Boolean = false,
    val isSaving: Boolean = false,
    /** The picture: [PictureDraft.Unchanged] until the user changes or removes it. Names files, holds no pixels. */
    val picture: PictureEditState = PictureEditState(draft = PictureDraft.Unchanged),
    /** The id of the vehicle's saved picture, for the preview. Rebuilt from the repository. */
    @Transient val savedPictureId: String? = null,
    /** A URI of the small version of the picture the form shows; null for none. Rebuilt from [picture]. */
    @Transient val previewUri: String? = null,
    /** The photo being cropped, decoded for the crop screen. Rebuilt from [picture] after a restore. */
    @Transient val cropImage: DecodedImage? = null,
) : ViewState

sealed interface EditVehicleIntent : ViewIntent {
    data class NameChanged(val text: String) : EditVehicleIntent
    data class LicensePlateChanged(val text: String) : EditVehicleIntent
    data class TypeSelected(val type: VehicleType) : EditVehicleIntent
    data class ColorSelected(val color: Rgb) : EditVehicleIntent
    data object Save : EditVehicleIntent

    /** The system's chooser of where the photo comes from gave [result]. */
    class PhotoPicked(val result: PhotoResult) : EditVehicleIntent

    /** Rebuilds the preview and the crop image from the picture state, e.g. after a restore. */
    data object PictureRefresh : EditVehicleIntent
    data class CropConfirmed(val crop: CropRect) : EditVehicleIntent
    data object CropCancelled : EditVehicleIntent
    data object PictureRemoved : EditVehicleIntent
    data object PictureErrorDismissed : EditVehicleIntent

    /** The user left the screen without saving: the pending picture files are deleted. */
    data object Left : EditVehicleIntent
}

sealed interface EditVehicleEffect : SideEffect {
    /** The changes were saved; go back to the details. */
    data object Saved : EditVehicleEffect
}
