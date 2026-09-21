package com.mikonoma.drivinglog.vehicle.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VehicleTypeTest {

    @Test
    fun everyCodeIsPinnedBecauseItIsWhatIsStored() {
        assertEquals("CAR", VehicleType.CAR.code)
        assertEquals("SUV", VehicleType.SUV.code)
        assertEquals("VAN", VehicleType.VAN.code)
        assertEquals("TRUCK", VehicleType.TRUCK.code)
        assertEquals("BUS", VehicleType.BUS.code)
        assertEquals("MOTORCYCLE", VehicleType.MOTORCYCLE.code)
        assertEquals("SCOOTER", VehicleType.SCOOTER.code)
        assertEquals("OTHER", VehicleType.OTHER.code)
    }

    @Test
    fun everyLabelIsPinned() {
        assertEquals("Car", VehicleType.CAR.label)
        assertEquals("SUV", VehicleType.SUV.label)
        assertEquals("Van", VehicleType.VAN.label)
        assertEquals("Truck", VehicleType.TRUCK.label)
        assertEquals("Bus", VehicleType.BUS.label)
        assertEquals("Motorcycle", VehicleType.MOTORCYCLE.label)
        assertEquals("Scooter", VehicleType.SCOOTER.label)
        assertEquals("Other", VehicleType.OTHER.label)
    }

    @Test
    fun theOrderIsTheOrderOfTheChoice() {
        assertEquals(
            listOf("Car", "SUV", "Van", "Truck", "Bus", "Motorcycle", "Scooter", "Other"),
            VehicleType.entries.map { it.label },
        )
    }

    @Test
    fun thereAreEightTypesWithDistinctCodesAndLabels() {
        assertEquals(8, VehicleType.entries.size)
        assertEquals(8, VehicleType.entries.map { it.code }.toSet().size)
        assertEquals(8, VehicleType.entries.map { it.label }.toSet().size)
    }

    @Test
    fun everyTypeRoundTripsThroughItsCode() {
        for (type in VehicleType.entries) assertEquals(type, VehicleType.fromCode(type.code), type.name)
    }

    @Test
    fun theCodeIsTheEnumName() {
        // Nothing else depends on it today, but a rename of an entry would silently change what is stored.
        for (type in VehicleType.entries) assertEquals(type.name, type.code)
    }

    @Test
    fun anUnknownCodeIsNoType() {
        assertNull(VehicleType.fromCode("PICKUP"))
        assertNull(VehicleType.fromCode("HOVERCRAFT"))
    }

    @Test
    fun anEmptyOrMissingCodeIsNoType() {
        assertNull(VehicleType.fromCode(""))
        assertNull(VehicleType.fromCode(null))
    }

    @Test
    fun aCodeMustMatchExactly() {
        assertNull(VehicleType.fromCode("van"))
        assertNull(VehicleType.fromCode("Van"))
        assertNull(VehicleType.fromCode(" VAN"))
        assertNull(VehicleType.fromCode("VAN "))
    }
}
