package com.mikonoma.drivinglog.ui.color

import com.mikonoma.drivinglog.ui.theme.contrastRatio
import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class VehicleTonesTest {

    /** Every color the tones must hold for: the presets, white, black, greys and a grid of hues, chromas and tones. */
    private fun sweep(): List<Pair<String, Rgb>> {
        val colors = mutableListOf<Pair<String, Rgb>>()
        for (preset in VehicleColors.presets) colors += preset.name to preset.color
        colors += "white" to Rgb(0xFFFFFF)
        colors += "black" to Rgb(0)
        for (level in 0..255 step 15) colors += "grey $level" to Rgb((level shl 16) or (level shl 8) or level)
        for (hue in 0 until 360 step 10) for (chroma in listOf(4.0, 16.0, 32.0, 60.0, 100.0)) for (tone in listOf(15.0, 50.0, 85.0)) {
            colors += "h$hue c${chroma.toInt()} t${tone.toInt()}" to HctColors.build(hue.toDouble(), chroma, tone)
        }
        return colors
    }

    @Test
    fun theIconReachesThreeToOneOnItsContainerForEveryColorInBothSchemes() {
        for ((name, color) in sweep()) for (dark in listOf(false, true)) {
            val tones = VehicleTones.of(color, dark)
            val ratio = contrastRatio(tones.icon.toColor(), tones.container.toColor())
            assertTrue(ratio >= 3.0, "$name ${if (dark) "dark" else "light"}: $ratio")
        }
    }

    @Test
    fun theIconIsInFactReadableAsTextForEveryColorToo() {
        // The tone rule gives about 4.5:1; the requirement is 3:1, so there is headroom.
        for ((name, color) in sweep()) for (dark in listOf(false, true)) {
            val tones = VehicleTones.of(color, dark)
            assertTrue(contrastRatio(tones.icon.toColor(), tones.container.toColor()) >= 4.0, "$name dark=$dark")
        }
    }

    @Test
    fun greysWhiteAndBlackGiveNeutralTones() {
        for (color in listOf(Rgb(0xFFFFFF), Rgb(0), Rgb(0x808080), Rgb(0x9E9E9E))) for (dark in listOf(false, true)) {
            val tones = VehicleTones.of(color, dark)
            assertTrue(HctColors.read(tones.icon).chroma < 4.0, "icon of ${color.hex}")
            assertTrue(HctColors.read(tones.container).chroma < 4.0, "container of ${color.hex}")
        }
    }

    @Test
    fun aColorfulColorGivesAColorfulIconOfItsHue() {
        val red = VehicleColors.presets.first { it.name == "Red" }.color

        val icon = HctColors.read(VehicleTones.of(red, dark = false).icon)

        assertTrue(icon.chroma > 20.0, "chroma ${icon.chroma}")
        assertTrue(kotlin.math.abs(icon.hue - HctColors.read(red).hue) < 8.0, "hue ${icon.hue}")
    }

    @Test
    fun theLightSchemeIsDarkOnLightAndTheDarkSchemeLightOnDark() {
        val blue = VehicleColors.presets.first { it.name == "Blue" }.color

        val light = VehicleTones.of(blue, dark = false)
        val dark = VehicleTones.of(blue, dark = true)

        assertTrue(HctColors.read(light.icon).tone < HctColors.read(light.container).tone)
        assertTrue(HctColors.read(dark.icon).tone > HctColors.read(dark.container).tone)
    }

    @Test
    fun theTonesAreTheFixedOnes() {
        val teal = VehicleColors.presets.first { it.name == "Teal" }.color

        assertEquals(40.0, HctColors.read(VehicleTones.of(teal, false).icon).tone, 1.0)
        assertEquals(90.0, HctColors.read(VehicleTones.of(teal, false).container).tone, 1.0)
        assertEquals(80.0, HctColors.read(VehicleTones.of(teal, true).icon).tone, 1.0)
        assertEquals(30.0, HctColors.read(VehicleTones.of(teal, true).container).tone, 1.0)
    }

    @Test
    fun theResultIsDeterministicAndDependsOnTheModeOnly() {
        val red = Rgb(0xE53935)

        assertEquals(VehicleTones.of(red, false), VehicleTones.of(red, false))
        assertNotEquals(VehicleTones.of(red, false), VehicleTones.of(red, true))
    }
}
