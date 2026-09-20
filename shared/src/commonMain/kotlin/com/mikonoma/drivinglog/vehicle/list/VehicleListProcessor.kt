package com.mikonoma.drivinglog.vehicle.list

import com.mikonoma.drivinglog.vehicle.domain.Vehicle
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import dev.zacsweers.metro.Inject
import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor
import org.fuusio.kide.presentation.sideEffect

@Inject
class VehicleListProcessor(
    repository: VehicleRepository,
) : PresentationProcessor<VehicleListIntent, VehicleListState, VehicleListEffect>(VehicleListState()) {

    init {
        observe("vehicles", repository.observeVehicles()) { vehicles ->
            reduce { copy(isLoading = false, vehicles = vehicles.sortedForList().map { it.toItem() }) }
        }
    }

    override suspend fun map(intent: VehicleListIntent): Action<VehicleListState, VehicleListEffect>? = when (intent) {
        is VehicleListIntent.OpenVehicle -> sideEffect { VehicleListEffect.ShowDetails(intent.id) }
        VehicleListIntent.AddVehicle -> sideEffect { VehicleListEffect.ShowAdd }
    }

    private companion object {
        /** By name without regard to letter case (SQLite's NOCASE only folds ASCII), then by when it was added. */
        fun List<Vehicle>.sortedForList(): List<Vehicle> =
            sortedWith(compareBy<Vehicle>({ it.name.lowercase() }, { it.createdAt }))

        fun Vehicle.toItem() = VehicleListItem(id, name, licensePlate)
    }
}
