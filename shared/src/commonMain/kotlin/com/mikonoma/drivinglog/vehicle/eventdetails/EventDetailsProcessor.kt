package com.mikonoma.drivinglog.vehicle.eventdetails

import com.mikonoma.drivinglog.util.undoOnFailure
import com.mikonoma.drivinglog.vehicle.domain.PendingPicture
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.picture.EventPhotoDraftEditor
import com.mikonoma.drivinglog.vehicle.picture.ImageCodec
import com.mikonoma.drivinglog.vehicle.picture.PictureSize
import com.mikonoma.drivinglog.vehicle.picture.PictureStore
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.Named
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor
import org.fuusio.kide.presentation.async
import org.fuusio.kide.presentation.reduce

class EventDetailsProcessor @AssistedInject constructor(
    @Assisted private val vehicleId: String,
    @Assisted private val eventId: String,
    private val repository: VehicleRepository,
    @Named("event") private val eventPictures: PictureStore,
    private val codec: ImageCodec,
) : PresentationProcessor<EventDetailsIntent, EventDetailsState, EventDetailsEffect>(EventDetailsState()) {

    @AssistedFactory
    fun interface Factory {
        fun create(vehicleId: String, eventId: String): EventDetailsProcessor
    }

    private val photoEditor = EventPhotoDraftEditor(eventPictures, codec)

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
                val thumbnails = thumbnailsOf(event)
                reduce {
                    copy(isLoading = false, notFound = false, event = event, unit = vehicle.vehicle.odometerUnit, photoThumbnailUris = thumbnails)
                }
            }
        }
    }

    private suspend fun thumbnailsOf(event: VehicleEvent): List<Pair<String, String>> =
        event.photoIds.mapNotNull { id -> eventPictures.uri(id, PictureSize.SMALL)?.let { id to it } }

    override suspend fun map(intent: EventDetailsIntent): Action<EventDetailsState, EventDetailsEffect>? = when (intent) {
        EventDetailsIntent.EditClicked -> reduce {
            copy(edit = EventEditState(noteDraft = event?.note.orEmpty(), keptPhotos = photoThumbnailUris))
        }

        EventDetailsIntent.EditNoteOpened -> reduce { copy(edit = edit?.copy(noteEditorText = edit.noteDraft)) }

        is EventDetailsIntent.EditNoteTextEdited -> reduce { copy(edit = edit?.copy(noteEditorText = intent.text)) }

        EventDetailsIntent.EditNoteAttached -> reduce {
            copy(edit = edit?.let { it.copy(noteDraft = it.noteEditorText.orEmpty().trim(), noteEditorText = null) })
        }

        EventDetailsIntent.EditNoteDiscarded -> reduce { copy(edit = edit?.copy(noteEditorText = null)) }

        is EventDetailsIntent.EditPhotoPicked -> editPhotoStep { current ->
            if (current.isFull) current else current.copy(newPhotos = photoEditor.photoPicked(current.newPhotos, intent.result))
        }

        is EventDetailsIntent.EditNewPhotoRemoveRequested -> reduce {
            copy(edit = edit?.let { it.copy(newPhotos = photoEditor.removeRequested(it.newPhotos, intent.pendingId)) })
        }

        EventDetailsIntent.EditNewPhotoRemoveConfirmed -> editPhotoStep { it.copy(newPhotos = photoEditor.removeConfirmed(it.newPhotos)) }

        EventDetailsIntent.EditNewPhotoRemoveCancelled -> reduce {
            copy(edit = edit?.let { it.copy(newPhotos = photoEditor.removeCancelled(it.newPhotos)) })
        }

        is EventDetailsIntent.EditSavedPhotoRemoveRequested -> reduce { copy(edit = edit?.copy(savedPhotoRemovalPendingId = intent.photoId)) }

        EventDetailsIntent.EditSavedPhotoRemoveConfirmed -> reduce {
            copy(
                edit = edit?.let {
                    it.copy(keptPhotos = it.keptPhotos.filterNot { (id, _) -> id == it.savedPhotoRemovalPendingId }, savedPhotoRemovalPendingId = null)
                },
            )
        }

        EventDetailsIntent.EditSavedPhotoRemoveCancelled -> reduce { copy(edit = edit?.copy(savedPhotoRemovalPendingId = null)) }

        EventDetailsIntent.EditSaved -> editSaved()

        EventDetailsIntent.EditLeft -> editLeft()

        is EventDetailsIntent.PhotoClicked -> async("view-photo") {
            reduce { copy(viewingPhotoId = intent.photoId, viewingPhotoUri = null) }
            val uri = eventPictures.uri(intent.photoId, PictureSize.LARGE)
            reduce { copy(viewingPhotoUri = uri) }
        }

        EventDetailsIntent.PhotoViewerClosed -> reduce { copy(viewingPhotoId = null, viewingPhotoUri = null) }
    }

    /** Applies an edit-session photo change, then rebuilds the new photos' thumbnail URIs. */
    private fun editPhotoStep(change: suspend (EventEditState) -> EventEditState): Action<EventDetailsState, EventDetailsEffect>? {
        if (state.edit == null) return null
        return async("edit-photo") {
            val current = state.edit ?: return@async
            val next = change(current)
            val previews = photoEditor.previewUris(next.newPhotos)
            reduce { copy(edit = next.copy(newPhotoPreviewUris = previews)) }
        }
    }

    /** Commits the edit session's note and final photo set together: only the note if it changed, only the photos
     * that were actually added or removed this session — an unrelated field never gets touched. */
    private fun editSaved(): Action<EventDetailsState, EventDetailsEffect>? {
        val edit = state.edit ?: return null
        val originalEvent = state.event ?: return null
        return async("edit-save") {
            reduce { copy(edit = edit.copy(isSaving = true)) }
            undoOnFailure(
                undo = {
                    // Let the user try again; Kide logs the rethrown error.
                    reduce { copy(edit = edit.copy(isSaving = false)) }
                },
            ) {
                val note = edit.noteDraft.ifBlank { null }
                if (note != originalEvent.note) repository.updateEventNote(vehicleId, eventId, note)
                val keptIds = edit.keptPhotos.map { it.first }
                val removedIds = originalEvent.photoIds.filter { it !in keptIds }
                for (id in removedIds) repository.removeEventPhoto(vehicleId, eventId, id)
                for (pendingId in edit.newPhotos.pendingIds) repository.addEventPhoto(vehicleId, eventId, PendingPicture(pendingId))
            }
            // The live `observe("event", ...)` observer picks up the change too, but asynchronously (it needs its
            // own round of photo URI lookups): closing the screen only after re-reading the event and its photos
            // here too means the details screen never shows a stale photo list even for the one frame that would
            // otherwise pass before that observer catches up.
            val freshEvent = repository.observeEvent(vehicleId, eventId).first()
            val freshThumbnails = freshEvent?.let { thumbnailsOf(it) }.orEmpty()
            reduce { copy(edit = null, event = freshEvent ?: event, photoThumbnailUris = freshThumbnails) }
        }
    }

    /** Leaving without saving: newly picked photos' pending files are discarded; a locally removed saved photo was
     * never actually deleted, so the event is untouched. */
    private fun editLeft(): Action<EventDetailsState, EventDetailsEffect> {
        val edit = state.edit
        return async("edit-leave") {
            edit?.let { photoEditor.discardAll(it.newPhotos) }
            reduce { copy(edit = null) }
        }
    }
}
