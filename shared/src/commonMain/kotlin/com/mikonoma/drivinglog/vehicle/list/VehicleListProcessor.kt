package com.mikonoma.drivinglog.vehicle.list

import com.mikonoma.drivinglog.vehicle.domain.Vehicle
import com.mikonoma.drivinglog.vehicle.domain.VehicleNameOrder
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.picture.PictureSize
import com.mikonoma.drivinglog.vehicle.picture.PictureStore
import dev.zacsweers.metro.Inject
import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor
import org.fuusio.kide.presentation.sideEffect

@Inject
class VehicleListProcessor(
    repository: VehicleRepository,
    private val pictures: PictureStore,
) : PresentationProcessor<VehicleListIntent, VehicleListState, VehicleListEffect>(VehicleListState()) {

    init {
        observe("vehicles", repository.observeVehicles()) { vehicles ->
            val items = vehicles.sortedForList().map { it.toItem() }
            reduce { copy(isLoading = false, vehicles = items) }
        }
    }

    override suspend fun map(intent: VehicleListIntent): Action<VehicleListState, VehicleListEffect>? = when (intent) {
        is VehicleListIntent.OpenVehicle -> sideEffect { VehicleListEffect.ShowDetails(intent.id) }
        VehicleListIntent.AddVehicle -> sideEffect { VehicleListEffect.ShowAdd }
    }

    private suspend fun Vehicle.toItem() =
        VehicleListItem(id, name, licensePlate, pictureId?.let { pictures.uri(it, PictureSize.SMALL) }, type, color)

    private companion object {
        fun List<Vehicle>.sortedForList(): List<Vehicle> = sortedWith(VehicleNameOrder)
    }
}
