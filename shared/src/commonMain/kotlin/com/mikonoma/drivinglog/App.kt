package com.mikonoma.drivinglog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mikonoma.drivinglog.di.AppGraph
import com.mikonoma.drivinglog.ui.theme.DrivingLogTheme
import com.mikonoma.drivinglog.vehicle.VehicleListNavKey
import com.mikonoma.drivinglog.vehicle.registerVehicleNavKeys
import org.fuusio.kide.navigation.AppNavigation
import org.fuusio.kide.navigation.rememberAppNavBackStack

@Composable
fun App(graph: AppGraph, modifier: Modifier = Modifier) {
    DrivingLogTheme {
        // Before the back stack, which may be restored from saved state and needs the registry.
        remember(graph) { registerVehicleNavKeys(graph) }
        val backStack = rememberAppNavBackStack(VehicleListNavKey(graph))
        androidx.compose.foundation.layout.Box(modifier) {
            AppNavigation(backStack)
        }
    }
}
