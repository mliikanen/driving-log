package com.mikonoma.drivinglog.vehicle.eventdetails

import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import org.fuusio.kide.presentation.SideEffect
import org.fuusio.kide.presentation.ViewIntent
import org.fuusio.kide.presentation.ViewState

/**
 * Read-only (add-event-details-view): the screen re-observes [event] live from the repository, by id, so it
 * reflects a later change (`add-event-editing`, `add-event-pictures`) without the user having to navigate away
 * and back. [event] carries everything the screen shows, including its note (`VehicleEvent.note`, always null for
 * an [com.mikonoma.drivinglog.vehicle.domain.VehicleEvent.InitialOdometer]); [unit] formats its figure.
 */
data class EventDetailsState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val event: VehicleEvent? = null,
    val unit: OdometerUnit = OdometerUnit.KILOMETERS,
) : ViewState

/** Nothing is editable yet; `add-event-editing` adds the "Edit" action as its own, separately reviewed change. */
sealed interface EventDetailsIntent : ViewIntent

sealed interface EventDetailsEffect : SideEffect
