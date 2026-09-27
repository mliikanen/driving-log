package com.mikonoma.drivinglog.vehicle.details

import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.picture.PictureSize
import com.mikonoma.drivinglog.vehicle.picture.PictureStore
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.combine
import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor
import org.fuusio.kide.presentation.sideEffect

class VehicleDetailsProcessor @AssistedInject constructor(
    @Assisted private val vehicleId: String,
    repository: VehicleRepository,
    private val pictures: PictureStore,
) : PresentationProcessor<VehicleDetailsIntent, VehicleDetailsState, VehicleDetailsEffect>(VehicleDetailsState()) {

    @AssistedFactory
    fun interface Factory {
        fun create(vehicleId: String): VehicleDetailsProcessor
    }

    init {
        val details = combine(
            repository.observeVehicle(vehicleId),
            repository.observeRecentEvents(vehicleId, RECENT_EVENT_LIMIT),
        ) { details, events -> details to events }

        observe("details", details) { (details, events) ->
            if (details == null) {
                reduce { copy(isLoading = false, notFound = true) }
            } else {
                val pictureUri = details.vehicle.pictureId?.let { pictures.uri(it, PictureSize.LARGE) }
                reduce {
                    copy(
                        isLoading = false,
                        notFound = false,
                        name = details.vehicle.name,
                        licensePlate = details.vehicle.licensePlate,
                        pictureUri = pictureUri,
                        type = details.vehicle.type,
                        color = details.vehicle.color,
                        unit = details.vehicle.odometerUnit,
                        currentOdometer = details.currentOdometer,
                        recentEvents = events,
                    )
                }
            }
        }
    }

    override suspend fun map(intent: VehicleDetailsIntent): Action<VehicleDetailsState, VehicleDetailsEffect>? =
        when (intent) {
            VehicleDetailsIntent.EditClicked -> sideEffect { VehicleDetailsEffect.ShowEdit(vehicleId) }
            VehicleDetailsIntent.ViewLogClicked -> sideEffect { VehicleDetailsEffect.ShowLog(vehicleId) }
            VehicleDetailsIntent.LogEventClicked -> sideEffect { VehicleDetailsEffect.ShowLogEvent(vehicleId) }
            is VehicleDetailsIntent.EventClicked -> {
                val eventId = intent.eventId
                sideEffect { VehicleDetailsEffect.ShowEventDetails(vehicleId, eventId) }
            }
        }

    companion object {
        const val RECENT_EVENT_LIMIT = 5
    }
}
