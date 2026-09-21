package com.mikonoma.drivinglog.ui

import com.mikonoma.drivinglog.landing.LandingIcon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LandingIconsTest {

    @Test
    fun everyIconHasPathDataOnA256ViewBoxThatBuilds() {
        for (icon in LandingIcon.values()) {
            val vector = LandingIcons.of(icon)

            assertEquals(256f, vector.viewportWidth, "$icon")
            assertEquals(256f, vector.viewportHeight, "$icon")
            // Building parses the path data; an invalid one would have thrown by now.
            assertTrue(vector.root.size > 0, "$icon has a path")
        }
    }

    @Test
    fun theFivePathsAreDifferent() {
        val paths = LandingIcons.pathData.values.toList()
        assertEquals(paths.size, paths.toSet().size)
        assertNotEquals(VehicleIcons.Car, LandingIcons.of(LandingIcon.ADD_VEHICLE))
    }

    @Test
    fun theVehiclesTileUsesTheGenericCar() {
        assertEquals(VehicleIcons.Car, LandingIcons.of(LandingIcon.VEHICLES))
    }

    @Test
    fun theNewPathsAreThoseOfTheOriginalSvgFiles() {
        // The files in docs/icons/phosphor are Phosphor's; a path that drifted from them would start differently.
        assertTrue(LandingIcons.ADD_VEHICLE_PATH.startsWith("M128,24A104,104,0,1,0,232,128,104.13"))
        assertTrue(LandingIcons.LOG_EVENT_PATH.startsWith("M224,128v80a16,16"))
        assertTrue(LandingIcons.TRIP_PATH.startsWith("M228,200a28,28,0,0,1-54.83,8H72"))
        assertTrue(LandingIcons.PLACEHOLDER_PATH.startsWith("M128,24A104,104,0,1,0,232,128,104.11"))
    }
}
