package com.mikonoma.drivinglog.vehicle.edit

import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.input.VehicleFieldsResult
import com.mikonoma.drivinglog.vehicle.input.validateVehicleFields
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
) : PresentationProcessor<EditVehicleIntent, EditVehicleState, EditVehicleEffect>(EditVehicleState()) {

    @AssistedFactory
    fun interface Factory {
        fun create(vehicleId: String): EditVehicleProcessor
    }

    init {
        // Copy the saved values in once. A state restored after process death is already loaded and keeps its typing.
        observe("vehicle", repository.observeVehicle(vehicleId)) { details ->
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
    }

    private fun save(): Action<EditVehicleState, EditVehicleEffect>? {
        if (state.isSaving || !state.loaded || state.notFound) return null
        return when (val result = validateVehicleFields(state.name, state.licensePlate)) {
            VehicleFieldsResult.NameRequired -> reduce { copy(nameError = true) }
            is VehicleFieldsResult.Valid -> async("save") {
                reduce { copy(isSaving = true, nameError = false) }
                try {
                    repository.updateVehicle(vehicleId, result.fields.name, result.fields.licensePlate)
                } catch (throwable: Throwable) {
                    reduce { copy(isSaving = false) }
                    throw throwable
                }
                emit(EditVehicleEffect.Saved)
            }
        }
    }
}
