package com.mikonoma.drivinglog.vehicle.edit

import kotlinx.serialization.Serializable
import org.fuusio.kide.presentation.SideEffect
import org.fuusio.kide.presentation.ViewIntent
import org.fuusio.kide.presentation.ViewState

/** The edit form. Only name and plate can be edited: there is no odometer or unit here. */
@Serializable
data class EditVehicleState(
    /** True once the saved values have been copied into the form, so later database changes never overwrite typing. */
    val loaded: Boolean = false,
    val notFound: Boolean = false,
    val name: String = "",
    val licensePlate: String = "",
    val nameError: Boolean = false,
    val isSaving: Boolean = false,
) : ViewState

sealed interface EditVehicleIntent : ViewIntent {
    data class NameChanged(val text: String) : EditVehicleIntent
    data class LicensePlateChanged(val text: String) : EditVehicleIntent
    data object Save : EditVehicleIntent
}

sealed interface EditVehicleEffect : SideEffect {
    /** The changes were saved; go back to the details. */
    data object Saved : EditVehicleEffect
}
