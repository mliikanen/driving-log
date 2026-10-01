package com.mikonoma.drivinglog.landing

import org.fuusio.kide.presentation.SideEffect
import org.fuusio.kide.presentation.ViewIntent
import org.fuusio.kide.presentation.ViewState

/** The four actions of the Home screen, in the order of the grid (row by row). */
enum class LandingTileId { VEHICLES, LOG_EVENT, TRIP, PLACEHOLDER }

/** Which icon a tile draws (the screen maps it to the vector). */
enum class LandingIcon { VEHICLES, LOG_EVENT, TRIP, PLACEHOLDER }

/**
 * One action of the grid. A tile is an icon and shows no text; [name] is what a screen reader reads (and what the UI flows find it by), empty only while the first tile
 * loads. A tile that is not [enabled] is drawn and exposed as disabled.
 */
data class LandingTile(val id: LandingTileId, val name: String, val icon: LandingIcon, val enabled: Boolean)

/**
 * The tiles of the Home screen for a user who has or has not any vehicle. The first tile is always the car: it opens the vehicles or, when there are none, the add
 * vehicle screen, and only its name says which; it is not enabled until the vehicles have loaded, so a tap cannot pick the wrong screen. The other three are placeholders
 * for the changes that build them and are not enabled yet.
 */
fun landingTiles(hasVehicles: Boolean, isLoading: Boolean): List<LandingTile> = listOf(
    when {
        isLoading -> LandingTile(LandingTileId.VEHICLES, "", LandingIcon.VEHICLES, enabled = false)
        hasVehicles -> LandingTile(LandingTileId.VEHICLES, "Vehicles", LandingIcon.VEHICLES, enabled = true)
        else -> LandingTile(LandingTileId.VEHICLES, "Add vehicle", LandingIcon.VEHICLES, enabled = true)
    },
    LandingTile(LandingTileId.LOG_EVENT, "Log event", LandingIcon.LOG_EVENT, enabled = hasVehicles),
    LandingTile(LandingTileId.TRIP, "Trip", LandingIcon.TRIP, enabled = false),
    LandingTile(LandingTileId.PLACEHOLDER, "Placeholder", LandingIcon.PLACEHOLDER, enabled = false),
)

data class LandingState(
    /** True until the first load. */
    val isLoading: Boolean = true,
    val hasVehicles: Boolean = false,
    /** Null only for the brief window before the first `AuthState` arrives — Home is never shown signed out. */
    val accountEmail: String? = null,
    val isAccountMenuOpen: Boolean = false,
) : ViewState {
    val tiles: List<LandingTile> get() = landingTiles(hasVehicles, isLoading)
}

sealed interface LandingIntent : ViewIntent {
    data object OpenVehicles : LandingIntent
    data object AddVehicle : LandingIntent
    data object OpenLogEvent : LandingIntent
    data object ToggleAccountMenu : LandingIntent
    data object DismissAccountMenu : LandingIntent
    data object SignOut : LandingIntent
}

sealed interface LandingEffect : SideEffect {
    data object ShowVehicles : LandingEffect
    data object ShowAddVehicle : LandingEffect
    data object ShowLogEvent : LandingEffect
}
