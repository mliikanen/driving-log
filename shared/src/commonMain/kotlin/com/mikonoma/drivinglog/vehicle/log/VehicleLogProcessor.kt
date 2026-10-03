package com.mikonoma.drivinglog.vehicle.log

import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.combine
import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor

@AssistedInject
class VehicleLogProcessor(@Assisted vehicleId: String, repository: VehicleRepository) :
    PresentationProcessor<VehicleLogIntent, VehicleLogState, VehicleLogEffect>(VehicleLogState()) {

    @AssistedFactory
    fun interface Factory {
        fun create(vehicleId: String): VehicleLogProcessor
    }

    init {
        val log = combine(repository.observeVehicle(vehicleId), repository.observeLog(vehicleId)) { details, events ->
            details to events
        }
        observe("log", log) { (details, events) ->
            if (details == null) {
                reduce { copy(isLoading = false, notFound = true) }
            } else {
                reduce {
                    copy(
                        isLoading = false,
                        notFound = false,
                        vehicleName = details.vehicle.name,
                        unit = details.vehicle.odometerUnit,
                        events = events,
                    )
                }
            }
        }
    }

    override suspend fun map(intent: VehicleLogIntent): Action<VehicleLogState, VehicleLogEffect>? = null
}
