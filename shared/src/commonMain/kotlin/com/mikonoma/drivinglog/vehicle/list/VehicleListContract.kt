package com.mikonoma.drivinglog.vehicle.list

import org.fuusio.kide.presentation.SideEffect
import org.fuusio.kide.presentation.ViewIntent
import org.fuusio.kide.presentation.ViewState

data class VehicleListItem(val id: String, val name: String, val licensePlate: String?)

data class VehicleListState(
    /** True until the first load, so the empty state is not flashed before the vehicles arrive. */
    val isLoading: Boolean = true,
    val vehicles: List<VehicleListItem> = emptyList(),
) : ViewState

sealed interface VehicleListIntent : ViewIntent {
    data class OpenVehicle(val id: String) : VehicleListIntent
    data object AddVehicle : VehicleListIntent
}

sealed interface VehicleListEffect : SideEffect {
    data class ShowDetails(val id: String) : VehicleListEffect
    data object ShowAdd : VehicleListEffect
}
