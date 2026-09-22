package com.mikonoma.drivinglog.vehicle.details

import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleColors
import com.mikonoma.drivinglog.vehicle.domain.VehicleType
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
    /** Where the large version of the vehicle's picture can be loaded from, or null when it has none (or its file is gone). */
    val pictureUri: String? = null,
    /** The vehicle's type, whose icon stands in for a missing picture (a placeholder until the vehicle has loaded). */
    val type: VehicleType = VehicleType.CAR,
    /** The vehicle's color, which its icon is drawn from (the default until the vehicle has loaded). */
    val color: Rgb = VehicleColors.default,
    val unit: OdometerUnit = OdometerUnit.KILOMETERS,
    val currentOdometer: Distance? = null,
    /** At most [VehicleDetailsProcessor.RECENT_EVENT_LIMIT] events, newest first. */
    val recentEvents: List<VehicleEvent> = emptyList(),
) : ViewState

sealed interface VehicleDetailsIntent : ViewIntent {
    data object EditClicked : VehicleDetailsIntent
    data object ViewLogClicked : VehicleDetailsIntent
    data object LogEventClicked : VehicleDetailsIntent
}

sealed interface VehicleDetailsEffect : SideEffect {
    data class ShowEdit(val vehicleId: String) : VehicleDetailsEffect
    data class ShowLog(val vehicleId: String) : VehicleDetailsEffect
    data class ShowLogEvent(val vehicleId: String) : VehicleDetailsEffect
}
