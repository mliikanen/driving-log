package com.mikonoma.drivinglog.vehicle.details

import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import org.fuusio.kide.presentation.SideEffect
import org.fuusio.kide.presentation.ViewIntent
import org.fuusio.kide.presentation.ViewState

/**
 * Holds domain values, not formatted text: the screen formats them with the device locale when it is
 * drawn, so a locale change shows the new separators without reloading anything.
 */
data class VehicleDetailsState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val name: String = "",
    val licensePlate: String? = null,
    val unit: OdometerUnit = OdometerUnit.KILOMETERS,
    val currentOdometer: Distance? = null,
    /** At most [VehicleDetailsProcessor.RECENT_EVENT_LIMIT] events, newest first. */
    val recentEvents: List<VehicleEvent> = emptyList(),
) : ViewState

sealed interface VehicleDetailsIntent : ViewIntent {
    data object EditClicked : VehicleDetailsIntent
    data object ViewLogClicked : VehicleDetailsIntent
}

sealed interface VehicleDetailsEffect : SideEffect {
    data class ShowEdit(val vehicleId: String) : VehicleDetailsEffect
    data class ShowLog(val vehicleId: String) : VehicleDetailsEffect
}
