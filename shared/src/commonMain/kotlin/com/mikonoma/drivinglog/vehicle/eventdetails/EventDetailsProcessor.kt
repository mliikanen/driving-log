package com.mikonoma.drivinglog.vehicle.eventdetails

import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.combine
import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor

class EventDetailsProcessor @AssistedInject constructor(
    @Assisted vehicleId: String,
    @Assisted eventId: String,
    repository: VehicleRepository,
) : PresentationProcessor<EventDetailsIntent, EventDetailsState, EventDetailsEffect>(EventDetailsState()) {

    @AssistedFactory
    fun interface Factory {
        fun create(vehicleId: String, eventId: String): EventDetailsProcessor
    }

    init {
        // Both the vehicle (for its unit) and the event (by id) are re-observed live; either disappearing is
        // "not found" — the event is the one this screen exists for, and its unit cannot be formatted without
        // the vehicle either.
        val details = combine(repository.observeVehicle(vehicleId), repository.observeEvent(vehicleId, eventId)) { vehicle, event ->
            vehicle to event
        }
        observe("event", details) { (vehicle, event) ->
            if (vehicle == null || event == null) {
                reduce { copy(isLoading = false, notFound = true) }
            } else {
                reduce { copy(isLoading = false, notFound = false, event = event, unit = vehicle.vehicle.odometerUnit) }
            }
        }
    }

    override suspend fun map(intent: EventDetailsIntent): Action<EventDetailsState, EventDetailsEffect>? = null
}
