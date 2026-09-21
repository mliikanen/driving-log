package com.mikonoma.drivinglog.ui.color

import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LerpHctTest {
    private val red = Rgb(0xE53935)
    private val blue = Rgb(0x1E88E5)

    @Test
    fun theEndsAreExact() {
        assertEquals(red, lerpHct(red, blue, 0f))
        assertEquals(blue, lerpHct(red, blue, 1f))
        assertEquals(red, lerpHct(red, blue, -0.5f))
        assertEquals(blue, lerpHct(red, blue, 1.5f))
    }

    @Test
    fun theSameColorStaysTheSameOnTheWay() {
        assertEquals(red, lerpHct(red, red, 0.4f))
    }

    @Test
    fun theHueTakesTheShortWayAcrossZero() {
        val mid = lerpHctValues(HctValue(350.0, 40.0, 50.0), HctValue(10.0, 40.0, 50.0), 0.5)

        assertEquals(0.0, hueDistance(mid.hue, 0.0), 1e-9)
    }

    @Test
    fun theHueTakesTheShortWayTheOtherWayToo() {
        val mid = lerpHctValues(HctValue(10.0, 40.0, 50.0), HctValue(350.0, 40.0, 50.0), 0.25)

        // 10 degrees, a quarter of the way to 350 by way of 0: 5 degrees.
        assertEquals(5.0, mid.hue, 1e-9)
    }

    @Test
    fun theLongWayIsNotTakenAcrossHalfAWheel() {
        val mid = lerpHctValues(HctValue(0.0, 40.0, 50.0), HctValue(120.0, 40.0, 50.0), 0.5)

        assertEquals(60.0, mid.hue, 1e-9)
    }

    @Test
    fun chromaAndToneMoveInAStraightLine() {
        val mid = lerpHctValues(HctValue(100.0, 10.0, 20.0), HctValue(100.0, 50.0, 80.0), 0.25)

        assertEquals(20.0, mid.chroma, 1e-9)
        assertEquals(35.0, mid.tone, 1e-9)
    }

    @Test
    fun aGreyTakesTheOtherColorsHueSoNothingSwingsThroughTheWheel() {
        val mid = lerpHctValues(HctValue(250.0, 0.5, 30.0), HctValue(20.0, 60.0, 60.0), 0.5)

        assertEquals(20.0, mid.hue, 1e-9)
        assertEquals(30.25, mid.chroma, 1e-9)
    }

    @Test
    fun theWayBetweenTwoGreysStaysGrey() {
        val white = Rgb(0xFFFFFF)
        val black = Rgb(0x000000)

        for (t in listOf(0.1f, 0.5f, 0.9f)) assertTrue(HctColors.read(lerpHct(white, black, t)).chroma < 3.0, "t=$t")
    }

    @Test
    fun theToneGrowsFromDarkToLightAlongTheWay() {
        val dark = Rgb(0x203A43)
        val light = Rgb(0xE0F0F5)
        var last = -1.0
        for (t in listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)) {
            val tone = HctColors.read(lerpHct(dark, light, t)).tone
            assertTrue(tone > last, "t=$t tone $tone after $last")
            last = tone
        }
    }

    @Test
    fun everyStepBetweenEveryPairOfPresetsIsAValidColor() {
        for (a in VehicleColors.presets) for (b in VehicleColors.presets) for (t in listOf(0.25f, 0.5f, 0.75f)) {
            val color = lerpHct(a.color, b.color, t)
            assertTrue(color.rgb in 0..0xFFFFFF, "${a.name} to ${b.name} at $t")
        }
    }
}
