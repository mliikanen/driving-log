package com.mikonoma.drivinglog.vehicle.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VehicleFuelTypeTest {

    @Test
    fun everyCodeIsPinnedBecauseItIsWhatIsStored() {
        assertEquals("PETROL", VehicleFuelType.PETROL.code)
        assertEquals("DIESEL", VehicleFuelType.DIESEL.code)
        assertEquals("LPG", VehicleFuelType.LPG.code)
        assertEquals("CNG", VehicleFuelType.CNG.code)
        assertEquals("HYDROGEN", VehicleFuelType.HYDROGEN.code)
        assertEquals("OTHER", VehicleFuelType.OTHER.code)
    }

    @Test
    fun theOrderIsTheOrderOfTheChoice() {
        assertEquals(
            listOf("Petrol", "Diesel", "LPG", "CNG", "Hydrogen", "Other"),
            VehicleFuelType.entries.map { it.label },
        )
    }

    @Test
    fun thereAreSixFuelTypesWithDistinctCodesAndLabels() {
        assertEquals(6, VehicleFuelType.entries.size)
        assertEquals(6, VehicleFuelType.entries.map { it.code }.toSet().size)
        assertEquals(6, VehicleFuelType.entries.map { it.label }.toSet().size)
    }

    @Test
    fun everyTypeRoundTripsThroughItsCode() {
        for (type in VehicleFuelType.entries) assertEquals(type, VehicleFuelType.fromCode(type.code), type.name)
    }

    @Test
    fun theCodeIsTheEnumName() {
        for (type in VehicleFuelType.entries) assertEquals(type.name, type.code)
    }

    @Test
    fun anUnknownEmptyOrMissingCodeIsNoType() {
        assertNull(VehicleFuelType.fromCode("HOVERCRAFT"))
        assertNull(VehicleFuelType.fromCode(""))
        assertNull(VehicleFuelType.fromCode(null))
    }

    // ---- The refueling fuel types each one offers (vehicle-fuel-type's filtering requirement)

    @Test
    fun petrolOffersRegularPremiumAndFlexFuel() {
        assertEquals(
            setOf(FuelType.REGULAR_PETROL, FuelType.PREMIUM_PETROL, FuelType.E85, FuelType.OTHER),
            VehicleFuelType.PETROL.allowedFuelTypes(),
        )
    }

    @Test
    fun dieselOffersDieselPremiumDieselAndBiodiesel() {
        assertEquals(
            setOf(FuelType.DIESEL, FuelType.PREMIUM_DIESEL, FuelType.BIODIESEL, FuelType.OTHER),
            VehicleFuelType.DIESEL.allowedFuelTypes(),
        )
    }

    @Test
    fun lpgOffersOnlyLpg() {
        assertEquals(setOf(FuelType.LPG, FuelType.OTHER), VehicleFuelType.LPG.allowedFuelTypes())
    }

    @Test
    fun cngOffersOnlyCng() {
        assertEquals(setOf(FuelType.CNG, FuelType.OTHER), VehicleFuelType.CNG.allowedFuelTypes())
    }

    @Test
    fun hydrogenOffersOnlyHydrogen() {
        assertEquals(setOf(FuelType.HYDROGEN, FuelType.OTHER), VehicleFuelType.HYDROGEN.allowedFuelTypes())
    }

    @Test
    fun otherOffersEveryFuelTypeUnfiltered() {
        assertEquals(FuelType.entries.toSet(), VehicleFuelType.OTHER.allowedFuelTypes())
    }

    @Test
    fun everyGroupOffersOther() {
        for (type in VehicleFuelType.entries) assertTrue(FuelType.OTHER in type.allowedFuelTypes(), type.name)
    }

    @Test
    fun theFiveNonOtherGroupsPartitionTheRemainingNineFuelTypesWithNoOverlap() {
        val groups = VehicleFuelType.entries.filter { it != VehicleFuelType.OTHER }.map { it.allowedFuelTypes() - FuelType.OTHER }
        val union = groups.reduce { a, b -> a + b }
        assertEquals(FuelType.entries.toSet() - FuelType.OTHER, union)
        assertEquals(union.size, groups.sumOf { it.size }) // no fuel type appears in two groups
    }
}
