package com.mikonoma.drivinglog.landing

import org.fuusio.kide.presentation.SideEffect
import org.fuusio.kide.presentation.ViewIntent
import org.fuusio.kide.presentation.ViewState

/** The four actions of the Home screen, in the order of the grid (row by row). */
enum class LandingTileId { VEHICLES, LOG_EVENT, TRIP, PLACEHOLDER }

/** Which icon a tile draws (the screen maps it to the vector). */
enum class LandingIcon { VEHICLES, ADD_VEHICLE, LOG_EVENT, TRIP, PLACEHOLDER }

/** One action of the grid. [label] is empty for the first tile until it is known which of its two forms it takes; a tile that is not [enabled] is drawn and exposed as disabled. */
data class LandingTile(val id: LandingTileId, val label: String, val icon: LandingIcon, val enabled: Boolean)

/**
 * The tiles of the Home screen for a user who has or has not any vehicle. The first tile opens the vehicles, or, when there are none, is "Add vehicle"; it is
 * neither until the vehicles have loaded, so the second form is never shown to a user who has vehicles. The other three are placeholders for the changes that
 * build them and are not enabled yet.
 */
fun landingTiles(hasVehicles: Boolean, isLoading: Boolean): List<LandingTile> = listOf(
    when {
        isLoading -> LandingTile(LandingTileId.VEHICLES, "", LandingIcon.VEHICLES, enabled = false)
        hasVehicles -> LandingTile(LandingTileId.VEHICLES, "Vehicles", LandingIcon.VEHICLES, enabled = true)
        else -> LandingTile(LandingTileId.VEHICLES, "Add vehicle", LandingIcon.ADD_VEHICLE, enabled = true)
    },
    LandingTile(LandingTileId.LOG_EVENT, "Log event", LandingIcon.LOG_EVENT, enabled = false),
    LandingTile(LandingTileId.TRIP, "Trip", LandingIcon.TRIP, enabled = false),
    LandingTile(LandingTileId.PLACEHOLDER, "Placeholder", LandingIcon.PLACEHOLDER, enabled = false),
)

data class LandingState(
    /** True until the first load. */
    val isLoading: Boolean = true,
    val hasVehicles: Boolean = false,
) : ViewState {
    val tiles: List<LandingTile> get() = landingTiles(hasVehicles, isLoading)
}

sealed interface LandingIntent : ViewIntent {
    data object OpenVehicles : LandingIntent
    data object AddVehicle : LandingIntent
}

sealed interface LandingEffect : SideEffect {
    data object ShowVehicles : LandingEffect
    data object ShowAddVehicle : LandingEffect
}
