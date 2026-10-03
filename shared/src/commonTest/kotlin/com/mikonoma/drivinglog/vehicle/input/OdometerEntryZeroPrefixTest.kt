package com.mikonoma.drivinglog.vehicle.input

import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.format.formatSteps
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** A first typed zero stays as an unshown prefix, so backspace undoes exactly what was typed. */
class OdometerEntryZeroPrefixTest {

    private val us = NumberSymbols.ENGLISH_US

    /** What the field draws: nothing while empty; the zero prefix is never drawn as an extra digit. */
    private fun OdometerEntry.shown() = steps?.let { formatSteps(it, unit.hasTenths, us) } ?: ""

    private fun tenths() = OdometerEntry(OdometerUnit.KILOMETERS_TENTHS)
    private fun whole() = OdometerEntry(OdometerUnit.KILOMETERS)

    private fun OdometerEntry.pressAll(vararg digits: Int) = digits.fold(this) { e, d -> e.press(d) }

    private fun OdometerEntry.shownAfterEach(vararg digits: Int): List<String> =
        digits.toList().runningFold(this) { e, d -> e.press(d) }.drop(1).map { it.shown() }

    /** The readings after each of [count] backspaces. */
    private fun OdometerEntry.backspaces(count: Int): List<String> = (1..count).runningFold(this) { e, _ -> e.backspace() }.drop(1).map { it.shown() }

    // Tenths units

    @Test
    fun aFirstZeroIsKeptWhenMoreDigitsFollowInATenthsUnit() {
        assertEquals(listOf("0.0", "0.5"), tenths().shownAfterEach(0, 5))
        assertEquals(listOf("0.0", ""), tenths().pressAll(0, 5).backspaces(2))
    }

    @Test
    fun aFirstZeroIsKeptUnderSeveralLaterDigits() {
        assertEquals(listOf("0.0", "0.5", "5.3"), tenths().shownAfterEach(0, 5, 3))
        assertEquals(listOf("0.5", "0.0", ""), tenths().pressAll(0, 5, 3).backspaces(3))
    }

    @Test
    fun theTypedZeroIsAnEnteredReadingAgainAfterBackspace() {
        val entry = tenths().pressAll(0, 5).backspace()
        assertFalse(entry.isEmpty)
        assertEquals(0L, entry.steps)
        assertEquals(0L, entry.toDistance()?.meters)
        assertFalse(entry.zeroPrefix)
    }

    @Test
    fun furtherLeadingZerosAreIgnored() {
        assertEquals(listOf("0.0", "0.0", "0.0", "0.5"), tenths().shownAfterEach(0, 0, 0, 5))
        assertEquals(listOf("0.0", ""), tenths().pressAll(0, 0, 0, 5).backspaces(2))
    }

    @Test
    fun aZeroThatIsNotTypedFirstIsAnOrdinaryDigit() {
        assertEquals(listOf("0.1", "1.0"), tenths().shownAfterEach(1, 0))
        val entry = tenths().pressAll(1, 0)
        assertFalse(entry.zeroPrefix)
        assertEquals(listOf("0.1", ""), entry.backspaces(2))
    }

    @Test
    fun zerosInTheMiddleAreOrdinaryDigitsToo() {
        assertEquals(listOf("0.0", "0.5", "5.0", "50.0"), tenths().shownAfterEach(0, 5, 0, 0))
        assertEquals(listOf("5.0", "0.5", "0.0", ""), tenths().pressAll(0, 5, 0, 0).backspaces(4))
    }

    // Whole-number units

    @Test
    fun aFirstZeroIsKeptInAWholeNumberUnit() {
        assertEquals(listOf("0", "5"), whole().shownAfterEach(0, 5))
        assertEquals(listOf("0", ""), whole().pressAll(0, 5).backspaces(2))
    }

