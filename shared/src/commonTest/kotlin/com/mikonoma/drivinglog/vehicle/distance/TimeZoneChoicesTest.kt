package com.mikonoma.drivinglog.vehicle.distance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class TimeZoneChoicesTest {

    private val september = Instant.parse("2026-09-20T12:00:00Z")

    private val platformIds = setOf(
        "Europe/Helsinki", "America/New_York", "America/Argentina/Buenos_Aires", "Asia/Kolkata", "Pacific/Auckland",
        "Arctic/Longyearbyen", "UTC",
        // Legacy aliases and abbreviations a platform list also holds:
        "Brazil/East", "CET", "CST6CDT", "Canada/Eastern", "US/Eastern", "Etc/GMT+5", "Mexico/General",
    )

    private fun ids(query: String = "", device: String = "Europe/Helsinki", available: Set<String> = platformIds) =
        timeZoneChoices(available, device, september, query).map { it.id }

    @Test
    fun legacyAliasesAreDropped() {
        val all = ids()
        for (legacy in listOf("Brazil/East", "CET", "CST6CDT", "Canada/Eastern", "US/Eastern", "Etc/GMT+5", "Mexico/General")) {
            assertFalse(legacy in all, legacy)
        }
    }

    @Test
    fun continentCityNamesAndUtcAreKept() {
        val all = ids()
        for (kept in listOf("Europe/Helsinki", "America/New_York", "America/Argentina/Buenos_Aires", "Asia/Kolkata", "Pacific/Auckland", "Arctic/Longyearbyen", "UTC")) {
            assertTrue(kept in all, kept)
        }
    }

    @Test
    fun theDeviceZoneIsFirstWhenUnfilteredAndTheRestAreSorted() {
        val all = ids(device = "Asia/Kolkata")

        assertEquals("Asia/Kolkata", all.first())
        assertEquals(all.drop(1).sorted(), all.drop(1))
        assertEquals(all.size, all.toSet().size)
    }

    @Test
    fun aDeviceZoneThatIsNotContinentCityIsStillOffered() {
        val all = ids(device = "Etc/GMT+5")
        assertEquals("Etc/GMT+5", all.first())
    }

    @Test
    fun aDeviceZoneThePlatformCannotResolveIsLeftOutInsteadOfCrashing() {
        assertFalse("Mars/Olympus_Mons" in ids(device = "Mars/Olympus_Mons"))
    }

    @Test
    fun searchingFiltersByASubstringOfTheId() {
        assertEquals(listOf("America/New_York"), ids("new_y"))
        assertEquals(listOf("America/New_York"), ids("new y"))
        assertEquals(listOf("America/Argentina/Buenos_Aires", "America/New_York"), ids("america"))
    }

    @Test
    fun searchingIgnoresCaseAndSurroundingSpaces() {
        assertEquals(listOf("Europe/Helsinki"), ids("  HELSINKI "))
    }

    @Test
    fun aSearchWithNoMatchGivesAnEmptyList() {
        assertEquals(emptyList(), ids("zzz"))
    }

    @Test
    fun aFilteredListIsSortedAndNotDeviceFirst() {
        val all = ids("a", device = "Pacific/Auckland")
        assertEquals(all.sorted(), all)
    }

    @Test
    fun eachChoiceCarriesItsOffsetAtTheGivenMoment() {
        val choices = timeZoneChoices(platformIds, "Europe/Helsinki", september).associate { it.id to it.offsetSeconds }

        assertEquals(3 * 3600, choices["Europe/Helsinki"])
        assertEquals(-4 * 3600, choices["America/New_York"])
        assertEquals(5 * 3600 + 1800, choices["Asia/Kolkata"])
        assertEquals(0, choices["UTC"])
    }

    @Test
    fun theOffsetFollowsDaylightSavingAtTheGivenMoment() {
        val winter = Instant.parse("2026-01-15T12:00:00Z")
        val choices = timeZoneChoices(platformIds, "Europe/Helsinki", winter).associate { it.id to it.offsetSeconds }

        assertEquals(2 * 3600, choices["Europe/Helsinki"])
        assertEquals(-5 * 3600, choices["America/New_York"])
    }

    @Test
    fun offsetsAreFormatted() {
        assertEquals("UTC+03:00", formatUtcOffset(3 * 3600))
        assertEquals("UTC-04:00", formatUtcOffset(-4 * 3600))
        assertEquals("UTC+05:30", formatUtcOffset(5 * 3600 + 1800))
        assertEquals("UTC-03:30", formatUtcOffset(-(3 * 3600 + 1800)))
        assertEquals("UTC", formatUtcOffset(0))
    }
}
