package com.mikonoma.drivinglog.vehicle.domain

import com.mikonoma.drivinglog.vehicle.anchorEvent
import com.mikonoma.drivinglog.vehicle.distanceEvent
import com.mikonoma.drivinglog.vehicle.initialEvent
import com.mikonoma.drivinglog.vehicle.refuelingEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** `vehicle-log`, "Events at the same time are ordered by entry, then by kind" (atomic-log-events). */
class LogOrderTest {

    @Test
    fun eventsAreOrderedByInstantFirst() {
        val added = listOf(distanceEvent("late", 3_000, 10_000), distanceEvent("early", 1_000, 10_000))
        assertEquals(listOf("early", "late"), added.inLogOrder().map { it.id })
    }

    @Test
    fun entriesAtTheSameInstantKeepTheOrderTheyWereAddedInWhateverTheirKind() {
        // A refueling with no mileage, then a distance entry, both at 12:00: the distance entry's entry was added last.
        val added = listOf(refuelingEvent("r", 1_000, 42_300), distanceEvent("d", 1_000, 30_000))
        assertEquals(listOf("r", "d"), added.inLogOrder().map { it.id })
    }

    @Test
    fun theEventsOfOneEntryFollowTheirKindWhateverOrderTheyWereAddedIn() {
        // A refueling added before its own mileage event still comes after it: odometer reading, distance, refueling.
        val withDistance = listOf(refuelingEvent("r", 1_000, 42_300), distanceEvent("r-mileage", 1_000, 30_000, entryId = "r"))
        assertEquals(listOf("r-mileage", "r"), withDistance.inLogOrder().map { it.id })
        val withReading = listOf(refuelingEvent("r", 1_000, 42_300), anchorEvent("r-mileage", 1_000, 45_300_000, entryId = "r"))
        assertEquals(listOf("r-mileage", "r"), withReading.inLogOrder().map { it.id })
    }

    @Test
    fun anEntrysPlaceIsWhereItsFirstEventWasAdded() {
        val added = listOf(
            refuelingEvent("r", 1_000, 42_300),
            distanceEvent("d", 1_000, 10_000),
            distanceEvent("r-mileage", 1_000, 30_000, entryId = "r"),
        )
        assertEquals(listOf("r-mileage", "r", "d"), added.inLogOrder().map { it.id })
    }

    @Test
    fun theKindRanksAreTheSpecifiedOrder() {
        assertEquals(
            listOf(VehicleEvent.Kind.ODOMETER_READING, VehicleEvent.Kind.DISTANCE, VehicleEvent.Kind.REFUELING),
            VehicleEvent.Kind.entries.sortedBy { it.rank },
        )
    }

    @Test
    fun anEntryShowsItsRefuelingAndItsMileage() {
        val refueling = refuelingEvent("r", 1_000, 42_300)
        val mileage = distanceEvent("r-mileage", 1_000, 30_000, entryId = "r")
        val entry = LogEntry("r", EntrySource.LOG_FORM, listOf(mileage, refueling))

        assertEquals(refueling, entry.primary)
        assertEquals(mileage, entry.mileage)
        assertEquals(true, entry.isEditable)
    }

    @Test
    fun anEntryOfOneEventShowsThatEventAndNoMileage() {
        val distance = distanceEvent("d", 1_000, 30_000)
        val entry = LogEntry("d", EntrySource.LOG_FORM, listOf(distance))

        assertEquals(distance, entry.primary)
        assertNull(entry.mileage)
    }

    @Test
    fun theInitialOdometersEntryCannotBeEdited() {
        val entry = LogEntry("i", EntrySource.VEHICLE_ADDED, listOf(initialEvent("i", 1_000, 45_200_000)))
        assertEquals(false, entry.isEditable)
    }

    @Test
    fun anEntryHoldsAtLeastOneEvent() {
        assertFailsWith<IllegalArgumentException> { LogEntry("x", EntrySource.LOG_FORM, emptyList()) }
    }

    @Test
    fun anUnknownSourceCodeReadsAsTheLogEventForm() {
        assertEquals(EntrySource.VEHICLE_ADDED, EntrySource.fromCode("VEHICLE_ADDED"))
        assertEquals(EntrySource.LOG_FORM, EntrySource.fromCode("TRIP_START"))
    }
}
