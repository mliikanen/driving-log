package com.mikonoma.drivinglog.landing

import androidx.compose.runtime.Composable
import com.mikonoma.drivinglog.di.AppGraph
import com.mikonoma.drivinglog.vehicle.AddVehicleNavKey
import com.mikonoma.drivinglog.vehicle.LogEventNavKey
import com.mikonoma.drivinglog.vehicle.VehicleListNavKey
import org.fuusio.kide.navigation.ScreenContext
import org.fuusio.kide.navigation.ScreenNavKey

/**
 * The Home screen, the start destination. It has no arguments and equals any other instance (Kide keeps a screen's processor in a store keyed by the key, and after a
 * rotation the back stack is restored into new key instances, see the note in `VehicleNavKeys.kt`).
 */
class LandingNavKey(private val graph: AppGraph) : ScreenNavKey<LandingProcessor> {
    override val serialKey: String = "landing"

    override fun equals(other: Any?): Boolean = other is LandingNavKey

    override fun hashCode(): Int = serialKey.hashCode()

    override fun toString(): String = "LandingNavKey"

    override fun createProcessor(): LandingProcessor = graph.landingProcessor

    override val screen: @Composable (ScreenContext<LandingProcessor>) -> Unit = { ctx ->
        LandingScreen(
            processor = ctx.processor,
            onShowVehicles = { ctx.navigateTo(VehicleListNavKey(graph)) },
            onShowAddVehicle = { ctx.navigateTo(AddVehicleNavKey(graph)) },
            onShowLogEvent = { ctx.navigateTo(LogEventNavKey(graph, "")) },
        )
    }
}
