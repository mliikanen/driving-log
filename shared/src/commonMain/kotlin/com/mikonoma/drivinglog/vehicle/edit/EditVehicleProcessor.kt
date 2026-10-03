package com.mikonoma.drivinglog.vehicle.edit

import com.mikonoma.drivinglog.util.undoOnFailure
import com.mikonoma.drivinglog.vehicle.color.ColorExtractor
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.input.VehicleFieldsResult
import com.mikonoma.drivinglog.vehicle.input.validateVehicleFields
import com.mikonoma.drivinglog.vehicle.picture.ColorStep
import com.mikonoma.drivinglog.vehicle.picture.ImageCodec
import com.mikonoma.drivinglog.vehicle.picture.PictureDraft
import com.mikonoma.drivinglog.vehicle.picture.PictureDraftEditor
import com.mikonoma.drivinglog.vehicle.picture.PictureEditState
import com.mikonoma.drivinglog.vehicle.picture.PictureError
import com.mikonoma.drivinglog.vehicle.picture.PictureStore
import com.mikonoma.drivinglog.vehicle.picture.toChange
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor
import org.fuusio.kide.presentation.async
import org.fuusio.kide.presentation.reduce

@AssistedInject
class EditVehicleProcessor(
    @Assisted private val vehicleId: String,
    private val repository: VehicleRepository,
    pictures: PictureStore,
    codec: ImageCodec,
    private val colors: ColorExtractor,
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
                        copy(
                            loaded = true,
                            name = details.vehicle.name,
                            licensePlate = details.vehicle.licensePlate.orEmpty(),
                            type = details.vehicle.type,
                            fuelType = details.vehicle.fuelType,
                            color = details.vehicle.color,
                            savedColor = details.vehicle.color,
                        )
                    }
                }
            }
        }
    }

    override suspend fun map(intent: EditVehicleIntent): Action<EditVehicleState, EditVehicleEffect>? = when (intent) {
        is EditVehicleIntent.NameChanged -> reduce { copy(name = intent.text) }
        is EditVehicleIntent.LicensePlateChanged -> reduce { copy(licensePlate = intent.text) }
        is EditVehicleIntent.TypeSelected -> reduce { copy(type = intent.type) }
        is EditVehicleIntent.FuelTypeSelected -> reduce { copy(fuelType = intent.fuelType) }
        is EditVehicleIntent.ColorSelected -> reduce { copy(color = intent.color) }
        EditVehicleIntent.Save -> save()
        is EditVehicleIntent.PhotoPicked -> pictureStep { editor.photoPicked(it, intent.result) }
        EditVehicleIntent.PictureRefresh -> pictureStep { it }
        is EditVehicleIntent.CropConfirmed -> pictureStep(ColorStep.FromConfirmedCrop) { editor.cropConfirmed(it, intent.crop, intent.quarterTurns) }
        EditVehicleIntent.CropCancelled -> pictureStep { editor.cropCancelled(it) }
        EditVehicleIntent.PictureRemoved -> pictureStep(ColorStep.ClearPictureColor) { editor.removed(it) }
        EditVehicleIntent.PictureErrorDismissed -> pictureStep { editor.errorDismissed(it) }
        EditVehicleIntent.Left -> async("leave") { editor.discardAll(state.picture) }
    }

    /** Applies a picture change, then rebuilds what is derived from it: the preview, the photo being cropped and, as [colorStep] says, the color. */
    private fun pictureStep(
        colorStep: ColorStep = ColorStep.Keep,
        change: suspend (PictureEditState) -> PictureEditState,
    ): Action<EditVehicleState, EditVehicleEffect> = async("picture") {
        var next = change(state.picture)
        var cropImage = if (next.isCropping) editor.cropImage(next) else null
        // A photo that is gone (or cannot be decoded any more) closes the crop with the error instead of leaving it waiting.
        if (next.isCropping && cropImage == null) {
            next = editor.cropCancelled(next).copy(error = PictureError.COULD_NOT_OPEN)
            cropImage = null
        }
        val preview = editor.previewUri(next, state.savedPictureId)
        // A confirmed crop gives the color of its picture (null: the picture has none, so the color stays as it was).
        val confirmed = colorStep == ColorStep.FromConfirmedCrop && next.error == null && next.draft is PictureDraft.Pending
        val extracted = if (confirmed) editor.sampleColor(next, colors) else null
        reduce {
            copy(
                picture = next,
                previewUri = preview,
                cropImage = cropImage,
                pictureColor = when {
                    confirmed -> extracted
                    colorStep == ColorStep.ClearPictureColor -> null
                    else -> pictureColor
                },
                color = extracted ?: color,
            )
        }
    }

    private fun save(): Action<EditVehicleState, EditVehicleEffect>? {
        if (state.isSaving || !state.loaded || state.notFound) return null
        // A loaded vehicle always has a type, a fuel type and a color; without them there is nothing to save.
        val type = state.type ?: return null
        val fuelType = state.fuelType ?: return null
        val color = state.color ?: return null
        // disable-invalid-save: the screen disables Save while the name is blank, so NameRequired here is a
        // defense-in-depth no-op against a direct dispatch, not a path a tap can reach.
        return when (val result = validateVehicleFields(state.name, state.licensePlate)) {
            VehicleFieldsResult.NameRequired -> null

            is VehicleFieldsResult.Valid -> async("save") {
                reduce { copy(isSaving = true) }
                undoOnFailure(
                    undo = {
                        reduce { copy(isSaving = false) }
                    },
                ) {
                    repository.updateVehicle(vehicleId, result.fields.name, result.fields.licensePlate, type, color, state.picture.draft.toChange(), fuelType)
                }
                emit(EditVehicleEffect.Saved)
            }
        }
    }
}
