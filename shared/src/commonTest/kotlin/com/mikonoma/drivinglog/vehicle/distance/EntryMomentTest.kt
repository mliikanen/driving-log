package com.mikonoma.drivinglog.vehicle.distance

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Instant

class EntryMomentTest {

    @Test
    fun aWallClockTimeAndAZoneMakeAnInstantAndRecordTheZone() {
        val moment = momentOf(LocalDateTime(2026, 9, 20, 18, 30), "Europe/Helsinki")

        assertEquals(Instant.parse("2026-09-20T15:30:00Z"), moment.instant)
        assertEquals("Europe/Helsinki", moment.zone?.id)
        assertEquals(3 * 3600, moment.zone?.offsetSeconds)
    }

    @Test
    fun theSameWallClockTimeInTwoZonesIsTwoInstants() {
        val wall = LocalDateTime(2026, 9, 20, 18, 30)
        val helsinki = momentOf(wall, "Europe/Helsinki")
        val newYork = momentOf(wall, "America/New_York")

        assertNotEquals(helsinki.instant, newYork.instant)
        assertEquals(Instant.parse("2026-09-20T22:30:00Z"), newYork.instant)
        assertEquals(-4 * 3600, newYork.zone?.offsetSeconds)
        // Either way the wall-clock time reads back as it was entered.
        assertEquals(wall, helsinki.localDateTime)
        assertEquals(wall, newYork.localDateTime)
    }

    @Test
    fun midnightInAZoneIsResolvedInThatZone() {
        val moment = momentOf(LocalDateTime(2026, 9, 20, 0, 0), "America/New_York")
        assertEquals(Instant.parse("2026-09-20T04:00:00Z"), moment.instant)
    }

    @Test
    fun aTimeInADaylightSavingGapIsResolvedForward() {
        // Helsinki jumps from 03:00 to 04:00 on 2026-03-29, so 03:30 does not exist.
        val moment = momentOf(LocalDateTime(2026, 3, 29, 3, 30), "Europe/Helsinki")

        assertEquals(Instant.parse("2026-03-29T01:30:00Z"), moment.instant)
        assertEquals(3 * 3600, moment.zone?.offsetSeconds)
        assertEquals(LocalDateTime(2026, 3, 29, 4, 30), moment.localDateTime)
    }

    @Test
    fun aTimeThatOccursTwiceTakesTheEarlierOffset() {
        // Helsinki goes back from 04:00 to 03:00 on 2026-10-25, so 03:30 happens twice.
        val moment = momentOf(LocalDateTime(2026, 10, 25, 3, 30), "Europe/Helsinki")

        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), moment.instant)
        assertEquals(3 * 3600, moment.zone?.offsetSeconds)
    }

    @Test
    fun theOffsetInWinterIsTheWinterOffset() {
        assertEquals(2 * 3600, momentOf(LocalDateTime(2026, 1, 15, 12, 0), "Europe/Helsinki").zone?.offsetSeconds)
    }

    @Test
    fun theFormOpensAtTheCurrentTimeToTheMinuteInTheDeviceZone() {
        val now = Instant.parse("2026-09-20T12:34:56Z")

        assertEquals(LocalDateTime(2026, 9, 20, 15, 34), openedAt(now, TimeZone.of("Europe/Helsinki")))
        assertEquals(LocalDateTime(2026, 9, 20, 8, 34), openedAt(now, TimeZone.of("America/New_York")))
    }

    @Test
    fun theDefaultMomentIsNeverInTheFuture() {
        val now = Instant.parse("2026-09-20T12:34:56Z")
        val zone = TimeZone.of("Europe/Helsinki")

        val moment = momentOf(openedAt(now, zone), zone.id)

        assertEquals(true, moment.instant <= now)
    }

    @Test
    fun theDatePickerDateIsTheChosenDayWhateverTheDeviceZone() {
        // Material's date picker reports midnight UTC of the chosen day.
        val utcMidnight = Instant.parse("2026-09-20T00:00:00Z").toEpochMilliseconds()

        assertEquals(LocalDate(2026, 9, 20), dateFromPicker(utcMidnight))
        assertEquals(utcMidnight, dateToPicker(LocalDate(2026, 9, 20)))
    }

    @Test
    fun replacingTheDateOrTheTimeKeepsTheOtherPart() {
        val local = LocalDateTime(2026, 9, 20, 18, 30)

        assertEquals(LocalDateTime(2026, 9, 19, 18, 30), withDate(local, LocalDate(2026, 9, 19)))
        assertEquals(LocalDateTime(2026, 9, 20, 7, 5), withTime(local, 7, 5))
    }

    @Test
    fun changingTheZoneKeepsTheWallClockTimeAndChangesTheInstant() {
        val wall = LocalDateTime(2026, 9, 20, 18, 30)
        val before = momentOf(wall, "Europe/Helsinki")

        val after = momentOf(wall, "America/New_York")

        assertEquals(before.localDateTime, after.localDateTime)
        assertNotEquals(before.instant, after.instant)
    }
}
