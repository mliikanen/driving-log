package com.mikonoma.drivinglog.vehicle.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class VehicleOrderTest {

    private fun vehicle(id: String, name: String, createdAtMillis: Long) = Vehicle(
        id = id,
        name = name,
        licensePlate = null,
        odometerUnit = OdometerUnit.KILOMETERS,
        createdAt = Instant.fromEpochMilliseconds(createdAtMillis),
        type = VehicleType.CAR,
        color = VehicleColors.default,
    )

    @Test
    fun ordersByNameWithoutRegardToCase() {
        val vehicles = listOf(vehicle("1", "van", 1), vehicle("2", "Bike", 2), vehicle("3", "Family car", 3))

        assertEquals(listOf("Bike", "Family car", "van"), vehicles.sortedWith(VehicleNameOrder).map { it.name })
    }

    @Test
    fun lowercaseNamesAreNotSortedAfterUppercaseOnes() {
        val vehicles = listOf(vehicle("1", "zebra", 1), vehicle("2", "Yak", 2), vehicle("3", "apple", 3))

        assertEquals(listOf("apple", "Yak", "zebra"), vehicles.sortedWith(VehicleNameOrder).map { it.name })
    }

    @Test
    fun namesThatDifferOnlyInCaseKeepTheOrderTheyWereAdded() {
        val vehicles = listOf(vehicle("1", "Van", 20), vehicle("2", "van", 10), vehicle("3", "VAN", 30))

        assertEquals(listOf("2", "1", "3"), vehicles.sortedWith(VehicleNameOrder).map { it.id })
    }

    @Test
    fun nonAsciiLettersAreFoldedToo() {
        val vehicles = listOf(vehicle("1", "Äiti", 2), vehicle("2", "äiti", 1))

        assertEquals(listOf("2", "1"), vehicles.sortedWith(VehicleNameOrder).map { it.id })
    }
}
