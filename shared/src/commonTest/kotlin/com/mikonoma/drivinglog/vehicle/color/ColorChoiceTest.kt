package com.mikonoma.drivinglog.vehicle.color

import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorChoiceTest {
    private val red = Rgb(0xE53935)
    private val teal = Rgb(0x00796B)
    private val custom = Rgb(0x123ABC)
    private val other = Rgb(0xABC123)

    /** The names of the selected elements: presets and segments together. */
    private fun selected(model: ColorChoiceModel) =
        model.palette.filter { it.selected }.map { it.name } + model.segments.filter { it.selected }.map { it.label }

    @Test
    fun theDefaultOffersTheTwelvePresetsAndNoRow() {
        val model = colorChoice(VehicleColors.default, pictureColor = null, savedColor = null)

        assertEquals(VehicleColors.presets.map { it.name }, model.palette.map { it.name })
        assertEquals(emptyList(), model.segments)
        assertEquals(listOf("Petroleum"), selected(model))
    }

    @Test
    fun exactlyOneElementIsSelectedForEveryPreset() {
        for (preset in VehicleColors.presets) {
            assertEquals(listOf(preset.name), selected(colorChoice(preset.color, null, null)), preset.name)
        }
    }

    @Test
    fun thePhotoColorIsASegmentAndSelectedWhileItIsTheCurrentColor() {
        val model = colorChoice(custom, pictureColor = custom, savedColor = null)

        assertEquals(listOf("Photo color"), model.segments.map { it.label })
        assertEquals(custom, model.segments.single().color)
        assertEquals(listOf("Photo color"), selected(model))
    }

    @Test
    fun choosingAPresetAfterThePhotoKeepsThePhotoColorOfferedButNotSelected() {
        val model = colorChoice(Rgb(0x1E88E5), pictureColor = custom, savedColor = null)

        assertEquals(listOf("Photo color"), model.segments.map { it.label })
        assertEquals(listOf("Blue"), selected(model))
    }

    @Test
    fun onTheEditScreenTheSavedColorIsAlwaysTheOldColorSegment() {
        val model = colorChoice(teal, pictureColor = null, savedColor = teal)

        assertEquals(listOf("Old color"), model.segments.map { it.label })
        assertEquals(teal, model.segments.single().color)
    }

    @Test
    fun aSavedPresetIsSelectedAsThePresetAndNotAlsoAsTheOldColor() {
        val model = colorChoice(teal, pictureColor = null, savedColor = teal)

        assertEquals(listOf("Teal"), selected(model))
    }

    @Test
    fun aSavedColorThatIsNotAPresetIsTheSelectedOldColor() {
        val model = colorChoice(custom, pictureColor = null, savedColor = custom)

        assertEquals(listOf("Old color"), selected(model))
        assertTrue(model.palette.none { it.selected })
    }

    @Test
    fun goingBackToTheOldColorSelectsItAgain() {
        val afterChoosing = colorChoice(red, pictureColor = null, savedColor = custom)
        assertEquals(listOf("Red"), selected(afterChoosing))

        val back = colorChoice(custom, pictureColor = null, savedColor = custom)
        assertEquals(listOf("Old color"), selected(back))
    }

    @Test
    fun theOldAndThePhotoColorShareTheRowInThatOrder() {
        val model = colorChoice(other, pictureColor = other, savedColor = custom)

        assertEquals(listOf("Old color", "Photo color"), model.segments.map { it.label })
        assertEquals(listOf("Photo color"), selected(model))
    }

    @Test
    fun whenThePhotoColorAndTheOldColorAreTheSameColorThePhotoOneIsSelected() {
        val model = colorChoice(custom, pictureColor = custom, savedColor = custom)

        assertEquals(listOf("Photo color"), selected(model))
    }

    @Test
    fun aPhotoColorThatIsAPresetIsSelectedAsThePresetAndStillOffered() {
        val model = colorChoice(red, pictureColor = red, savedColor = null)

        assertEquals(listOf("Red"), selected(model))
        assertEquals(listOf("Photo color"), model.segments.map { it.label })
    }

    @Test
    fun aCurrentColorThatIsNoneOfTheOthersGetsItsOwnSegmentAfterThePictureWasRemoved() {
        val addModel = colorChoice(custom, pictureColor = null, savedColor = null)
        assertEquals(listOf("Current color"), addModel.segments.map { it.label })
        assertEquals(listOf("Current color"), selected(addModel))

        val editModel = colorChoice(other, pictureColor = null, savedColor = custom)
        assertEquals(listOf("Old color", "Current color"), editModel.segments.map { it.label })
        assertEquals(listOf("Current color"), selected(editModel))
    }

    @Test
    fun noSegmentIsSelectedTwiceAndNeverMoreThanOneElementIsSelected() {
        val colors = listOf(VehicleColors.default, red, teal, custom, other)
        for (color in colors) {
            for (picture in listOf(null, red, custom, other)) {
                for (saved in listOf(null, teal, custom)) {
                    val model = colorChoice(color, picture, saved)
                    assertEquals(1, selected(model).size, "color ${color.hex} picture ${picture?.hex} saved ${saved?.hex}")
                    assertTrue(model.segments.size <= 3)
                }
            }
        }
    }

    @Test
    fun everyElementHasADistinctTestTag() {
        val model = colorChoice(other, pictureColor = custom, savedColor = teal)

        val tags = model.palette.map { it.tag } + model.segments.map { it.tag }
        assertEquals(tags.size, tags.toSet().size)
        assertTrue("vehicle_color_203A43" in tags)
        assertTrue("vehicle_color_old" in tags)
        assertTrue("vehicle_color_picture" in tags)
        assertTrue("vehicle_color_current" in tags)
    }
}
