package com.mikonoma.drivinglog.vehicle.edit

import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.input.VehicleFieldsResult
import com.mikonoma.drivinglog.vehicle.input.validateVehicleFields
import com.mikonoma.drivinglog.vehicle.picture.ImageCodec
import com.mikonoma.drivinglog.vehicle.picture.PictureDraft
import com.mikonoma.drivinglog.vehicle.picture.PictureDraftEditor
import com.mikonoma.drivinglog.vehicle.picture.PictureEditState
import com.mikonoma.drivinglog.vehicle.picture.PictureError
import com.mikonoma.drivinglog.vehicle.picture.VehiclePictureStore
import com.mikonoma.drivinglog.vehicle.picture.toChange
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor
import org.fuusio.kide.presentation.async
import org.fuusio.kide.presentation.reduce

class EditVehicleProcessor @AssistedInject constructor(
    @Assisted private val vehicleId: String,
    private val repository: VehicleRepository,
    pictures: VehiclePictureStore,
    codec: ImageCodec,
) : PresentationProcessor<EditVehicleIntent, EditVehicleState, EditVehicleEffect>(EditVehicleState()) {

    @AssistedFactory
    fun interface Factory {
        fun create(vehicleId: String): EditVehicleProcessor
    }

    private val editor = PictureDraftEditor(pictures, codec, PictureDraft.Removed)

    init {
        // Copy the saved values in once. A state restored after process death is already loaded and keeps its typing; the saved
        // picture's id is not typing, so it is taken every time.
        observe("vehicle", repository.observeVehicle(vehicleId)) { details ->
            if (details != null) {
                val preview = editor.previewUri(state.picture, details.vehicle.pictureId)
                reduce { copy(savedPictureId = details.vehicle.pictureId, previewUri = preview) }
            }
            if (!state.loaded) {
                if (details == null) {
                    reduce { copy(loaded = true, notFound = true) }
                } else {
                    reduce {
                        copy(loaded = true, name = details.vehicle.name, licensePlate = details.vehicle.licensePlate.orEmpty())
                    }
                }
            }
        }
    }

    override suspend fun map(intent: EditVehicleIntent): Action<EditVehicleState, EditVehicleEffect>? = when (intent) {
        is EditVehicleIntent.NameChanged -> reduce { copy(name = intent.text, nameError = false) }
        is EditVehicleIntent.LicensePlateChanged -> reduce { copy(licensePlate = intent.text) }
        EditVehicleIntent.Save -> save()
        is EditVehicleIntent.PhotoPicked -> pictureStep { editor.photoPicked(it, intent.result) }
        EditVehicleIntent.PictureRefresh -> pictureStep { it }
        is EditVehicleIntent.CropConfirmed -> pictureStep { editor.cropConfirmed(it, intent.crop) }
        EditVehicleIntent.CropCancelled -> pictureStep { editor.cropCancelled(it) }
        EditVehicleIntent.PictureRemoved -> pictureStep { editor.removed(it) }
        EditVehicleIntent.PictureErrorDismissed -> pictureStep { editor.errorDismissed(it) }
        EditVehicleIntent.Left -> async("leave") { editor.discardAll(state.picture) }
    }

    /** Applies a picture change, then rebuilds what is derived from it: the preview and the photo being cropped. */
    private fun pictureStep(change: suspend (PictureEditState) -> PictureEditState): Action<EditVehicleState, EditVehicleEffect> =
        async("picture") {
            var next = change(state.picture)
            var cropImage = if (next.isCropping) editor.cropImage(next) else null
            // A photo that is gone (or cannot be decoded any more) closes the crop with the error instead of leaving it waiting.
            if (next.isCropping && cropImage == null) {
                next = editor.cropCancelled(next).copy(error = PictureError.COULD_NOT_OPEN)
                cropImage = null
            }
            val preview = editor.previewUri(next, state.savedPictureId)
            reduce { copy(picture = next, previewUri = preview, cropImage = cropImage) }
        }

    private fun save(): Action<EditVehicleState, EditVehicleEffect>? {
        if (state.isSaving || !state.loaded || state.notFound) return null
        return when (val result = validateVehicleFields(state.name, state.licensePlate)) {
            VehicleFieldsResult.NameRequired -> reduce { copy(nameError = true) }
            is VehicleFieldsResult.Valid -> async("save") {
                reduce { copy(isSaving = true, nameError = false) }
                try {
                    repository.updateVehicle(vehicleId, result.fields.name, result.fields.licensePlate, state.picture.draft.toChange())
                } catch (throwable: Throwable) {
                    reduce { copy(isSaving = false) }
                    throw throwable
                }
                emit(EditVehicleEffect.Saved)
            }
        }
    }
}
