package com.mikonoma.drivinglog.vehicle.format

import com.mikonoma.drivinglog.locale.TimeFormat
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatTimeTest {

    private val h24 = TimeFormat(is24Hour = true)
    private val h12 = TimeFormat(is24Hour = false, amMarker = "AM", pmMarker = "PM")

    @Test
    fun aTwentyFourHourTimeIsZeroPadded() {
        assertEquals("00:00", formatTimeOfDay(0, 0, h24))
        assertEquals("03:07", formatTimeOfDay(3, 7, h24))
        assertEquals("16:30", formatTimeOfDay(16, 30, h24))
        assertEquals("23:59", formatTimeOfDay(23, 59, h24))
    }

    @Test
    fun aTwelveHourTimeHasNoLeadingZeroInTheHourAndAMarker() {
        assertEquals("3:07 AM", formatTimeOfDay(3, 7, h12))
        assertEquals("4:30 PM", formatTimeOfDay(16, 30, h12))
        assertEquals("11:59 PM", formatTimeOfDay(23, 59, h12))
    }

    @Test
    fun midnightIsTwelveAmAndNoonIsTwelvePm() {
        assertEquals("12:00 AM", formatTimeOfDay(0, 0, h12))
        assertEquals("12:15 AM", formatTimeOfDay(0, 15, h12))
        assertEquals("12:00 PM", formatTimeOfDay(12, 0, h12))
        assertEquals("1:00 PM", formatTimeOfDay(13, 0, h12))
        assertEquals("11:59 AM", formatTimeOfDay(11, 59, h12))
    }

    @Test
    fun theDevicesOwnMarkersAreUsed() {
        val finnish = TimeFormat(is24Hour = false, amMarker = "ap.", pmMarker = "ip.")
        assertEquals("9:05 ap.", formatTimeOfDay(9, 5, finnish))
        assertEquals("9:05 ip.", formatTimeOfDay(21, 5, finnish))
    }

    @Test
    fun theDefaultIsTwentyFourHour() {
        assertEquals("16:30", formatTimeOfDay(16, 30))
    }

    @Test
    fun theDateStaysYearMonthDayWhateverTheClock() {
        val t = LocalDateTime(2026, 9, 5, 16, 30)
        assertEquals("2026-09-05 16:30", formatLocalDateTime(t, h24))
        assertEquals("2026-09-05 4:30 PM", formatLocalDateTime(t, h12))
    }
}
