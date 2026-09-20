package com.mikonoma.drivinglog.vehicle.input

import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.format.formatSteps
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OdometerEntryTest {

    private val us = NumberSymbols.ENGLISH_US

    /** What the field shows: nothing at all while empty, never 0 or 0.0. */
    private fun OdometerEntry.shown() = steps?.let { formatSteps(it, unit.hasTenths, us) } ?: ""

    private fun tenths() = OdometerEntry(OdometerUnit.KILOMETERS_TENTHS)
    private fun whole() = OdometerEntry(OdometerUnit.KILOMETERS)

    private fun OdometerEntry.pressAll(vararg digits: Int) = digits.fold(this) { e, d -> e.press(d) }

    // The field starts empty.

    @Test
    fun startsEmpty() {
        assertTrue(tenths().isEmpty)
        assertTrue(whole().isEmpty)
        assertEquals("", tenths().shown())
        assertEquals("", whole().shown())
        assertEquals("", tenths().digits)
        assertNull(tenths().toDistance())
    }

    @Test
    fun everyUnitStartsEmpty() {
        for (unit in OdometerUnit.entries) {
            val entry = OdometerEntry(unit)
            assertTrue(entry.isEmpty, unit.name)
            assertEquals("", entry.shown(), unit.name)
        }
    }

    @Test
    fun aTypedZeroIsNotEmptyAndIsAValidReading() {
        val tenthsZero = tenths().press(0)
        assertFalse(tenthsZero.isEmpty)
        assertEquals("0.0", tenthsZero.shown())
        assertEquals("0", whole().press(0).shown())
        assertEquals(0, tenthsZero.toDistance()?.meters)
    }

    @Test
    fun backspaceOnATypedZeroEmptiesIt() {
        assertEquals("", tenths().press(0).backspace().shown())
        assertTrue(whole().press(0).backspace().isEmpty)
    }

    @Test
    fun wholeUnitsFillFromTheRight() {
        val shown = listOf(1, 2, 3).runningFold(whole()) { e, d -> e.press(d) }.drop(1).map { it.shown() }
        assertEquals(listOf("1", "12", "123"), shown)
    }

    @Test
    fun tenthsUnitsFillFromTheRight() {
        val shown = listOf(1, 2, 3).runningFold(tenths()) { e, d -> e.press(d) }.drop(1).map { it.shown() }
        assertEquals(listOf("0.1", "1.2", "12.3"), shown)
    }

    @Test
    fun tenthsOfMilesFillFromTheRightToo() {
        val shown = listOf(1, 2, 3).runningFold(OdometerEntry(OdometerUnit.MILES_TENTHS)) { e, d -> e.press(d) }
            .drop(1).map { it.shown() }
        assertEquals(listOf("0.1", "1.2", "12.3"), shown)
    }

    @Test
    fun backspaceRestoresTheEarlierValue() {
        val entry = tenths().pressAll(1, 2)
        assertEquals("1.2", entry.shown())
        val plus3 = entry.press(3)
        assertEquals("12.3", plus3.shown())
        assertEquals("1.2", plus3.backspace().shown())
    }

    @Test
    fun backspaceRemovesDigitsOneAtATimeDownToEmpty() {
        var entry = tenths().pressAll(1, 2, 3)
        val shown = buildList {
            repeat(3) {
                entry = entry.backspace()
                add(entry.shown())
            }
        }
        assertEquals(listOf("1.2", "0.1", ""), shown)
    }

    @Test
    fun backspaceOnAnEmptyEntryStaysEmpty() {
        assertEquals("", tenths().backspace().shown())
        assertEquals("", whole().backspace().backspace().shown())
        assertTrue(tenths().backspace().isEmpty)
    }

    @Test
    fun backspaceOnAWholeUnit() {
        assertEquals("12", whole().pressAll(1, 2, 3).backspace().shown())
        assertEquals("", whole().press(1).backspace().shown())
    }

    @Test
    fun typeDeleteToEmptyAndTypeAgainForTenths() {
        val steps = listOf<(OdometerEntry) -> OdometerEntry>(
            { it.press(1) }, { it.press(2) }, { it.backspace() }, { it.backspace() },
            { it.press(2) }, { it.press(3) }, { it.press(0) },
        )
        val shown = steps.runningFold(tenths()) { e, step -> step(e) }.drop(1).map { it.shown() }
        assertEquals(listOf("0.1", "1.2", "0.1", "", "0.2", "2.3", "23.0"), shown)
    }

    @Test
    fun typeDeleteToEmptyAndTypeAgainForWholeUnits() {
        val steps = listOf<(OdometerEntry) -> OdometerEntry>(
            { it.press(1) }, { it.press(2) }, { it.backspace() }, { it.backspace() },
            { it.press(2) }, { it.press(3) }, { it.press(0) },
        )
        val shown = steps.runningFold(whole()) { e, step -> step(e) }.drop(1).map { it.shown() }
        assertEquals(listOf("1", "12", "1", "", "2", "23", "230"), shown)
    }

    @Test
    fun clearEmptiesTheEntry() {
        val cleared = tenths().pressAll(1, 2, 3).clear()
        assertEquals("", cleared.shown())
        assertTrue(cleared.isEmpty)
        assertNull(cleared.toDistance())
    }

    @Test
    fun zeroAtZeroStaysAtZero() {
        assertEquals("0", whole().pressAll(0, 0, 0).shown())
        assertEquals("0.0", tenths().pressAll(0, 0, 0).shown())
    }

    @Test
    fun leadingZerosDoNotAccumulate() {
        assertEquals("5", whole().pressAll(0, 0, 5).shown())
    }

    @Test
    fun theCapIgnoresFurtherDigitsForWholeUnits() {
        val full = whole().pressAll(9, 9, 9, 9, 9, 9, 9)
        assertEquals("9,999,999", full.shown())
        assertEquals(full, full.press(1))
    }

    @Test
    fun theCapIgnoresFurtherDigitsForTenthsUnits() {
        val full = tenths().pressAll(9, 9, 9, 9, 9, 9, 9, 9)
        assertEquals("9,999,999.9", full.shown())
        assertEquals(full, full.press(1))
    }

    // applyEdit: the system keyboard reports text, not key presses.

    @Test
    fun editTypingIntoTheEmptyFieldEntersTheDigit() {
        assertEquals("0.1", tenths().applyEdit("1").shown())
        assertEquals("5", whole().applyEdit("5").shown())
    }

    @Test
    fun editTypingAZeroIntoTheEmptyFieldIsATypedZero() {
        val entry = tenths().applyEdit("0")
        assertFalse(entry.isEmpty)
        assertEquals("0.0", entry.shown())
    }

    @Test
    fun editDeletingTheOnlyDigitEmptiesTheField() {
        val entry = tenths().press(5).applyEdit("")
        assertTrue(entry.isEmpty)
        assertEquals("", entry.shown())
    }

    @Test
    fun editDeletingTheTypedZeroEmptiesTheField() {
        assertTrue(tenths().press(0).applyEdit("").isEmpty)
    }

    @Test
    fun editAppendingADigitPressesIt() {
        val entry = tenths().pressAll(1, 2)
        assertEquals("12.3", entry.applyEdit(entry.digits + "3").shown())
    }

    @Test
    fun editDeletingADigitIsABackspace() {
        val entry = tenths().pressAll(1, 2, 3)
        assertEquals("1.2", entry.applyEdit("12").shown())
    }

    @Test
    fun editTypingOnAZeroReplacesTheZero() {
        assertEquals("0.1", tenths().press(0).applyEdit("01").shown())
        assertEquals("0.0", tenths().press(0).applyEdit("00").shown())
    }

    @Test
    fun pastedDigitsAreAllEntered() {
        assertEquals("12.3", tenths().applyEdit("123").shown())
        assertEquals("12.3", tenths().press(0).applyEdit("0123").shown())
    }

    @Test
    fun nonDigitsAreIgnored() {
        val entry = tenths().pressAll(1, 2, 3)
        for (typed in listOf(",", ".", "-", " ", "a")) {
            assertEquals("12.3", entry.applyEdit(entry.digits + typed).shown(), typed)
        }
    }

    @Test
    fun nonDigitsIntoTheEmptyFieldLeaveItEmpty() {
        for (typed in listOf(",", ".", "-", " ", "a")) {
            assertTrue(tenths().applyEdit(typed).isEmpty, typed)
        }
    }

    @Test
    fun editReplacingEverythingStartsAgain() {
        assertEquals("4.5", tenths().pressAll(1, 2, 3).applyEdit("45").shown())
    }

    @Test
    fun editPastTheCapIsIgnored() {
        val full = whole().pressAll(9, 9, 9, 9, 9, 9, 9)
        assertEquals(full, full.applyEdit(full.digits + "1"))
    }

    @Test
    fun unchangedTextChangesNothing() {
        val entry = tenths().pressAll(1, 2)
        assertEquals(entry, entry.applyEdit(entry.digits))
        assertTrue(tenths().applyEdit("").isEmpty)
    }

    // Unit changes.

    @Test
    fun wholeToTenthsKeepsTheNumber() {
        assertEquals("123.0", whole().pressAll(1, 2, 3).withUnit(OdometerUnit.KILOMETERS_TENTHS).shown())
    }

    @Test
    fun tenthsBackToWholeKeepsTheNumber() {
        val entry = whole().pressAll(1, 2, 3).withUnit(OdometerUnit.KILOMETERS_TENTHS).withUnit(OdometerUnit.KILOMETERS)
        assertEquals("123", entry.shown())
    }

    @Test
    fun tenthsToWholeRoundsHalfUp() {
        assertEquals("13", tenths().pressAll(1, 2, 6).withUnit(OdometerUnit.KILOMETERS).shown())
        assertEquals("12", tenths().pressAll(1, 2, 3).withUnit(OdometerUnit.KILOMETERS).shown())
        assertEquals("13", tenths().pressAll(1, 2, 5).withUnit(OdometerUnit.KILOMETERS).shown())
    }

    @Test
    fun kilometersToMilesKeepsTheNumber() {
        val entry = whole().pressAll(1, 2, 3).withUnit(OdometerUnit.MILES)
        assertEquals(OdometerUnit.MILES, entry.unit)
        assertEquals("123", entry.shown())
    }

    @Test
    fun roundingAtTheCapStaysWithinTheCap() {
        val full = tenths().pressAll(9, 9, 9, 9, 9, 9, 9, 9)
        assertEquals("9,999,999", full.withUnit(OdometerUnit.KILOMETERS).shown())
    }

    @Test
    fun anEmptyEntryStaysEmptyWhateverTheUnitChange() {
        for (from in OdometerUnit.entries) for (to in OdometerUnit.entries) {
            val changed = OdometerEntry(from).withUnit(to)
            assertTrue(changed.isEmpty, "$from -> $to")
            assertEquals(to, changed.unit)
        }
    }

    @Test
    fun aTypedZeroStaysATypedZeroWhenTheUnitChanges() {
        val changed = whole().press(0).withUnit(OdometerUnit.KILOMETERS_TENTHS)
        assertFalse(changed.isEmpty)
        assertEquals("0.0", changed.shown())
    }

    @Test
    fun toDistanceUsesTheUnit() {
        assertEquals(45_200_300, tenths().pressAll(4, 5, 2, 0, 0, 3).toDistance()?.meters)
    }
}
