package com.mikonoma.drivinglog.landing

import com.mikonoma.drivinglog.auth.AuthRepository
import com.mikonoma.drivinglog.auth.AuthState
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import dev.zacsweers.metro.Inject
import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor
import org.fuusio.kide.presentation.async
import org.fuusio.kide.presentation.reduce
import org.fuusio.kide.presentation.sideEffect

/** The Home screen: what its tiles are follows whether the user has any vehicle; nothing typed is kept, so nothing is saved across a rotation. */
@Inject
class LandingProcessor(repository: VehicleRepository, private val authRepository: AuthRepository) :
    PresentationProcessor<LandingIntent, LandingState, LandingEffect>(LandingState()) {

    init {
        observe("vehicles", repository.observeVehicles()) { vehicles ->
            reduce { copy(isLoading = false, hasVehicles = vehicles.isNotEmpty()) }
        }
        // `firebase-auth`'s "The signed-in account ... are reachable from the Home screen": Home is only ever
        // shown while SignedIn (App.kt's gate), so this is always a SignedIn value in practice.
        observe("auth", authRepository.observeAuthState()) { authState ->
            reduce { copy(accountEmail = (authState as? AuthState.SignedIn)?.email) }
        }
    }

    override suspend fun map(intent: LandingIntent): Action<LandingState, LandingEffect>? = when (intent) {
        LandingIntent.OpenVehicles -> sideEffect { LandingEffect.ShowVehicles }

        LandingIntent.AddVehicle -> sideEffect { LandingEffect.ShowAddVehicle }

        LandingIntent.OpenLogEvent -> if (state.hasVehicles) sideEffect { LandingEffect.ShowLogEvent } else null

        LandingIntent.ToggleAccountMenu -> reduce { copy(isAccountMenuOpen = !isAccountMenuOpen) }

        LandingIntent.DismissAccountMenu -> reduce { copy(isAccountMenuOpen = false) }

        LandingIntent.SignOut -> async("sign-out") {
            reduce { copy(isAccountMenuOpen = false) }
            authRepository.signOut()
        }
    }
}
