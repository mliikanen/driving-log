package com.mikonoma.drivinglog.vehicle.format

import com.mikonoma.drivinglog.locale.TimeFormat
import com.mikonoma.drivinglog.vehicle.domain.EventZone
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class FormatMomentTest {

    private val helsinki = TimeZone.of("Europe/Helsinki")
    private val newYork = TimeZone.of("America/New_York")
    private val noon = Instant.parse("2026-09-20T12:00:00Z")

    @Test
    fun aMomentFromAnotherZoneShowsItsOwnWallClockTimeAndTheZoneName() {
        // 08:30 in New York is 12:30 UTC.
        val moment = ZonedMoment.of(Instant.parse("2026-09-20T12:30:00Z"), newYork)
        assertEquals("2026-09-20 08:30 (America/New_York)", formatMoment(moment, deviceZone = helsinki))
    }

    @Test
    fun aMomentFromTheDeviceZoneShowsNoZoneName() {
        val moment = ZonedMoment.of(Instant.parse("2026-09-20T12:30:00Z"), helsinki)
        assertEquals("2026-09-20 15:30", formatMoment(moment, deviceZone = helsinki))
    }

    @Test
    fun aMomentStaysInTheZoneItWasEnteredInWhenTheDeviceZoneChanges() {
        val moment = ZonedMoment.of(Instant.parse("2026-09-20T15:30:00Z"), helsinki) // 18:30 in Helsinki

        assertEquals("2026-09-20 18:30", formatMoment(moment, deviceZone = helsinki))
        assertEquals("2026-09-20 18:30 (Europe/Helsinki)", formatMoment(moment, deviceZone = newYork))
    }

    @Test
    fun aMomentWithoutAZoneIsShownInTheDeviceZoneWithNoZoneName() {
        val legacy = ZonedMoment(noon)
        assertEquals("2026-09-20 15:00", formatMoment(legacy, deviceZone = helsinki))
        assertEquals("2026-09-20 08:00", formatMoment(legacy, deviceZone = newYork))
    }

    @Test
    fun aZoneTheDeviceDoesNotKnowIsShownFromItsStoredOffset() {
        val moment = ZonedMoment(noon, EventZone("Mars/Olympus_Mons", 5 * 3600 + 1800))
        assertEquals("2026-09-20 17:30 (Mars/Olympus_Mons)", formatMoment(moment, deviceZone = helsinki))
    }

    @Test
    fun theStoredOffsetWinsOverWhatTheZoneRulesSayNow() {
        // Stored as Helsinki +02:00 although Helsinki is +03:00 in September: the wall-clock time as entered is kept.
        val moment = ZonedMoment(noon, EventZone("Europe/Helsinki", 2 * 3600))
        assertEquals("2026-09-20 14:00", formatMoment(moment, deviceZone = helsinki))
    }

    @Test
    fun theFormatIsFixedWithZeroPadding() {
        val moment = ZonedMoment.of(Instant.parse("2026-01-05T03:07:00Z"), TimeZone.UTC)
        assertEquals("2026-01-05 03:07 (UTC)", formatMoment(moment, deviceZone = helsinki))
        assertEquals("2026-01-05 03:07", formatMoment(moment, deviceZone = TimeZone.UTC))
    }

    private val h12 = TimeFormat(is24Hour = false)

    @Test
    fun aTwelveHourSettingShowsTheEnteredTimeWithAMarkerInTheEnteredZone() {
        val moment = ZonedMoment.of(Instant.parse("2026-09-20T12:30:00Z"), newYork) // 08:30 in New York

        assertEquals("2026-09-20 8:30 AM (America/New_York)", formatMoment(moment, helsinki, h12))
    }

    @Test
    fun aTwelveHourSettingForAMomentFromTheDeviceZoneShowsNoZoneName() {
        val moment = ZonedMoment.of(Instant.parse("2026-09-20T12:30:00Z"), helsinki) // 15:30 in Helsinki

        assertEquals("2026-09-20 3:30 PM", formatMoment(moment, helsinki, h12))
    }

    @Test
    fun aMomentWithoutAZoneUsesTheClockSettingToo() {
        assertEquals("2026-09-20 3:00 PM", formatMoment(ZonedMoment(noon), helsinki, h12))
    }

    @Test
    fun theSettingChangesOnlyTheTimeNotTheZoneOrTheDate() {
        val moment = ZonedMoment.of(Instant.parse("2026-09-20T22:30:00Z"), newYork) // 18:30 in New York
        assertEquals("2026-09-20 18:30 (America/New_York)", formatMoment(moment, helsinki, TimeFormat(is24Hour = true)))
        assertEquals("2026-09-20 6:30 PM (America/New_York)", formatMoment(moment, helsinki, h12))
    }
}
