package com.mikonoma.drivinglog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mikonoma.drivinglog.di.AppGraph
import com.mikonoma.drivinglog.ui.theme.DrivingLogTheme
import com.mikonoma.drivinglog.vehicle.VehicleListNavKey
import com.mikonoma.drivinglog.vehicle.picture.sweepPictures
import com.mikonoma.drivinglog.vehicle.registerVehicleNavKeys
import org.fuusio.kide.navigation.AppNavigation
import org.fuusio.kide.navigation.rememberAppNavBackStack

@Composable
fun App(graph: AppGraph, modifier: Modifier = Modifier) {
    DrivingLogTheme {
        // Before the back stack, which may be restored from saved state and needs the registry.
        remember(graph) { registerVehicleNavKeys(graph) }
        // Once per start: files of pictures no vehicle uses (an interrupted save) are deleted. Failing to clean up is not a reason to stop.
        LaunchedEffect(graph) { runCatching { sweepPictures(graph.vehicleRepository, graph.vehiclePictureStore) } }
        val backStack = rememberAppNavBackStack(VehicleListNavKey(graph))
        androidx.compose.foundation.layout.Box(modifier) {
            AppNavigation(backStack)
        }
    }
}
