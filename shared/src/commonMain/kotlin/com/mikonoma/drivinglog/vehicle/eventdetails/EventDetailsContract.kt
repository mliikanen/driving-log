package com.mikonoma.drivinglog.vehicle.eventdetails

import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.picture.EventPhotoDraft
import com.mikonoma.drivinglog.vehicle.picture.PhotoResult
import org.fuusio.kide.presentation.SideEffect
import org.fuusio.kide.presentation.ViewIntent
import org.fuusio.kide.presentation.ViewState

/**
 * The screen re-observes [event] live from the repository, by id, so it reflects a later change without the user
 * having to navigate away and back. [event] carries everything the screen shows, including its note
 * (`VehicleEvent.note`, always null for an [com.mikonoma.drivinglog.vehicle.domain.VehicleEvent.InitialOdometer])
 * and its photos (`VehicleEvent.photoIds`, `add-event-pictures`); [unit] formats its figure.
 *
 * [edit] is non-null while the "Edit" screen (`add-event-editing`, extended by `add-event-pictures`) is open, and
 * [viewingPhotoId] while a thumbnail's full-size viewer is open.
 */
data class EventDetailsState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val event: VehicleEvent? = null,
    val unit: OdometerUnit = OdometerUnit.KILOMETERS,
    /** [event]'s photos' thumbnail URIs (the small version), paired with each photo's id, in attach order. */
    val photoThumbnailUris: List<Pair<String, String>> = emptyList(),
    val edit: EventEditState? = null,
    /** The id of the photo open in the full-size viewer (`add-event-pictures`), or null when none is. */
    val viewingPhotoId: String? = null,
    /** The full-size viewer's URI for [viewingPhotoId], loaded once it opens; null while it loads or none is open. */
    val viewingPhotoUri: String? = null,
) : ViewState

/**
 * The "Edit" screen's own state (`add-event-editing`, extended by `add-event-pictures`): the note element (tap it to
 * open the same full-screen note editor the log event form uses) and the photo strip (the same element, up to 5
 * total). Nothing here takes effect on the saved event until [EventDetailsIntent.EditSaved] — leaving without saving
 * (back navigation) discards it, keeping the event exactly as it was.
 */
data class EventEditState(
    /** The note as currently committed within this edit session: seeded from the event's own note (or "") when the
     * screen opens, and changed only when the note editor closes via back navigation (attaching [noteEditorText]). */
    val noteDraft: String = "",
    /** Non-null while the full-screen note editor (reusing `NoteEditorContent`) is open on top of this screen,
     * holding the text currently typed there — the same [noteDraft]/[noteEditorText] split
     * `distance-logging`'s compose-time `LogEventState.pendingNote`/`noteDraft` already uses, nested one level in. */
    val noteEditorText: String? = null,
    /** The event's own photos (id to thumbnail URI, seeded from [EventDetailsState.photoThumbnailUris]) still kept
     * in this session: starts as all of them; removing one drops it here immediately, but its file is not deleted
     * until [EventDetailsIntent.EditSaved] — leaving without saving keeps it. */
    val keptPhotos: List<Pair<String, String>> = emptyList(),
    /** Newly picked photos this session, not yet part of the event: the same shape the log event form uses. */
    val newPhotos: EventPhotoDraft = EventPhotoDraft(),
    /** [newPhotos]' thumbnail URIs, paired with each photo's pending id: rebuilt from the store, not persisted. */
    val newPhotoPreviewUris: List<Pair<String, String>> = emptyList(),
    /** The already-saved photo id whose "Remove this photo?" confirmation is showing, or null. A [newPhotos] photo's
     * own removal confirmation is tracked by [EventPhotoDraft.removalPendingId] instead. */
    val savedPhotoRemovalPendingId: String? = null,
    val isSaving: Boolean = false,
) {
    /** The strip shows [keptPhotos] then [EventPhotoDraft.pendingIds]; capped at 5 photos total, from either source. */
    val isFull: Boolean get() = keptPhotos.size + newPhotos.pendingIds.size >= 5
}

sealed interface EventDetailsIntent : ViewIntent {
    /** Opens the "Edit" screen, seeded with the event's current note and photos. */
    data object EditClicked : EventDetailsIntent

    /** Opens the note element's full-screen editor, seeded from [EventEditState.noteDraft]. */
    data object EditNoteOpened : EventDetailsIntent
    data class EditNoteTextEdited(val text: String) : EventDetailsIntent

    /** Back navigation from the note editor: keeps the typed text as the edit session's draft (not yet saved). */
    data object EditNoteAttached : EventDetailsIntent

    /** The note editor's "Discard": returns to the edit screen with the draft as it was before the editor opened. */
    data object EditNoteDiscarded : EventDetailsIntent

    /** The system chooser gave back a photo, or nothing, for the edit screen's photo strip (`add-event-pictures`). */
    data class EditPhotoPicked(val result: PhotoResult) : EventDetailsIntent

    /** A newly picked (not yet saved) photo's remove action: asks for confirmation. */
    data class EditNewPhotoRemoveRequested(val pendingId: String) : EventDetailsIntent
    data object EditNewPhotoRemoveConfirmed : EventDetailsIntent
    data object EditNewPhotoRemoveCancelled : EventDetailsIntent

    /** An already-saved photo's remove action: asks for confirmation (the file is kept until Save). */
    data class EditSavedPhotoRemoveRequested(val photoId: String) : EventDetailsIntent
    data object EditSavedPhotoRemoveConfirmed : EventDetailsIntent
    data object EditSavedPhotoRemoveCancelled : EventDetailsIntent

    /** The edit screen's explicit "Save" action: commits the note text and the final photo set together. */
    data object EditSaved : EventDetailsIntent

    /** Leaving the edit screen without saving: the event's note and photos are left exactly as they were. */
    data object EditLeft : EventDetailsIntent

    /** A thumbnail was tapped: opens the full-size viewer for that photo. */
    data class PhotoClicked(val photoId: String) : EventDetailsIntent

    /** The full-size viewer was dismissed. */
    data object PhotoViewerClosed : EventDetailsIntent
}

sealed interface EventDetailsEffect : SideEffect
