package com.mikonoma.drivinglog.vehicle.color

import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.picture.PixelSamples
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ColorExtractorTest {
    private val extractor = HistogramColorExtractor()

    private val red = 0xFFE53935.toInt()
    private val blue = 0xFF1E88E5.toInt()
    private val green = 0xFF43A047.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val darkGrey = 0xFF303030.toInt()
    private val clear = 0x00000000

    private fun image(size: Int = 100, pixel: (x: Int, y: Int) -> Int) =
        PixelSamples(size, size, IntArray(size * size) { pixel(it % size, it / size) })

    @Test
    fun aSolidColorGivesThatColor() {
        assertEquals(Rgb(0xE53935), extractor.extract(image { _, _ -> red }))
    }

    @Test
    fun theLargerOfTwoHalvesWins() {
        // 70% of the width is red, 30% is blue: red is the most common color of the middle.
        val samples = image { x, _ -> if (x < 70) red else blue }

        assertEquals(Rgb(0xE53935), extractor.extract(samples))
    }

    @Test
    fun aTieGoesToTheLowerColorValue() {
        val samples = image { x, _ -> if (x < 50) red else blue }

        assertEquals(Rgb(0x1E88E5), extractor.extract(samples))
    }

    @Test
    fun whiteWithALittleDarkGreyIsWhite() {
        val samples = image { x, y -> if (x in 45..55 && y in 45..55) darkGrey else white }

        assertEquals(Rgb(0xFFFFFF), extractor.extract(samples))
    }

    @Test
    fun aBlackPhotoIsBlack() {
        assertEquals(Rgb(0x000000), extractor.extract(image { _, _ -> 0xFF000000.toInt() }))
    }

    @Test
    fun aGreyPhotoKeepsItsGrey() {
        assertEquals(Rgb(0x808080), extractor.extract(image { _, _ -> 0xFF808080.toInt() }))
    }

    @Test
    fun aRedSquareFillingTheMiddleInABlueBorderIsRed() {
        // The border is 10% on each side, which the extraction leaves out.
        val samples = image { x, y -> if (x in 10..89 && y in 10..89) red else blue }

        assertEquals(Rgb(0xE53935), extractor.extract(samples))
    }

    @Test
    fun theBorderCountsLessThanTheMiddle() {
        // Blue is the more common color of the whole image (51%), but red fills most of the middle (4900 of its 6400 pixels).
        val samples = image { x, y -> if (x in 15..84 && y in 15..84) red else blue }

        assertEquals(Rgb(0xE53935), extractor.extract(samples))
    }

    @Test
    fun aFullyTransparentImageHasNoColor() {
        assertNull(extractor.extract(image { _, _ -> clear }))
    }

    @Test
    fun transparentPixelsAreNotColors() {
        // Most of the picture is transparent; the opaque part is green.
        val samples = image { x, _ -> if (x < 20) green else clear }

        assertEquals(Rgb(0x43A047), extractor.extract(samples))
    }

    @Test
    fun aHalfTransparentPixelWithAlphaBelow128IsNotAColor() {
        val samples = image { _, _ -> 0x7FE53935 }

        assertNull(extractor.extract(samples))
    }

    @Test
    fun fewerThanOnePercentOpaqueHasNoColor() {
        // One opaque pixel in the 80 x 80 middle (6400 pixels) is far below 1%.
        val samples = image { x, y -> if (x == 50 && y == 50) red else clear }

        assertNull(extractor.extract(samples))
    }

    @Test
    fun aVerySmallImageStillGivesItsColor() {
        assertEquals(Rgb(0xE53935), extractor.extract(image(size = 4) { _, _ -> red }))
    }

    @Test
    fun theSameSamplesGiveTheSameColorTwice() {
        val samples = image { x, y -> if ((x / 7 + y / 5) % 3 == 0) red else if ((x + y) % 2 == 0) blue else green }

        assertEquals(extractor.extract(samples), extractor.extract(samples))
        assertEquals(extractor.extract(samples), HistogramColorExtractor().extract(samples))
    }

    @Test
    fun aSampleMustFillItsSize() {
        assertFailsWith<IllegalArgumentException> { PixelSamples(2, 2, IntArray(3)) }
    }
}
