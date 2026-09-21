package com.mikonoma.drivinglog.vehicle.color

import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorSwatchesTest {
    private val red = Rgb(0xE53935)
    private val custom = Rgb(0x123ABC)

    private fun selected(swatches: List<ColorSwatch>) = swatches.filter { it.selected }.map { it.name }

    @Test
    fun theDefaultOffersTheTwelvePresetsWithThePetroleumOneSelected() {
        val swatches = colorSwatches(VehicleColors.default, pictureColor = null)

        assertEquals(VehicleColors.presets.map { it.name }, swatches.map { it.name })
        assertEquals(listOf("Petroleum"), selected(swatches))
    }

    @Test
    fun exactlyOneSwatchIsSelectedForEveryPreset() {
        for (preset in VehicleColors.presets) {
            assertEquals(listOf(preset.name), selected(colorSwatches(preset.color, null)), preset.name)
        }
    }

    @Test
    fun aColorThatIsNotAPresetGetsASwatchOfItsOwnBeforeThePresets() {
        val swatches = colorSwatches(custom, pictureColor = null)

        assertEquals("Current color", swatches.first().name)
        assertEquals(custom, swatches.first().color)
        assertEquals(listOf("Current color"), selected(swatches))
        assertEquals(1 + VehicleColors.presets.size, swatches.size)
    }

    @Test
    fun thePictureColorIsOfferedFirstAndSelectedWhileItIsTheCurrentColor() {
        val swatches = colorSwatches(custom, pictureColor = custom)

        assertEquals("Picture color", swatches.first().name)
        assertEquals(listOf("Picture color"), selected(swatches))
        // It is the picture color, so there is no second "Current color" swatch for the same color.
        assertTrue(swatches.none { it.name == "Current color" })
    }

    @Test
    fun choosingAPresetAfterThePictureKeepsThePictureColorOfferedButNotSelected() {
        val swatches = colorSwatches(Rgb(0x1E88E5), pictureColor = custom)

        assertEquals("Picture color", swatches.first().name)
        assertEquals(listOf("Blue"), selected(swatches))
    }

    @Test
    fun aPictureColorThatIsAPresetIsSelectedAsThePictureColorAndNotAlsoAsThePreset() {
        val swatches = colorSwatches(red, pictureColor = red)

        assertEquals(listOf("Picture color"), selected(swatches))
        assertTrue(swatches.any { it.name == "Red" && !it.selected })
    }

    @Test
    fun aCustomColorOtherThanThePictureColorKeepsItsCurrentColorSwatchAlongsideThePictureColor() {
        val swatches = colorSwatches(custom, pictureColor = red)

        assertEquals(listOf("Picture color", "Current color"), swatches.take(2).map { it.name })
        assertEquals(listOf("Current color"), selected(swatches))
    }

    @Test
    fun everySwatchHasADistinctTestTag() {
        val swatches = colorSwatches(custom, pictureColor = red)

        assertEquals(swatches.size, swatches.map { it.tag }.toSet().size)
        assertTrue(swatches.any { it.tag == "vehicle_color_203A43" })
        assertTrue(swatches.any { it.tag == "vehicle_color_picture" })
        assertTrue(swatches.any { it.tag == "vehicle_color_current" })
    }
}
