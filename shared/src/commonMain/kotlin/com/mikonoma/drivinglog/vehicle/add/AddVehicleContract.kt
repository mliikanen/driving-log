package com.mikonoma.drivinglog.vehicle.add

import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.input.OdometerEntry
import kotlinx.serialization.Serializable
import org.fuusio.kide.presentation.SideEffect
import org.fuusio.kide.presentation.ViewIntent
import org.fuusio.kide.presentation.ViewState

/** The add form. Serializable so typed text survives process death. */
@Serializable
data class AddVehicleState(
    val name: String = "",
    val licensePlate: String = "",
    /**
     * The odometer unit and the value typed so far; the unit starts at the default for the device region and the
     * entry starts empty.
     */
    val entry: OdometerEntry,
    val nameError: Boolean = false,
    /** Shown when the user tries to save without entering an odometer reading. */
    val odometerError: Boolean = false,
    val isSaving: Boolean = false,
) : ViewState

sealed interface AddVehicleIntent : ViewIntent {
    data class NameChanged(val text: String) : AddVehicleIntent
    data class LicensePlateChanged(val text: String) : AddVehicleIntent
    data class UnitSelected(val unit: OdometerUnit) : AddVehicleIntent

    /** The odometer field's new text from the system keyboard. */
    data class OdometerEdited(val text: String) : AddVehicleIntent
    data object OdometerCleared : AddVehicleIntent
    data object Save : AddVehicleIntent
}

sealed interface AddVehicleEffect : SideEffect {
    /** The vehicle was saved; go back to the list. */
    data object Saved : AddVehicleEffect
}
