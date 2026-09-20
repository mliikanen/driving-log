package com.mikonoma.drivinglog.vehicle.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

class ZonedMomentTest {

    private val noon = Instant.parse("2026-09-20T12:00:00Z")

    @Test
    fun aMomentRecordsTheZoneIdAndItsOffsetAtThatInstant() {
        val helsinki = ZonedMoment.of(noon, TimeZone.of("Europe/Helsinki"))
        assertEquals(EventZone("Europe/Helsinki", 3 * 3600), helsinki.zone)
        val newYork = ZonedMoment.of(noon, TimeZone.of("America/New_York"))
        assertEquals(EventZone("America/New_York", -4 * 3600), newYork.zone)
    }

    @Test
    fun theOffsetIsTheOneInForceAtTheInstantNotTheZonesCurrentOne() {
        val winter = ZonedMoment.of(Instant.parse("2026-01-15T12:00:00Z"), TimeZone.of("Europe/Helsinki"))
        assertEquals(2 * 3600, winter.zone?.offsetSeconds)
    }

    @Test
    fun theWallClockTimeIsTheInstantShiftedByTheStoredOffset() {
        assertEquals(LocalDateTime(2026, 9, 20, 15, 0), ZonedMoment.of(noon, TimeZone.of("Europe/Helsinki")).localDateTime)
        assertEquals(LocalDateTime(2026, 9, 20, 8, 0), ZonedMoment.of(noon, TimeZone.of("America/New_York")).localDateTime)
    }

    @Test
    fun theWallClockTimeUsesTheStoredOffsetEvenForAZoneNoDeviceKnows() {
        val moment = ZonedMoment(noon, EventZone("Mars/Olympus_Mons", 5 * 3600 + 1800))
        assertEquals(LocalDateTime(2026, 9, 20, 17, 30), moment.localDateTime)
    }

    @Test
    fun aMomentWithoutAZoneHasNoWallClockTime() {
        assertNull(ZonedMoment(noon).localDateTime)
    }

    @Test
    fun theSameInstantInTwoZonesIsTheSameInstant() {
        val a = ZonedMoment.of(noon, TimeZone.of("Europe/Helsinki"))
        val b = ZonedMoment.of(noon, TimeZone.of("America/New_York"))
        assertEquals(a.instant, b.instant)
    }
}
