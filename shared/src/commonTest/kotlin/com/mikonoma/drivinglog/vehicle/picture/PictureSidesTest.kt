package com.mikonoma.drivinglog.vehicle.picture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
}
