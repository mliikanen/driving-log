package com.mikonoma.drivinglog.vehicle.log

import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import org.fuusio.kide.presentation.SideEffect
import org.fuusio.kide.presentation.ViewIntent
import org.fuusio.kide.presentation.ViewState

data class VehicleLogState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val vehicleName: String = "",
    val unit: OdometerUnit = OdometerUnit.KILOMETERS,
    /** Every event, newest first. */
    val events: List<VehicleEvent> = emptyList(),
) : ViewState

/** The full log has no actions yet. */
sealed interface VehicleLogIntent : ViewIntent

sealed interface VehicleLogEffect : SideEffect
