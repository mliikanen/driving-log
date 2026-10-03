package com.mikonoma.drivinglog.vehicle.distance

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

/** The picker's list built from the platform's real zone ids (here the JVM's `java.time` list). */
class PlatformTimeZonesTest {

    private val at = Instant.parse("2026-09-20T12:00:00Z")
    private val choices = timeZoneChoices(TimeZone.availableZoneIds, "Europe/Helsinki", at)
    private val ids = choices.map { it.id }

    @Test
    fun theRawPlatformListHasLegacyAliasesThePickerDrops() {
        val raw = TimeZone.availableZoneIds
        assertTrue("Brazil/East" in raw || "CET" in raw || "US/Eastern" in raw, "expected legacy aliases in the raw list")
        for (legacy in listOf("Brazil/East", "CET", "CST6CDT", "Canada/Eastern", "US/Eastern")) assertFalse(legacy in ids, legacy)
    }

    @Test
    fun thePickerListsTheReadableZones() {
        for (zone in listOf("Europe/Helsinki", "America/New_York", "Asia/Kolkata", "Pacific/Auckland", "UTC")) assertTrue(zone in ids, zone)
        assertTrue(ids.size in 300..600, "unexpected number of zones: ${ids.size}")
    }

    @Test
    fun theDeviceZoneIsFirstAndSearchFindsNewYork() {
        assertEquals("Europe/Helsinki", ids.first())
        assertEquals(listOf("America/New_York"), timeZoneChoices(TimeZone.availableZoneIds, "Europe/Helsinki", at, "new_y").map { it.id })
    }

    @Test
    fun everyOfferedZoneResolvesToAnOffset() {
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(choices.all { it.offsetSeconds in -14 * 3600..14 * 3600 })
    }
}
