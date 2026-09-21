package com.mikonoma.drivinglog.vehicle.add

import com.mikonoma.drivinglog.locale.DeviceLocale
import com.mikonoma.drivinglog.vehicle.color.ColorExtractor
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.domain.defaultOdometerUnit
import com.mikonoma.drivinglog.vehicle.input.OdometerEntry
import com.mikonoma.drivinglog.vehicle.input.VehicleFieldsResult
import com.mikonoma.drivinglog.vehicle.input.validateVehicleFields
import com.mikonoma.drivinglog.vehicle.picture.ColorStep
import com.mikonoma.drivinglog.vehicle.picture.ImageCodec
import com.mikonoma.drivinglog.vehicle.picture.PictureDraft
import com.mikonoma.drivinglog.vehicle.picture.PictureDraftEditor
import com.mikonoma.drivinglog.vehicle.picture.PictureEditState
import com.mikonoma.drivinglog.vehicle.picture.PictureError
import com.mikonoma.drivinglog.vehicle.picture.VehiclePictureStore
import com.mikonoma.drivinglog.vehicle.picture.forAdd
import dev.zacsweers.metro.Inject
import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor
import org.fuusio.kide.presentation.async
import org.fuusio.kide.presentation.reduce

@Inject
class AddVehicleProcessor(
    private val repository: VehicleRepository,
    deviceLocale: DeviceLocale,
    pictures: VehiclePictureStore,
    codec: ImageCodec,
    private val colors: ColorExtractor,
) : PresentationProcessor<AddVehicleIntent, AddVehicleState, AddVehicleEffect>(
    AddVehicleState(entry = OdometerEntry(defaultOdometerUnit(deviceLocale.regionCode))),
) {

    private val editor = PictureDraftEditor(pictures, codec, PictureDraft.None)

    override suspend fun map(intent: AddVehicleIntent): Action<AddVehicleState, AddVehicleEffect>? = when (intent) {
        is AddVehicleIntent.NameChanged -> reduce { copy(name = intent.text, nameError = false) }
        is AddVehicleIntent.LicensePlateChanged -> reduce { copy(licensePlate = intent.text) }
        is AddVehicleIntent.UnitSelected -> reduce { copy(entry = entry.withUnit(intent.unit)) }
        is AddVehicleIntent.TypeSelected -> reduce { copy(type = intent.type) }
        is AddVehicleIntent.ColorSelected -> reduce { copy(color = intent.color) }
        is AddVehicleIntent.OdometerEdited -> reduce {
            val edited = entry.applyEdit(intent.text)
            copy(entry = edited, odometerError = odometerError && edited.isEmpty)
        }
        AddVehicleIntent.OdometerCleared -> reduce { copy(entry = entry.clear()) }
        AddVehicleIntent.Save -> save()
        is AddVehicleIntent.PhotoPicked -> pictureStep { editor.photoPicked(it, intent.result) }
        AddVehicleIntent.PictureRefresh -> pictureStep { it }
        is AddVehicleIntent.CropConfirmed -> pictureStep(ColorStep.FromConfirmedCrop) { editor.cropConfirmed(it, intent.crop) }
        AddVehicleIntent.CropCancelled -> pictureStep { editor.cropCancelled(it) }
        AddVehicleIntent.PictureRemoved -> pictureStep(ColorStep.ClearPictureColor) { editor.removed(it) }
        AddVehicleIntent.PictureErrorDismissed -> pictureStep { editor.errorDismissed(it) }
        AddVehicleIntent.Left -> async("leave") { editor.discardAll(state.picture) }
    }

    /** Applies a picture change, then rebuilds what is derived from it: the preview, the photo being cropped and, as [colorStep] says, the color. */
    private fun pictureStep(colorStep: ColorStep = ColorStep.Keep, change: suspend (PictureEditState) -> PictureEditState): Action<AddVehicleState, AddVehicleEffect> =
        async("picture") {
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

    private fun save(): Action<AddVehicleState, AddVehicleEffect>? {
        if (state.isSaving) return null
        val fields = validateVehicleFields(state.name, state.licensePlate)
        val initialOdometer = state.entry.toDistance()
        // Every error is shown together, so the user sees everything that is missing at once.
        if (fields !is VehicleFieldsResult.Valid || initialOdometer == null) {
            return reduce {
                copy(
                    nameError = fields is VehicleFieldsResult.NameRequired,
                    odometerError = initialOdometer == null,
                )
            }
        }
        return async("save") {
            reduce { copy(isSaving = true, nameError = false, odometerError = false) }
            try {
                repository.addVehicle(
                    fields.fields.name, fields.fields.licensePlate, state.type, state.color, state.entry.unit, initialOdometer, state.picture.draft.forAdd(),
                )
            } catch (throwable: Throwable) {
                // Let the user try again; Kide logs the rethrown error.
                reduce { copy(isSaving = false) }
                throw throwable
            }
            emit(AddVehicleEffect.Saved)
        }
    }
}
