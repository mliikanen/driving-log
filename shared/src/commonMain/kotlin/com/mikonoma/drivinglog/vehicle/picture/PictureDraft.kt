package com.mikonoma.drivinglog.vehicle.picture

import com.mikonoma.drivinglog.vehicle.domain.PendingPicture
import com.mikonoma.drivinglog.vehicle.domain.PictureChange
import kotlinx.serialization.Serializable

/**
 * What the form holds as the vehicle's picture. It only names files (by pending id), never holds pixels, so it survives rotation
 * and process death as part of the serializable form state.
 */
@Serializable
sealed interface PictureDraft {
    /** Adding: the vehicle has no picture. */
    @Serializable
    data object None : PictureDraft

    /** Editing: the vehicle keeps the picture it has (or none). */
    @Serializable
    data object Unchanged : PictureDraft

    /** Editing: the picture is removed when the vehicle is saved. */
    @Serializable
    data object Removed : PictureDraft

    /** A cropped picture waiting in the pending area for the vehicle to be saved. */
    @Serializable
    data class Pending(val pendingId: String) : PictureDraft
}

/** Why the last attempt to get a picture did not work; shown under the preview until the next action. */
@Serializable
enum class PictureError {
    /** The photo could not be read as an image, or was too large. */
    COULD_NOT_OPEN,

    /** The user has not allowed the camera. */
    CAMERA_DENIED,
}

/**
 * The picture part of the add and edit forms: the [draft], the photo being cropped ([cropSourceId], the pending id of the chosen
 * photo, while the crop screen is open) and why the last attempt failed, if it did ([error]).
 */
@Serializable
data class PictureEditState(
    val draft: PictureDraft = PictureDraft.None,
    val cropSourceId: String? = null,
    val error: PictureError? = null,
) {
    val isCropping: Boolean get() = cropSourceId != null
}

/** The picture an added vehicle is saved with. */
fun PictureDraft.forAdd(): PendingPicture? = (this as? PictureDraft.Pending)?.let { PendingPicture(it.pendingId) }

/** What saving an edit does to the picture. */
fun PictureDraft.toChange(): PictureChange = when (this) {
    is PictureDraft.Pending -> PictureChange.Replace(PendingPicture(pendingId))
    PictureDraft.Removed -> PictureChange.Remove
    PictureDraft.Unchanged, PictureDraft.None -> PictureChange.Keep
}
