package com.mikonoma.drivinglog.vehicle.eventdetails

import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import org.fuusio.kide.presentation.SideEffect
import org.fuusio.kide.presentation.ViewIntent
import org.fuusio.kide.presentation.ViewState

/**
 * The screen re-observes [event] live from the repository, by id, so it reflects a later change (this one,
 * `add-event-pictures`) without the user having to navigate away and back. [event] carries everything the screen
 * shows, including its note (`VehicleEvent.note`, always null for an
 * [com.mikonoma.drivinglog.vehicle.domain.VehicleEvent.InitialOdometer]); [unit] formats its figure.
 *
 * [noteDraft] is non-null while the note editor (add-event-editing) is open, holding the text currently typed
 * there — the same shape `distance-logging`'s compose-time `LogEventState.noteDraft` already uses, seeded from the
 * event's current note (or empty) rather than from a pending, unsaved one.
 */
data class EventDetailsState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val event: VehicleEvent? = null,
    val unit: OdometerUnit = OdometerUnit.KILOMETERS,
    val noteDraft: String? = null,
) : ViewState

sealed interface EventDetailsIntent : ViewIntent {
    /** Opens the note editor, seeded with the event's current note (empty when it has none). */
    data object EditClicked : EventDetailsIntent
    data class NoteDraftEdited(val text: String) : EventDetailsIntent
    /** Back navigation from the editor: saves the typed text as the event's note (blank clears it). */
    data object NoteAttached : EventDetailsIntent
    /** "Discard": returns without changing the event's note. */
    data object NoteDiscarded : EventDetailsIntent
}

sealed interface EventDetailsEffect : SideEffect
