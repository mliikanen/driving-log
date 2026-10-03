package com.mikonoma.drivinglog.ui

import com.mikonoma.drivinglog.vehicle.domain.VehicleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class VehicleIconsTest {

    @Test
    fun everyTypeHasAnIcon() {
        for (type in VehicleType.entries) {
            val icon = VehicleIcons.of(type)
            assertEquals(type.label, icon.name)
            assertTrue(icon.viewportWidth == 256f && icon.viewportHeight == 256f, "${type.name} view box")
        }
    }

    @Test
    fun theLookupCoversExactlyTheTypes() {
        assertEquals(VehicleType.entries.toSet(), VehicleIcons.pathData.keys)
    }

    @Test
    fun theEightIconsAreDifferentFromOneAnother() {
        val paths = VehicleType.entries.map { VehicleIcons.pathData.getValue(it) }

        assertEquals(8, paths.toSet().size)
        for (a in VehicleType.entries) {
            for (b in VehicleType.entries) {
                if (a != b) assertNotEquals(VehicleIcons.pathData[a], VehicleIcons.pathData[b], "$a and $b")
            }
        }
    }

    @Test
    fun everyPathIsNonEmptyAndBuilds() {
        for (type in VehicleType.entries) {
            assertTrue(VehicleIcons.pathData.getValue(type).length > 50, "${type.name} path data")
            // Building parses the path data; an invalid one throws here.
            assertTrue(VehicleIcons.of(type).root.size > 0, "${type.name} has a path")
        }
    }

    @Test
    fun theGenericCarIsTheCarIcon() {
        assertSame(VehicleIcons.of(VehicleType.CAR), VehicleIcons.Car)
    }

    @Test
    fun theCarPathIsTheOneOfTheOriginalSvgFile() {
        // The generic car placeholder of the picture change was `car-fill`; it must not drift.
        assertTrue(VehicleIcons.pathData.getValue(VehicleType.CAR).startsWith("M240,104H229.2L201.42,41.5A16,16"))
    }
}
