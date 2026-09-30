package com.mikonoma.drivinglog.vehicle.input

import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.vehicle.domain.FuelUnit
import com.mikonoma.drivinglog.vehicle.format.formatFuelSteps
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FuelAmountEntryTest {

    private val us = NumberSymbols.ENGLISH_US

    private fun FuelAmountEntry.shown() = steps?.let { formatFuelSteps(it, us) } ?: ""

    private fun entry() = FuelAmountEntry()

    private fun FuelAmountEntry.pressAll(vararg digits: Int) = digits.fold(this) { e, d -> e.press(d) }

    @Test
    fun startsEmpty() {
        assertTrue(entry().isEmpty)
        assertEquals("", entry().shown())
        assertEquals("", entry().digits)
        assertNull(entry().toVolume(FuelUnit.LITERS))
    }

    @Test
    fun aTypedZeroIsNotEmptyAndIsAValidAmount() {
        val zero = entry().press(0)
        assertFalse(zero.isEmpty)
        assertEquals("0.00", zero.shown())
        assertEquals(0, zero.toVolume(FuelUnit.LITERS)?.milliliters)
    }

    @Test
    fun alwaysFillsAsTwoDecimals() {
        val shown = listOf(4, 2, 3).runningFold(entry()) { e, d -> e.press(d) }.drop(1).map { it.shown() }
        assertEquals(listOf("0.04", "0.42", "4.23"), shown)
    }

    @Test
    fun backspaceRemovesDigitsOneAtATimeDownToEmpty() {
        var e = entry().pressAll(4, 2, 3)
        val shown = buildList {
            repeat(3) {
                e = e.backspace()
                add(e.shown())
            }
        }
        assertEquals(listOf("0.42", "0.04", ""), shown)
    }

    @Test
    fun clearEmptiesTheEntry() {
        val cleared = entry().pressAll(4, 2, 3).clear()
        assertTrue(cleared.isEmpty)
        assertNull(cleared.toVolume(FuelUnit.LITERS))
    }

    @Test
    fun theCapIgnoresFurtherDigits() {
        val full = entry().pressAll(9, 9, 9, 9, 9, 9)
        assertEquals("9,999.99", full.shown())
        assertEquals(full, full.press(1))
    }

    @Test
    fun editTypingIntoTheEmptyFieldEntersTheDigit() {
        assertEquals("0.04", entry().applyEdit("4").shown())
    }

    @Test
    fun editDeletingTheOnlyDigitEmptiesTheField() {
        assertTrue(entry().press(5).applyEdit("").isEmpty)
    }

    @Test
    fun editAppendingADigitPressesIt() {
        val e = entry().pressAll(4, 2)
        assertEquals("4.23", e.applyEdit(e.digits + "3").shown())
    }

    @Test
    fun nonDigitsAreIgnored() {
        val e = entry().pressAll(4, 2, 3)
        for (typed in listOf(",", ".", "-", " ", "a")) {
            assertEquals("4.23", e.applyEdit(e.digits + typed).shown(), typed)
        }
    }

    @Test
    fun unitDoesNotAffectTheDigits() {
        // "Changing the unit keeps the digits typed" (add-refueling-logging): the entry itself carries no unit,
        // so a caller switching FuelUnit elsewhere never has to touch this entry at all.
        val e = entry().pressAll(4, 2, 3)
        assertEquals(423L, e.toVolume(FuelUnit.LITERS)?.milliliters?.let { it / 10 })
        assertEquals(e.digits, e.digits)
    }

    @Test
    fun toVolumeUsesTheGivenUnit() {
        val e = entry().pressAll(4, 2, 3) // 4.23
        assertEquals(4_230, e.toVolume(FuelUnit.LITERS)?.milliliters)
        assertEquals(FuelUnit.GALLONS.stepsToMilliliters(423), e.toVolume(FuelUnit.GALLONS)?.milliliters)
    }
}
