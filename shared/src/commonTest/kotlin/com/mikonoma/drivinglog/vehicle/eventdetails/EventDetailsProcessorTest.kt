package com.mikonoma.drivinglog.vehicle.eventdetails

import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import com.mikonoma.drivinglog.vehicle.UpdateNoteCall
import com.mikonoma.drivinglog.vehicle.distanceEvent
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import com.mikonoma.drivinglog.vehicle.initialEvent
import com.mikonoma.drivinglog.vehicle.picture.FakeImageCodec
import com.mikonoma.drivinglog.vehicle.picture.FakePictureStore
import com.mikonoma.drivinglog.vehicle.picture.PhotoResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailsProcessorTest {

    private val repository = FakeVehicleRepository()
    private val eventPictures = FakePictureStore()
    private val codec = FakeImageCodec()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun processor(vehicleId: String = "v1", eventId: String) = EventDetailsProcessor(vehicleId, eventId, repository, eventPictures, codec)

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

    // ---- Editing the note and photos (add-event-editing, extended by add-event-pictures)

    private fun distanceEventProcessor(note: String? = null, photoIds: List<String> = emptyList()): EventDetailsProcessor {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents(
            "v1",
            listOf(VehicleEvent.DistanceEntry("e2", ZonedMoment(Instant.fromEpochMilliseconds(200)), Distance(30_000), note = note, photoIds = photoIds)),
        )
        for (id in photoIds) eventPictures.pictures[id] = FakePictureStore.Versions(FakePictureStore.image(1), FakePictureStore.image(2))
        return processor(eventId = "e2")
    }

    @Test
    fun editClickedOpensTheEditScreenSeededWithTheCurrentNote() {
        val processor = distanceEventProcessor(note = "borrowed to Sam")

        processor.dispatch(EventDetailsIntent.EditClicked)

        assertEquals("borrowed to Sam", processor.state.edit?.noteDraft)
    }

    @Test
    fun editClickedOpensTheEditScreenEmptyWhenThereIsNoNoteYet() {
        val processor = distanceEventProcessor(note = null)

        processor.dispatch(EventDetailsIntent.EditClicked)

        assertEquals("", processor.state.edit?.noteDraft)
    }

    @Test
    fun editClickedSeedsTheEditScreenWithTheEventsPhotos() {
        val processor = distanceEventProcessor(photoIds = listOf("p1", "p2"))

        processor.dispatch(EventDetailsIntent.EditClicked)

        assertEquals(listOf("p1", "p2"), processor.state.edit?.keptPhotos?.map { it.first })
    }

    @Test
    fun nothingIsSavedUntilTheEditScreensSaveIsTapped() {
        val processor = distanceEventProcessor(note = null)
        processor.dispatch(EventDetailsIntent.EditClicked)

        processor.dispatch(EventDetailsIntent.EditNoteOpened)
        processor.dispatch(EventDetailsIntent.EditNoteTextEdited("added later"))
        processor.dispatch(EventDetailsIntent.EditNoteAttached)

        assertNull(processor.state.event?.note)
        assertEquals(emptyList(), repository.updateNoteCalls)
        assertEquals("added later", processor.state.edit?.noteDraft)
    }

    @Test
    fun savingAddsANoteToAnEventThatHadNone() {
        val processor = distanceEventProcessor(note = null)
        processor.dispatch(EventDetailsIntent.EditClicked)
        processor.dispatch(EventDetailsIntent.EditNoteOpened)
        processor.dispatch(EventDetailsIntent.EditNoteTextEdited("added later"))
        processor.dispatch(EventDetailsIntent.EditNoteAttached)

        processor.dispatch(EventDetailsIntent.EditSaved)

        assertEquals("added later", processor.state.event?.note)
        assertNull(processor.state.edit)
        assertEquals(listOf(UpdateNoteCall("v1", "e2", "added later")), repository.updateNoteCalls)
    }

    @Test
    fun savingChangesAnExistingNote() {
        val processor = distanceEventProcessor(note = "old note")
        processor.dispatch(EventDetailsIntent.EditClicked)
        processor.dispatch(EventDetailsIntent.EditNoteOpened)
        processor.dispatch(EventDetailsIntent.EditNoteTextEdited("new note"))
        processor.dispatch(EventDetailsIntent.EditNoteAttached)

        processor.dispatch(EventDetailsIntent.EditSaved)

        assertEquals("new note", processor.state.event?.note)
    }

    @Test
    fun savingClearsANoteByEditingItBlank() {
        val processor = distanceEventProcessor(note = "borrowed to Sam")
        processor.dispatch(EventDetailsIntent.EditClicked)
        processor.dispatch(EventDetailsIntent.EditNoteOpened)
        processor.dispatch(EventDetailsIntent.EditNoteTextEdited("   "))
        processor.dispatch(EventDetailsIntent.EditNoteAttached)

        processor.dispatch(EventDetailsIntent.EditSaved)

        assertNull(processor.state.event?.note)
        assertEquals(listOf(UpdateNoteCall("v1", "e2", null)), repository.updateNoteCalls)
    }

    @Test
    fun discardingTheNoteEditorKeepsTheEditSessionsPreviousDraft() {
        val processor = distanceEventProcessor(note = "borrowed to Sam")
        processor.dispatch(EventDetailsIntent.EditClicked)

        processor.dispatch(EventDetailsIntent.EditNoteOpened)
        processor.dispatch(EventDetailsIntent.EditNoteTextEdited("changed but discarded"))
        processor.dispatch(EventDetailsIntent.EditNoteDiscarded)

        assertEquals("borrowed to Sam", processor.state.edit?.noteDraft)
        assertNull(processor.state.edit?.noteEditorText)
    }

    @Test
    fun leavingTheEditScreenWithoutSavingLeavesTheNoteAndPhotosExactlyAsTheyWere() {
        val processor = distanceEventProcessor(note = "borrowed to Sam", photoIds = listOf("p1"))
        processor.dispatch(EventDetailsIntent.EditClicked)
        processor.dispatch(EventDetailsIntent.EditNoteOpened)
        processor.dispatch(EventDetailsIntent.EditNoteTextEdited("changed but not saved"))
        processor.dispatch(EventDetailsIntent.EditNoteAttached)
        processor.dispatch(EventDetailsIntent.EditSavedPhotoRemoveRequested("p1"))
        processor.dispatch(EventDetailsIntent.EditSavedPhotoRemoveConfirmed)

        processor.dispatch(EventDetailsIntent.EditLeft)

        assertNull(processor.state.edit)
        assertEquals("borrowed to Sam", processor.state.event?.note)
        assertEquals(listOf("p1"), processor.state.event?.photoIds)
        assertEquals(emptyList(), repository.updateNoteCalls)
        assertEquals(emptyList(), repository.removeEventPhotoCalls)
    }

    @Test
    fun addingAPhotoInTheEditScreenIsNotSavedUntilSave() {
        val processor = distanceEventProcessor()
        processor.dispatch(EventDetailsIntent.EditClicked)

        processor.dispatch(EventDetailsIntent.EditPhotoPicked(PhotoResult.Chosen(byteArrayOf(1))))

        assertEquals(1, processor.state.edit?.newPhotos?.pendingIds?.size)
        assertEquals(emptyList(), repository.addEventPhotoCalls)
    }

    @Test
    fun savingAddsANewlyPickedPhotoToTheEvent() {
        val processor = distanceEventProcessor()
        processor.dispatch(EventDetailsIntent.EditClicked)
        processor.dispatch(EventDetailsIntent.EditPhotoPicked(PhotoResult.Chosen(byteArrayOf(1))))

        processor.dispatch(EventDetailsIntent.EditSaved)

        assertEquals(1, processor.state.event?.photoIds?.size)
        assertEquals(1, repository.addEventPhotoCalls.size)
    }

    @Test
    fun savingKeepsTheExistingPhotoAndAddsTheNewOneToTheEventImmediately() {
        val processor = distanceEventProcessor(photoIds = listOf("p1"))
        processor.dispatch(EventDetailsIntent.EditClicked)
        processor.dispatch(EventDetailsIntent.EditPhotoPicked(PhotoResult.Chosen(byteArrayOf(1))))
        assertEquals(1, processor.state.edit?.newPhotos?.pendingIds?.size, "pending photo not picked up")

        processor.dispatch(EventDetailsIntent.EditSaved)

        // The very first state after Save already reflects both photos on the event itself — not just the kept
        // one, waiting on the separate live observer to catch up a frame later.
        assertEquals(2, processor.state.event?.photoIds?.size)
        assertEquals(1, repository.addEventPhotoCalls.size)
    }

    @Test
    fun savingAfterRemovingAPhotoImmediatelyUpdatesTheThumbnailsNoStaleFrame() {
        val processor = distanceEventProcessor(photoIds = listOf("p1", "p2"))
        processor.dispatch(EventDetailsIntent.EditClicked)
        processor.dispatch(EventDetailsIntent.EditSavedPhotoRemoveRequested("p1"))
        processor.dispatch(EventDetailsIntent.EditSavedPhotoRemoveConfirmed)

        processor.dispatch(EventDetailsIntent.EditSaved)

        // The very first state after Save already shows only the kept photo's thumbnail — not both, waiting on the
        // separate live observer to catch up a frame later.
        assertEquals(listOf("p2"), processor.state.photoThumbnailUris.map { it.first })
    }

    @Test
    fun removingASavedPhotoAndSavingDetachesIt() {
        val processor = distanceEventProcessor(photoIds = listOf("p1", "p2"))
        processor.dispatch(EventDetailsIntent.EditClicked)
        processor.dispatch(EventDetailsIntent.EditSavedPhotoRemoveRequested("p1"))
        assertEquals("p1", processor.state.edit?.savedPhotoRemovalPendingId)

        processor.dispatch(EventDetailsIntent.EditSavedPhotoRemoveConfirmed)
        processor.dispatch(EventDetailsIntent.EditSaved)

        assertEquals(listOf("p2"), processor.state.event?.photoIds)
        assertEquals(listOf("v1" to "p1"), repository.removeEventPhotoCalls.map { it.vehicleId to it.pictureId })
    }

    @Test
    fun cancellingASavedPhotoRemovalKeepsIt() {
        val processor = distanceEventProcessor(photoIds = listOf("p1"))
        processor.dispatch(EventDetailsIntent.EditClicked)
        processor.dispatch(EventDetailsIntent.EditSavedPhotoRemoveRequested("p1"))

        processor.dispatch(EventDetailsIntent.EditSavedPhotoRemoveCancelled)

        assertEquals(listOf("p1"), processor.state.edit?.keptPhotos?.map { it.first })
        assertNull(processor.state.edit?.savedPhotoRemovalPendingId)
    }

    @Test
    fun removingANewlyPickedPhotoDiscardsItImmediately() {
        val processor = distanceEventProcessor()
        processor.dispatch(EventDetailsIntent.EditClicked)
        processor.dispatch(EventDetailsIntent.EditPhotoPicked(PhotoResult.Chosen(byteArrayOf(1))))
        val pendingId = processor.state.edit?.newPhotos?.pendingIds?.single()!!

        processor.dispatch(EventDetailsIntent.EditNewPhotoRemoveRequested(pendingId))
        processor.dispatch(EventDetailsIntent.EditNewPhotoRemoveConfirmed)

        assertEquals(emptyList(), processor.state.edit?.newPhotos?.pendingIds)
        assertFalse(pendingId in eventPictures.pending)
    }

    @Test
    fun leavingTheEditScreenDiscardsNewlyPickedPendingPhotoFiles() {
        val processor = distanceEventProcessor()
        processor.dispatch(EventDetailsIntent.EditClicked)
        processor.dispatch(EventDetailsIntent.EditPhotoPicked(PhotoResult.Chosen(byteArrayOf(1))))
        val pendingId = processor.state.edit?.newPhotos?.pendingIds?.single()!!

        processor.dispatch(EventDetailsIntent.EditLeft)

        assertFalse(pendingId in eventPictures.pending)
        assertEquals(emptyList(), repository.addEventPhotoCalls)
    }

    // ---- Refueling (add-refueling-logging)

    @Test
    fun aRefuelingEventCarriesItsNote() {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents(
            "v1",
            listOf(
                com.mikonoma.drivinglog.vehicle.domain.VehicleEvent.Refueling(
                    "e2",
                    ZonedMoment(Instant.fromEpochMilliseconds(200)),
                    com.mikonoma.drivinglog.vehicle.domain.Volume(42_300),
                    com.mikonoma.drivinglog.vehicle.domain.FuelUnit.LITERS,
                    com.mikonoma.drivinglog.vehicle.domain.FuelType.DIESEL,
                    filledUp = true,
                    note = "cheap gas today",
                ),
            ),
        )

        val state = processor(eventId = "e2").state

        assertEquals("cheap gas today", state.event?.note)
    }

    private fun refuelingEventProcessor(note: String? = null, photoIds: List<String> = emptyList()): EventDetailsProcessor {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents(
            "v1",
            listOf(
                com.mikonoma.drivinglog.vehicle.domain.VehicleEvent.Refueling(
                    "e2",
                    ZonedMoment(Instant.fromEpochMilliseconds(200)),
                    com.mikonoma.drivinglog.vehicle.domain.Volume(42_300),
                    com.mikonoma.drivinglog.vehicle.domain.FuelUnit.LITERS,
                    com.mikonoma.drivinglog.vehicle.domain.FuelType.DIESEL,
                    filledUp = true,
                    note = note,
                    photoIds = photoIds,
                ),
            ),
        )
        for (id in photoIds) eventPictures.pictures[id] = FakePictureStore.Versions(FakePictureStore.image(1), FakePictureStore.image(2))
        return processor(eventId = "e2")
    }

    @Test
    fun editClickedOnARefuelingSeedsTheEditScreenWithItsNoteAndPhotos() {
        val processor = refuelingEventProcessor(note = "cheap gas today", photoIds = listOf("p1"))

        processor.dispatch(EventDetailsIntent.EditClicked)

        assertEquals("cheap gas today", processor.state.edit?.noteDraft)
        assertEquals(listOf("p1"), processor.state.edit?.keptPhotos?.map { it.first })
    }

    @Test
    fun savingARefuelingsNoteNeverChangesItsFuelFieldsOrMileage() {
        val processor = refuelingEventProcessor(note = "old note")
        processor.dispatch(EventDetailsIntent.EditClicked)
        processor.dispatch(EventDetailsIntent.EditNoteOpened)
        processor.dispatch(EventDetailsIntent.EditNoteTextEdited("new note"))
        processor.dispatch(EventDetailsIntent.EditNoteAttached)

        processor.dispatch(EventDetailsIntent.EditSaved)

        val refueling = processor.state.event as com.mikonoma.drivinglog.vehicle.domain.VehicleEvent.Refueling
        assertEquals("new note", refueling.note)
        assertEquals(com.mikonoma.drivinglog.vehicle.domain.Volume(42_300), refueling.amount)
        assertEquals(com.mikonoma.drivinglog.vehicle.domain.FuelType.DIESEL, refueling.fuelType)
        assertTrue(refueling.filledUp)
    }

    private fun refuelingWithMileage(mileage: com.mikonoma.drivinglog.vehicle.domain.RefuelingMileage?): EventDetailsProcessor {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents(
            "v1",
            listOf(
                com.mikonoma.drivinglog.vehicle.domain.VehicleEvent.Refueling(
                    "e2",
                    ZonedMoment(Instant.fromEpochMilliseconds(200)),
                    com.mikonoma.drivinglog.vehicle.domain.Volume(42_300),
                    com.mikonoma.drivinglog.vehicle.domain.FuelUnit.LITERS,
                    com.mikonoma.drivinglog.vehicle.domain.FuelType.DIESEL,
                    filledUp = true,
                    mileage = mileage,
                ),
            ),
        )
        return processor(eventId = "e2")
    }

    @Test
    fun theDetailsScreenShowsARefuelingsTripDistanceMileage() {
        val mileage = com.mikonoma.drivinglog.vehicle.domain.RefuelingMileage.Added(Distance(30_000))
        val state = refuelingWithMileage(mileage).state

        assertEquals(mileage, (state.event as com.mikonoma.drivinglog.vehicle.domain.VehicleEvent.Refueling).mileage)
    }

    @Test
    fun theDetailsScreenShowsARefuelingsNewOdometerMileage() {
        val mileage = com.mikonoma.drivinglog.vehicle.domain.RefuelingMileage.Anchor(Distance(50_000_000))
        val state = refuelingWithMileage(mileage).state

        assertEquals(mileage, (state.event as com.mikonoma.drivinglog.vehicle.domain.VehicleEvent.Refueling).mileage)
    }

    @Test
    fun theDetailsScreenShowsNoMileageWhenTheRefuelingHasNone() {
        val state = refuelingWithMileage(null).state

        assertNull((state.event as com.mikonoma.drivinglog.vehicle.domain.VehicleEvent.Refueling).mileage)
    }

    @Test
    fun addingAPhotoToARefuelingThroughEditWorksTheSameAsForADistanceEntry() {
        val processor = refuelingEventProcessor()
        processor.dispatch(EventDetailsIntent.EditClicked)
        processor.dispatch(EventDetailsIntent.EditPhotoPicked(PhotoResult.Chosen(byteArrayOf(1))))

        processor.dispatch(EventDetailsIntent.EditSaved)

        val refueling = processor.state.event as com.mikonoma.drivinglog.vehicle.domain.VehicleEvent.Refueling
        assertEquals(1, refueling.photoIds.size)
    }
}
