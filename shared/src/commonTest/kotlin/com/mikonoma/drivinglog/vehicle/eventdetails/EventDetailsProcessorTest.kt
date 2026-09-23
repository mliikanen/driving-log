package com.mikonoma.drivinglog.vehicle.eventdetails

import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import com.mikonoma.drivinglog.vehicle.UpdateNoteCall
import com.mikonoma.drivinglog.vehicle.distanceEvent
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import com.mikonoma.drivinglog.vehicle.initialEvent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailsProcessorTest {

    private val repository = FakeVehicleRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun processor(vehicleId: String = "v1", eventId: String) = EventDetailsProcessor(vehicleId, eventId, repository)

    @Test
    fun showsTheMatchingEventAndTheVehiclesUnit() {
        repository.seedVehicle("v1", "Family car", unit = OdometerUnit.KILOMETERS_TENTHS)
        repository.seedEvents("v1", listOf(distanceEvent("e2", 200, 30_000), initialEvent("e1", 100, 45_200_000)))

        val state = processor(eventId = "e2").state

        assertEquals(false, state.isLoading)
        assertEquals(false, state.notFound)
        assertEquals("e2", state.event?.id)
        assertEquals(OdometerUnit.KILOMETERS_TENTHS, state.unit)
    }

    @Test
    fun anUnknownEventIsReported() {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents("v1", listOf(initialEvent("e1", 100, 45_200_000)))

        val state = processor(eventId = "no-such-event").state

        assertEquals(false, state.isLoading)
        assertTrue(state.notFound)
        assertNull(state.event)
    }

    @Test
    fun aMissingVehicleIsReported() {
        val state = processor(vehicleId = "missing", eventId = "e1").state

        assertEquals(false, state.isLoading)
        assertTrue(state.notFound)
    }

    @Test
    fun theInitialOdometerEventHasNoNote() {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents("v1", listOf(initialEvent("e1", 100, 45_200_000)))

        val state = processor(eventId = "e1").state

        assertNull(state.event?.note)
    }

    @Test
    fun aDistanceEventCarriesItsNote() {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents(
            "v1",
            listOf(VehicleEvent.DistanceEntry("e2", ZonedMoment(Instant.fromEpochMilliseconds(200)), Distance(30_000), note = "borrowed to Sam")),
        )

        val state = processor(eventId = "e2").state

        assertEquals("borrowed to Sam", state.event?.note)
    }

    @Test
    fun anOdometerAnchorEventCarriesItsNote() {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents(
            "v1",
            listOf(VehicleEvent.OdometerAnchor("e2", ZonedMoment(Instant.fromEpochMilliseconds(200)), Distance(45_200_000), note = "note")),
        )

        val state = processor(eventId = "e2").state

        assertEquals("note", state.event?.note)
    }

    // ---- Editing the note (add-event-editing)

    private fun distanceEventProcessor(note: String? = null): EventDetailsProcessor {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents("v1", listOf(VehicleEvent.DistanceEntry("e2", ZonedMoment(Instant.fromEpochMilliseconds(200)), Distance(30_000), note = note)))
        return processor(eventId = "e2")
    }

    @Test
    fun editOpensTheEditorSeededWithTheCurrentNote() {
        val processor = distanceEventProcessor(note = "borrowed to Sam")

        processor.dispatch(EventDetailsIntent.EditClicked)

        assertEquals("borrowed to Sam", processor.state.noteDraft)
    }

    @Test
    fun editOpensTheEditorEmptyWhenThereIsNoNoteYet() {
        val processor = distanceEventProcessor(note = null)

        processor.dispatch(EventDetailsIntent.EditClicked)

        assertEquals("", processor.state.noteDraft)
    }

    @Test
    fun addingANoteToAnEventThatHadNone() {
        val processor = distanceEventProcessor(note = null)

        processor.dispatch(EventDetailsIntent.EditClicked)
        processor.dispatch(EventDetailsIntent.NoteDraftEdited("added later"))
        processor.dispatch(EventDetailsIntent.NoteAttached)

        assertEquals("added later", processor.state.event?.note)
        assertNull(processor.state.noteDraft)
        assertEquals(listOf(UpdateNoteCall("v1", "e2", "added later")), repository.updateNoteCalls)
    }

    @Test
    fun changingAnExistingNote() {
        val processor = distanceEventProcessor(note = "old note")

        processor.dispatch(EventDetailsIntent.EditClicked)
        processor.dispatch(EventDetailsIntent.NoteDraftEdited("new note"))
        processor.dispatch(EventDetailsIntent.NoteAttached)

        assertEquals("new note", processor.state.event?.note)
    }

    @Test
    fun clearingANoteByEditingItBlank() {
        val processor = distanceEventProcessor(note = "borrowed to Sam")

        processor.dispatch(EventDetailsIntent.EditClicked)
        processor.dispatch(EventDetailsIntent.NoteDraftEdited("   "))
        processor.dispatch(EventDetailsIntent.NoteAttached)

        assertNull(processor.state.event?.note)
        assertEquals(listOf(UpdateNoteCall("v1", "e2", null)), repository.updateNoteCalls)
    }

    @Test
    fun discardDropsTheEdit() {
        val processor = distanceEventProcessor(note = "borrowed to Sam")

        processor.dispatch(EventDetailsIntent.EditClicked)
        processor.dispatch(EventDetailsIntent.NoteDraftEdited("changed but discarded"))
        processor.dispatch(EventDetailsIntent.NoteDiscarded)

        assertEquals("borrowed to Sam", processor.state.event?.note)
        assertNull(processor.state.noteDraft)
        assertEquals(emptyList(), repository.updateNoteCalls)
    }
}