    @Test
    fun aFirstZeroIsKeptUnderSeveralLaterDigitsInAWholeNumberUnit() {
        assertEquals(listOf("0", "5", "53"), whole().shownAfterEach(0, 5, 3))
        assertEquals(listOf("5", "0", ""), whole().pressAll(0, 5, 3).backspaces(3))
    }

    @Test
    fun aWholeNumberUnitShowsNoExtraZero() {
        assertEquals("53", whole().pressAll(0, 5, 3).shown())
        assertEquals("0", whole().pressAll(0, 0, 0).shown())
    }

    // The text of the field

    @Test
    fun theTextOfTheFieldIncludesThePrefixZero() {
        assertEquals("", tenths().digits)
        assertEquals("0", tenths().press(0).digits)
        assertEquals("05", tenths().pressAll(0, 5).digits)
        assertEquals("053", tenths().pressAll(0, 5, 3).digits)
        assertEquals("53", tenths().pressAll(5, 3).digits)
        assertEquals("10", tenths().pressAll(1, 0).digits)
    }

    @Test
    fun theDrawnNumberIsTheSameWithOrWithoutThePrefix() {
        assertEquals(tenths().pressAll(5, 3).shown(), tenths().pressAll(0, 5, 3).shown())
        assertEquals(whole().pressAll(5, 3).shown(), whole().pressAll(0, 5, 3).shown())
    }

    // Pasting

    @Test
    fun aPastedLeadingZeroIsKeptLikeATypedOne() {
        val entry = tenths().applyEdit("0123")
        assertEquals("12.3", entry.shown())
        assertTrue(entry.zeroPrefix)
        assertEquals(listOf("1.2", "0.1", "0.0", ""), entry.backspaces(4))
    }

    // applyEdit: the system keyboard reports the field's text, which includes the prefix zero

    @Test
    fun deletingTheLastDigitOfAPrefixedEntryLeavesTheTypedZero() {
        val entry = tenths().pressAll(0, 5).applyEdit("0")
        assertEquals("0.0", entry.shown())
        assertEquals(0L, entry.steps)
        assertFalse(entry.zeroPrefix)
        assertEquals("0", entry.digits)
    }

    @Test
    fun deletingEverythingFromAPrefixedEntryEmptiesIt() {
        assertTrue(tenths().pressAll(0, 5).applyEdit("").isEmpty)
        assertTrue(tenths().pressAll(0, 5, 3).applyEdit("").isEmpty)
    }

    @Test
    fun extendingTheTypedZeroKeepsItAsThePrefix() {
        val entry = tenths().press(0).applyEdit("05")
        assertEquals("0.5", entry.shown())
        assertTrue(entry.zeroPrefix)
        assertEquals("05", entry.digits)
    }

    @Test
    fun aSecondZeroTypedOnTheTypedZeroChangesNothing() {
        val entry = tenths().press(0)
        assertEquals(entry, entry.applyEdit("00"))
        assertEquals(entry, entry.applyEdit("000"))
    }

    @Test
    fun zerosTypedBeforeADigitLeaveOneZeroPrefix() {
        val entry = tenths().press(0).applyEdit("005")
        assertEquals("0.5", entry.shown())
        assertEquals("05", entry.digits)
    }

    @Test
    fun extendingAPrefixedEntryStaysPrefixed() {
        val entry = tenths().pressAll(0, 5).applyEdit("053")
        assertEquals("5.3", entry.shown())
        assertTrue(entry.zeroPrefix)
        assertEquals("053", entry.digits)
    }

    @Test
    fun pastingDigitsWithALeadingZeroOntoAnEmptyEntry() {
        val entry = whole().applyEdit("0123")
        assertEquals("123", entry.shown())
        assertEquals("0123", entry.digits)
        assertEquals(listOf("12", "1", "0", ""), entry.backspaces(4))
    }

    @Test
    fun replacingAPrefixedEntryStartsAgain() {
        val entry = tenths().pressAll(0, 5).applyEdit("9")
        assertEquals("0.9", entry.shown())
        assertFalse(entry.zeroPrefix)
    }

