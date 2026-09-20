package com.mikonoma.drivinglog.vehicle.input

import kotlin.test.Test
import kotlin.test.assertEquals

class VehicleInputTest {

    private fun valid(name: String, plate: String) =
        (validateVehicleFields(name, plate) as VehicleFieldsResult.Valid).fields

    @Test
    fun surroundingWhitespaceIsTrimmed() {
        assertEquals(VehicleFields("Family car", "ABC-123"), valid("  Family car ", " ABC-123  "))
    }

    @Test
    fun tabsAndNewlinesAreTrimmedToo() {
        assertEquals(VehicleFields("Van", "X1"), valid("\tVan\n", "\r\nX1 "))
    }

    @Test
    fun noBreakSpaceIsTrimmed() {
        assertEquals(VehicleFields("Van", null), valid(" Van ", ""))
    }

    @Test
    fun whitespaceOnlyNameIsRejected() {
        assertEquals(VehicleFieldsResult.NameRequired, validateVehicleFields("   ", "ABC"))
    }

    @Test
    fun emptyNameIsRejected() {
        assertEquals(VehicleFieldsResult.NameRequired, validateVehicleFields("", ""))
    }

    @Test
    fun whitespaceOnlyPlateMeansNoPlate() {
        assertEquals(VehicleFields("Van", null), valid("Van", "   "))
    }

    @Test
    fun emptyPlateMeansNoPlate() {
        assertEquals(VehicleFields("Van", null), valid("Van", ""))
    }

    @Test
    fun innerWhitespaceIsKept() {
        assertEquals(VehicleFields("My  old car", "AB  12"), valid(" My  old car ", " AB  12 "))
    }

    @Test
    fun namesAreNotMadeUnique() {
        assertEquals(valid("Van", ""), valid("Van", ""))
    }
}
