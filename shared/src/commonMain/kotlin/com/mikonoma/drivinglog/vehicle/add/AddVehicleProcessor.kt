package com.mikonoma.drivinglog.vehicle.add

import com.mikonoma.drivinglog.locale.DeviceLocale
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.domain.defaultOdometerUnit
import com.mikonoma.drivinglog.vehicle.input.OdometerEntry
import com.mikonoma.drivinglog.vehicle.input.VehicleFieldsResult
import com.mikonoma.drivinglog.vehicle.input.validateVehicleFields
import dev.zacsweers.metro.Inject
import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor
import org.fuusio.kide.presentation.async
import org.fuusio.kide.presentation.reduce

@Inject
class AddVehicleProcessor(
    private val repository: VehicleRepository,
    deviceLocale: DeviceLocale,
) : PresentationProcessor<AddVehicleIntent, AddVehicleState, AddVehicleEffect>(
    AddVehicleState(entry = OdometerEntry(defaultOdometerUnit(deviceLocale.regionCode))),
) {

    override suspend fun map(intent: AddVehicleIntent): Action<AddVehicleState, AddVehicleEffect>? = when (intent) {
        is AddVehicleIntent.NameChanged -> reduce { copy(name = intent.text, nameError = false) }
        is AddVehicleIntent.LicensePlateChanged -> reduce { copy(licensePlate = intent.text) }
        is AddVehicleIntent.UnitSelected -> reduce { copy(entry = entry.withUnit(intent.unit)) }
        is AddVehicleIntent.OdometerEdited -> reduce { copy(entry = entry.applyEdit(intent.text)) }
        AddVehicleIntent.OdometerCleared -> reduce { copy(entry = entry.clear()) }
        AddVehicleIntent.Save -> save()
    }

    private fun save(): Action<AddVehicleState, AddVehicleEffect>? {
        if (state.isSaving) return null
        return when (val result = validateVehicleFields(state.name, state.licensePlate)) {
            VehicleFieldsResult.NameRequired -> reduce { copy(nameError = true) }
            is VehicleFieldsResult.Valid -> async("save") {
                reduce { copy(isSaving = true, nameError = false) }
                try {
                    val entry = state.entry
                    repository.addVehicle(result.fields.name, result.fields.licensePlate, entry.unit, entry.toDistance())
                } catch (throwable: Throwable) {
                    // Let the user try again; Kide logs the rethrown error.
                    reduce { copy(isSaving = false) }
                    throw throwable
                }
                emit(AddVehicleEffect.Saved)
            }
        }
    }
}
