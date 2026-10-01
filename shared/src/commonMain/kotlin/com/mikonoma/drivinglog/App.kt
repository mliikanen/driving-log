package com.mikonoma.drivinglog

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import com.mikonoma.drivinglog.auth.AuthRepository
import com.mikonoma.drivinglog.auth.AuthState
import com.mikonoma.drivinglog.auth.SignInProcessor
import com.mikonoma.drivinglog.auth.SignInScreen
import com.mikonoma.drivinglog.di.AppGraph
import com.mikonoma.drivinglog.ui.theme.DrivingLogTheme
import com.mikonoma.drivinglog.landing.LandingNavKey
import com.mikonoma.drivinglog.vehicle.ocr.sweepCaptures
import com.mikonoma.drivinglog.vehicle.picture.sweepPictures
import com.mikonoma.drivinglog.vehicle.registerVehicleNavKeys
import org.fuusio.kide.navigation.AppNavigation
import org.fuusio.kide.navigation.rememberAppNavBackStack

/**
 * Gated behind [AuthRepository] (design.md decision 3): nothing while `Loading`, the sign-in screen while
 * `SignedOut`, the app's own navigation — over the account-scoped [graphFor] result — while `SignedIn`. [graphFor]
 * is given the signed-in account's uid and returns the [AppGraph] for its own (per-account, design.md decision 5)
 * local database; the platform shell decides how that's resolved and cached.
 */
@Composable
fun App(authRepository: AuthRepository, graphFor: (uid: String) -> AppGraph, modifier: Modifier = Modifier) {
    // Pictures are small local files: a modest memory cache, no disk cache and no network module.
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.1).build() }
            .build()
    }
    val authState by authRepository.observeAuthState().collectAsState(initial = AuthState.Loading)
    DrivingLogTheme {
        // `modifier` carries `testTagsAsResourceId` (MainActivity) and must apply uniformly to whichever branch is
        // showing, not just AppNavigation's — otherwise the sign-in screen's own test tags are never exposed as
        // resource-ids, and Maestro can't find them (found via a real Maestro run, not anticipated in the plan).
        Box(modifier) {
            when (val state = authState) {
                AuthState.Loading -> Unit
                AuthState.SignedOut -> {
                    val processor = remember(authRepository) { SignInProcessor(authRepository) }
                    SignInScreen(processor)
                }
                is AuthState.SignedIn -> {
                    val graph = remember(state.uid) { graphFor(state.uid) }
                    // Before the back stack, which may be restored from saved state and needs the registry.
                    remember(graph) { registerVehicleNavKeys(graph) }
                    // Once per start: files of pictures no vehicle uses (an interrupted save) are deleted. Failing to clean up is not a reason to stop.
                    LaunchedEffect(graph) {
                        runCatching { sweepPictures(graph.vehicleRepository, graph.vehiclePictureStore) }
                        runCatching { sweepCaptures(graph.vehicleRepository, graph.captureStore) }
                    }
                    val backStack = rememberAppNavBackStack(LandingNavKey(graph))
                    AppNavigation(backStack)
                }
            }
        }
    }
}
