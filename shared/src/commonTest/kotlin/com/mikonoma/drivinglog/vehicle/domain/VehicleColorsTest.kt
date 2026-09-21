package com.mikonoma.drivinglog.vehicle.domain

import androidx.compose.ui.graphics.toArgb
import com.mikonoma.drivinglog.ui.theme.FuelGaugeGold
import com.mikonoma.drivinglog.ui.theme.OilSlickBlue
import com.mikonoma.drivinglog.ui.theme.RoadTripEmerald
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VehicleColorsTest {

    @Test
    fun hexIsSixUpperCaseDigits() {
        assertEquals("203A43", Rgb(0x203A43).hex)
        assertEquals("00000F", Rgb(0xF).hex)
        assertEquals("000000", Rgb(0).hex)
        assertEquals("FFFFFF", Rgb(0xFFFFFF).hex)
    }

    @Test
    fun theArgbIsOpaque() {
        assertEquals(0xFF203A43.toInt(), Rgb(0x203A43).argb)
        assertEquals(0xFF000000.toInt(), Rgb(0).argb)
    }

    @Test
    fun fromArgbDropsTheAlpha() {
        assertEquals(Rgb(0x203A43), Rgb.fromArgb(0x80203A43.toInt()))
        assertEquals(Rgb(0xFFFFFF), Rgb.fromArgb(-1))
    }

    @Test
    fun aColorOutsideTwentyFourBitsIsRefused() {
        assertFailsWith<IllegalArgumentException> { Rgb(0x1000000) }
        assertFailsWith<IllegalArgumentException> { Rgb(-1) }
    }

    @Test
    fun parseRoundTripsEveryPreset() {
        for (preset in VehicleColors.presets) assertEquals(preset.color, Rgb.parse(preset.color.hex), preset.name)
    }

    @Test
    fun parseAcceptsEitherCase() {
        assertEquals(Rgb(0xABCDEF), Rgb.parse("abcdef"))
        assertEquals(Rgb(0xABCDEF), Rgb.parse("ABCDEF"))
    }

    @Test
    fun parseRefusesEverythingElse() {
        for (text in listOf(null, "", "20", "203A4", "203A431", "#203A43", "203A4G", " 203A43", "203A43 ", "FF203A43", "0x203A43", "-03A43")) {
            assertNull(Rgb.parse(text), "'$text'")
        }
    }

    @Test
    fun theDefaultIsTheLightThemesPrimary() {
        assertEquals(OilSlickBlue.toArgb(), VehicleColors.default.argb)
    }

    @Test
    fun thereAreTwelvePresetsWithDistinctNamesAndColors() {
        val presets = VehicleColors.presets

        assertEquals(12, presets.size)
        assertEquals(12, presets.map { it.name }.toSet().size)
        assertEquals(12, presets.map { it.color }.toSet().size)
        assertTrue(presets.all { it.name.isNotBlank() })
    }

    @Test
    fun theFirstPresetIsTheDefault() {
        assertEquals(VehicleColors.default, VehicleColors.presets.first().color)
    }

    @Test
    fun noPresetIsADomainColor() {
        for (preset in VehicleColors.presets) {
            assertNotEquals(FuelGaugeGold.toArgb(), preset.color.argb, preset.name)
            assertNotEquals(RoadTripEmerald.toArgb(), preset.color.argb, preset.name)
        }
    }

    @Test
    fun presetOfFindsAPresetAndNothingElse() {
        assertEquals("Red", VehicleColors.presetOf(Rgb(0xE53935))?.name)
        assertNull(VehicleColors.presetOf(Rgb(0x123456)))
    }
}
