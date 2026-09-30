package com.mikonoma.drivinglog.vehicle.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FuelTypeTest {

    @Test
    fun codesRoundTrip() {
        for (type in FuelType.entries) assertEquals(type, FuelType.fromCode(type.code))
    }

    @Test
    fun unknownOrNullCodeIsNoType() {
        assertNull(FuelType.fromCode("SOMETHING_ELSE"))
        assertNull(FuelType.fromCode(null))
        assertNull(FuelType.fromCode(""))
    }

    @Test
    fun adBlueIsNotInTheList() {
        // Deliberately excluded (add-refueling-logging): not a propulsion fuel. See add-adblue-tracking.
        for (type in FuelType.entries) assertEquals(true, !type.label.contains("AdBlue", ignoreCase = true))
    }
}
