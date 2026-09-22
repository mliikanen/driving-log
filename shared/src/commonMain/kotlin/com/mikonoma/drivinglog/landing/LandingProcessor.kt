package com.mikonoma.drivinglog.landing

import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import dev.zacsweers.metro.Inject
import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor
import org.fuusio.kide.presentation.sideEffect

/** The Home screen: what its tiles are follows whether the user has any vehicle; nothing typed is kept, so nothing is saved across a rotation. */
@Inject
class LandingProcessor(
    repository: VehicleRepository,
) : PresentationProcessor<LandingIntent, LandingState, LandingEffect>(LandingState()) {

    init {
        observe("vehicles", repository.observeVehicles()) { vehicles ->
            reduce { copy(isLoading = false, hasVehicles = vehicles.isNotEmpty()) }
        }
    }

    override suspend fun map(intent: LandingIntent): Action<LandingState, LandingEffect>? = when (intent) {
        LandingIntent.OpenVehicles -> sideEffect { LandingEffect.ShowVehicles }
        LandingIntent.AddVehicle -> sideEffect { LandingEffect.ShowAddVehicle }
        LandingIntent.OpenLogEvent -> if (state.hasVehicles) sideEffect { LandingEffect.ShowLogEvent } else null
    }
}