    @Test
    fun nonDigitsNeverChangeAPrefixedEntry() {
        val entry = tenths().pressAll(0, 5)
        for (typed in listOf(",", ".", "-", " ")) assertEquals(entry, entry.applyEdit(entry.digits + typed), typed)
    }

    // The digit limit

    @Test
    fun thePrefixZeroDoesNotCountTowardsTheLimit() {
        val full = whole().pressAll(0, 9, 9, 9, 9, 9, 9, 9)
        assertEquals("9,999,999", full.shown())
        assertEquals(full, full.press(1))
        assertEquals("09999999", full.digits)
    }

    @Test
    fun thePrefixZeroDoesNotCountTowardsTheLimitInATenthsUnit() {
        val full = tenths().pressAll(0, 9, 9, 9, 9, 9, 9, 9, 9)
        assertEquals("9,999,999.9", full.shown())
        assertEquals(full, full.press(1))
    }

    // Clear, unit change

    @Test
    fun clearRemovesThePrefixToo() {
        val cleared = tenths().pressAll(0, 5, 3).clear()
        assertTrue(cleared.isEmpty)
        assertFalse(cleared.zeroPrefix)
        assertEquals("", cleared.digits)
    }

    @Test
    fun aUnitChangeDropsThePrefix() {
        val changed = whole().pressAll(0, 5).withUnit(OdometerUnit.KILOMETERS_TENTHS)
        assertEquals("5.0", changed.shown())
        assertFalse(changed.zeroPrefix)
        assertEquals("50", changed.digits)
        assertEquals(listOf("0.5", "", ""), changed.backspaces(3))
    }

    @Test
    fun aTypedZeroStaysATypedZeroWhenTheUnitChanges() {
        val changed = whole().press(0).withUnit(OdometerUnit.KILOMETERS_TENTHS)
        assertEquals("0.0", changed.shown())
        assertFalse(changed.zeroPrefix)
        assertFalse(changed.isEmpty)
    }

    @Test
    fun aPrefixedEntryChangingBetweenUnitsOfTheSameKindDropsThePrefixToo() {
        val changed = whole().pressAll(0, 5).withUnit(OdometerUnit.MILES)
        assertEquals("5", changed.shown())
        assertFalse(changed.zeroPrefix)
    }

    // The earlier sequences are unchanged

    @Test
    fun theEarlierSequencesAreUnchanged() {
        assertEquals(listOf("0.1", "1.2", "12.3"), tenths().shownAfterEach(1, 2, 3))
        assertEquals(listOf("1.2", "0.1", ""), tenths().pressAll(1, 2, 3).backspaces(3))
        val steps = listOf<(OdometerEntry) -> OdometerEntry>(
            { it.press(1) },
            { it.press(2) },
            { it.backspace() },
            { it.backspace() },
            { it.press(2) },
            { it.press(3) },
            { it.press(0) },
        )
        val shown = steps.runningFold(tenths()) { e, step -> step(e) }.drop(1).map { it.shown() }
        assertEquals(listOf("0.1", "1.2", "0.1", "", "0.2", "2.3", "23.0"), shown)
    }

    // Invariants and serialization

    @Test
    fun aPrefixNeedsAValueAboveZero() {
        assertFailsWith<IllegalArgumentException> { OdometerEntry(OdometerUnit.KILOMETERS, steps = 0, zeroPrefix = true) }
        assertFailsWith<IllegalArgumentException> { OdometerEntry(OdometerUnit.KILOMETERS, steps = null, zeroPrefix = true) }
    }

    @Test
    fun theNewFieldIsOptionalSoAStateSavedByAnEarlierBuildStillRestores() {
        val descriptor = OdometerEntry.serializer().descriptor
        val index = descriptor.getElementIndex("zeroPrefix")
        assertTrue(descriptor.isElementOptional(index))
        assertFalse(OdometerEntry(OdometerUnit.KILOMETERS, steps = 5).zeroPrefix)
    }
}
