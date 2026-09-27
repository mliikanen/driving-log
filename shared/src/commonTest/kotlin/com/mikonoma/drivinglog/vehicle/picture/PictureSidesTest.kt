package com.mikonoma.drivinglog.vehicle.picture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PictureSidesTest {

    @Test
    fun aLargeCropGivesTheFullSizes() {
        assertEquals(PictureSides(256, 1024), pictureSides(3000))
    }

    @Test
    fun aCropOf400IsNotEnlargedForTheLargeVersion() {
        assertEquals(PictureSides(256, 400), pictureSides(400))
    }

    @Test
    fun aCropOf200IsNotEnlargedAtAll() {
        assertEquals(PictureSides(200, 200), pictureSides(200))
    }

    @Test
    fun exactlyTheSmallSideGivesEqualVersions() {
        assertEquals(PictureSides(256, 256), pictureSides(256))
    }

    @Test
    fun justAboveTheSmallSideGivesTheCropForTheLargeVersion() {
        assertEquals(PictureSides(256, 257), pictureSides(257))
    }

    @Test
    fun exactlyTheLargeSideIsKeptAndOneAboveIsScaledDown() {
        assertEquals(PictureSides(256, 1024), pictureSides(1024))
        assertEquals(PictureSides(256, 1024), pictureSides(1025))
    }

    @Test
    fun theLargeVersionIsNeverSmallerThanTheSmallOne() {
        for (side in listOf(1, 2, 100, 255, 256, 257, 512, 1023, 1024, 1025, 4096)) {
            val sides = pictureSides(side)
            assertEquals(true, sides.large >= sides.small, "crop $side")
            assertEquals(true, sides.small <= side && sides.large <= side, "crop $side is not enlarged")
        }
    }

    @Test
    fun aCropWithNoSizeIsRefused() {
        assertFailsWith<IllegalArgumentException> { pictureSides(0) }
        assertFailsWith<IllegalArgumentException> { pictureSides(-5) }
    }

    // ---- The scaling steps

    @Test
    fun aBigReductionIsMadeInHalvingSteps() {
        assertEquals(listOf(1500, 750, 375, 256), downscaleSteps(3000, 256))
        assertEquals(listOf(1024), downscaleSteps(2048, 1024))
        assertEquals(listOf(512, 256), downscaleSteps(1024, 256))
    }

    @Test
    fun noStepIsMoreThanAHalvingAndTheLastIsTheSizeWanted() {
        for (from in listOf(257, 300, 511, 512, 513, 1000, 1023, 1024, 2047, 3072)) {
            for (to in listOf(128, 256, 1024)) {
                val steps = downscaleSteps(from, to)
                if (from <= to) { assertEquals(emptyList(), steps); continue }
                assertEquals(to, steps.last(), "$from to $to ends at the size wanted")
                var before = from
                for (step in steps) {
                    assertTrue(step >= before / 2, "$before to $step is at most a halving")
                    assertTrue(step < before, "$before to $step is a reduction")
                    before = step
                }
            }
        }
    }

    @Test
    fun aPictureIsNeverEnlargedAndAnEqualSizeNeedsNoStep() {
        assertEquals(emptyList(), downscaleSteps(200, 256))
        assertEquals(emptyList(), downscaleSteps(256, 256))
    }

    // ---- Aspect-ratio-preserving scaling for an uncropped event photo (add-event-pictures)

    @Test
    fun aLargePhotoIsDownscaledKeepingItsAspectRatio() {
        assertEquals(2048 to 1536, scaledToFit(4000, 3000, 2048))
        assertEquals(256 to 192, scaledToFit(4000, 3000, 256))
    }

    @Test
    fun aSmallPhotoIsNotEnlarged() {
        assertEquals(600 to 400, scaledToFit(600, 400, 2048))
        assertEquals(256 to 171, scaledToFit(600, 400, 256))
    }

    @Test
    fun aPortraitPhotoScalesByItsTallerSide() {
        assertEquals(1536 to 2048, scaledToFit(3000, 4000, 2048))
    }

    @Test
    fun exactlyTheCapIsKeptAsIs() {
        assertEquals(2048 to 1536, scaledToFit(2048, 1536, 2048))
    }

    @Test
    fun theLongerSideNeverExceedsTheCapAndNeitherSideIsEnlarged() {
        for (width in listOf(100, 600, 2048, 4000)) for (height in listOf(100, 400, 2048, 3000)) for (cap in listOf(256, 2048)) {
            val (w, h) = scaledToFit(width, height, cap)
            assertTrue(maxOf(w, h) <= cap, "$width x $height capped at $cap")
            assertTrue(w <= width && h <= height, "$width x $height capped at $cap is not enlarged")
        }
    }

    @Test
    fun aPhotoWithNoSizeIsRefused() {
        assertFailsWith<IllegalArgumentException> { scaledToFit(0, 100, 256) }
        assertFailsWith<IllegalArgumentException> { scaledToFit(100, 0, 256) }
        assertFailsWith<IllegalArgumentException> { scaledToFit(100, 100, 0) }
    }
}
