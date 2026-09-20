package com.mikonoma.drivinglog.vehicle.domain

import com.mikonoma.drivinglog.vehicle.anchorEvent
import com.mikonoma.drivinglog.vehicle.distanceEvent
import com.mikonoma.drivinglog.vehicle.initialEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class KnownOdometerTest {

    private fun at(millis: Long) = Instant.fromEpochMilliseconds(millis)

    private val initial = initialEvent("i", 1_000, 45_200_000)

    @Test
    fun anEmptyLogHasNoKnownOdometer() {
        assertNull(knownOdometerAt(emptyList(), at(5_000)))
        assertNull(currentOdometer(emptyList()))
    }

    @Test
    fun distanceEntriesWithoutAnOdometerSettingEventKnowNothing() {
        val log = listOf(distanceEvent("d", 1_000, 30_000))
        assertNull(knownOdometerAt(log, at(5_000)))
    }

    @Test
    fun theInitialEventAloneSetsTheOdometerFromItsTimeOn() {
        val log = listOf(initial)
        assertNull(knownOdometerAt(log, at(999)))
        assertEquals(Distance(45_200_000), knownOdometerAt(log, at(1_000)))
        assertEquals(Distance(45_200_000), knownOdometerAt(log, at(9_999_999)))
    }

    @Test
    fun anEntryBeforeTheInitialEventIsIgnored() {
        val log = listOf(distanceEvent("d", 500, 30_000), initial)
        assertNull(knownOdometerAt(log, at(700)))
        assertEquals(Distance(45_200_000), knownOdometerAt(log, at(2_000)))
        assertEquals(Distance(45_200_000), currentOdometer(log))
    }

    @Test
    fun severalEntriesAddUp() {
        val log = listOf(initial, distanceEvent("a", 2_000, 30_000), distanceEvent("b", 3_000, 20_000), distanceEvent("c", 4_000, 500))
        assertEquals(Distance(45_250_500), currentOdometer(log))
        assertEquals(Distance(45_250_500), knownOdometerAt(log, at(4_000)))
    }

    @Test
    fun theKnownOdometerFollowsTheChosenTime() {
        val log = listOf(initial, distanceEvent("a", 2_000, 30_000), distanceEvent("b", 3_000, 20_000))
        assertEquals(Distance(45_200_000), knownOdometerAt(log, at(1_500)))
        assertEquals(Distance(45_230_000), knownOdometerAt(log, at(2_500)))
        assertEquals(Distance(45_230_000), knownOdometerAt(log, at(2_000)))
        assertEquals(Distance(45_250_000), knownOdometerAt(log, at(3_000)))
    }

    @Test
    fun anEntryAtTheSameInstantAsTheInitialEventCountsWhenAddedAfterIt() {
        val log = listOf(initial, distanceEvent("d", 1_000, 30_000))
        assertEquals(Distance(45_230_000), knownOdometerAt(log, at(1_000)))
    }

    @Test
    fun anEntryAtTheSameInstantAddedBeforeTheInitialEventIsReplacedByIt() {
        val log = listOf(distanceEvent("d", 1_000, 30_000), initial)
        assertEquals(Distance(45_200_000), knownOdometerAt(log, at(1_000)))
    }

    @Test
    fun aLaterOdometerSettingEventReplacesTheRunningTotal() {
        val second = initialEvent("s", 3_000, 50_000_000)
        val log = listOf(initial, distanceEvent("a", 2_000, 30_000), second, distanceEvent("b", 4_000, 10_000))
        assertEquals(Distance(45_230_000), knownOdometerAt(log, at(2_500)))
        assertEquals(Distance(50_000_000), knownOdometerAt(log, at(3_000)))
        assertEquals(Distance(50_010_000), currentOdometer(log))
    }

    @Test
    fun aTypedCountOnAnEntryNeverChangesTheDerivedOdometer() {
        val log = listOf(initial, distanceEvent("a", 2_000, 50_000, loggedOdometer = 99_999_000))
        assertEquals(Distance(45_250_000), currentOdometer(log))
    }

    @Test
    fun theZoneAnEventWasEnteredInDoesNotMatterOnlyItsInstant() {
        val helsinki = ZonedMoment.of(at(2_000), TimeZone.of("Europe/Helsinki"))
        val newYork = ZonedMoment.of(at(2_000), TimeZone.of("America/New_York"))
        val a = listOf(initial, VehicleEvent.DistanceEntry("a", helsinki, Distance(30_000)))
        val b = listOf(initial, VehicleEvent.DistanceEntry("a", newYork, Distance(30_000)))
        assertEquals(currentOdometer(a), currentOdometer(b))
    }

    @Test
    fun eventsAtTheSameInstantInDifferentZonesCountInTheOrderOfTheList() {
        val helsinki = ZonedMoment.of(at(2_000), TimeZone.of("Europe/Helsinki"))
        val newYork = ZonedMoment.of(at(2_000), TimeZone.of("America/New_York"))
        val baseline = VehicleEvent.InitialOdometer("b", newYork, Distance(50_000_000))
        val entry = VehicleEvent.DistanceEntry("e", helsinki, Distance(30_000))

        // The entry after the baseline in the list counts; the same entry before it is replaced by it.
        assertEquals(Distance(50_030_000), currentOdometer(listOf(initial, baseline, entry)))
        assertEquals(Distance(50_000_000), currentOdometer(listOf(initial, entry, baseline)))
        assertEquals(Distance(50_030_000), knownOdometerAt(listOf(initial, baseline, entry), at(2_000)))
    }

    @Test
    fun anAnchorBeforeTheInitialEventIsTheBaselineUntilTheInitialEvent() {
        val anchor = anchorEvent("a", 500, 44_000_000)
        val log = listOf(anchor, distanceEvent("d", 700, 30_000), initial, distanceEvent("e", 2_000, 10_000))

        assertNull(knownOdometerAt(log, at(499)))
        assertEquals(Distance(44_000_000), knownOdometerAt(log, at(600)))
        assertEquals(Distance(44_030_000), knownOdometerAt(log, at(999)))
        // The initial event replaces the running total from its time on.
        assertEquals(Distance(45_200_000), knownOdometerAt(log, at(1_000)))
        assertEquals(Distance(45_210_000), currentOdometer(log))
    }

    @Test
    fun aLaterAnchorReplacesTheRunningTotal() {
        val log = listOf(initial, distanceEvent("d", 2_000, 30_000), anchorEvent("a", 3_000, 45_300_000))

        assertEquals(Distance(45_230_000), knownOdometerAt(log, at(2_999)))
        assertEquals(Distance(45_300_000), currentOdometer(log))
    }

    @Test
    fun anAnchorAloneKnowsItsReadingFromItsTimeOn() {
        val log = listOf(anchorEvent("a", 1_000, 0))

        assertNull(knownOdometerAt(log, at(999)))
        assertEquals(Distance(0), knownOdometerAt(log, at(1_000)))
    }
}
