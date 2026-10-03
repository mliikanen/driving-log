package com.mikonoma.drivinglog.vehicle.input

import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import kotlinx.serialization.Serializable

/** Steps are entered digit by digit, in base ten. */
private const val DIGIT_BASE = 10L
private const val MAX_DIGIT = 9

/**
 * A microwave-style odometer entry: digits enter at the right-hand end and shift the earlier digits left.
 *
 * [steps] is the value counted in the unit's step: whole kilometers or miles, or tenths for the tenths units.
 * So with a tenths unit the digits 1, 2, 3 give 1, 12, 123 steps, shown as 0.1, 1.2, 12.3.
 *
 * `null` means nothing has been entered yet: the field is empty and the form cannot be saved. That is different from
 * a typed 0, which is a valid reading.
 *
 * [zeroPrefix] remembers that the first digit typed was a 0 and more digits followed it: 0, 5, 3 is the text "053"
 * (steps 53, drawn 5.3 or 53), so backspace can take the 3, the 5 and then the zero away in the order they were typed.
 * The prefix is never drawn as an extra digit and never counts towards the digit limit. It only exists together with
 * a value above zero, and a second leading 0 is ignored, so there is at most one.
 *
 * Only digits can ever be entered, so an entry is never invalid.
 */
@Serializable
data class OdometerEntry(val unit: OdometerUnit, val steps: Long? = null, val zeroPrefix: Boolean = false) {
    init {
        require(steps == null || steps in 0..unit.maxSteps) { "Entry out of range: $steps" }
        require(!zeroPrefix || (steps != null && steps > 0)) { "A zero prefix needs a value above zero" }
    }

    val isEmpty: Boolean get() = steps == null

    /**
     * The text of the input field: "" when empty, "0" for a typed zero, otherwise the digits of [steps] with a "0" in
     * front when there is a zero prefix. The field draws the number, not this text.
     */
    val digits: String
        get() = when {
            steps == null -> ""
            zeroPrefix -> "0$steps"
            else -> steps.toString()
        }

    fun press(digit: Int): OdometerEntry {
        require(digit in 0..MAX_DIGIT) { "Not a digit: $digit" }
        val current = steps
        return when {
            // The first digit: a 0 is the typed zero (in a tenths unit it is the tenth).
            current == null -> copy(steps = digit.toLong())

            // Only the typed zero so far: another 0 changes nothing, any other digit turns the zero into the prefix.
            current == 0L -> if (digit == 0) this else copy(steps = digit.toLong(), zeroPrefix = true)

            else -> {
                val next = current * 10 + digit
                if (next > unit.maxSteps) this else copy(steps = next)
            }
        }
    }

    /**
     * Removes the last digit typed. The prefix zero goes last: after the digits typed behind it, back to the typed zero,
     * and one more backspace makes the entry empty.
     */
    fun backspace(): OdometerEntry {
        val current = steps ?: return this
        return when {
            current == 0L -> copy(steps = null)
            current >= DIGIT_BASE -> copy(steps = current / DIGIT_BASE)
            zeroPrefix -> copy(steps = 0, zeroPrefix = false)
            else -> copy(steps = null)
        }
    }

    fun clear(): OdometerEntry = copy(steps = null, zeroPrefix = false)

    /**
     * Applies a change of the field's text made with the system keyboard, reducing it to key presses.
     * Everything that is not a digit is dropped, whether typed or pasted.
     */
    fun applyEdit(newText: String): OdometerEntry {
        val newDigits = newText.filter { it in '0'..'9' }
        val oldDigits = digits
        return when {
            newDigits == oldDigits -> this
            newDigits.startsWith(oldDigits) -> newDigits.drop(oldDigits.length).fold(this) { entry, c -> entry.press(c - '0') }
            oldDigits.startsWith(newDigits) -> (1..oldDigits.length - newDigits.length).fold(this) { entry, _ -> entry.backspace() }
            else -> newDigits.fold(clear()) { entry, c -> entry.press(c - '0') }
        }
    }

    /**
     * Changes the unit while keeping the number as read off the dial: between a whole and a tenths unit the
     * value is rescaled (tenths to whole rounds half up), between two units of the same kind it is unchanged.
     * An empty entry stays empty. The zero prefix is dropped, because the rescaled number is no longer what was typed;
     * a typed zero stays a typed zero.
     */
    fun withUnit(newUnit: OdometerUnit): OdometerEntry {
        val current = steps ?: return OdometerEntry(newUnit)
        val rescaled = when {
            unit.hasTenths == newUnit.hasTenths -> current
            newUnit.hasTenths -> current * 10
            else -> (current + 5) / 10
        }
        return OdometerEntry(newUnit, rescaled.coerceAtMost(newUnit.maxSteps))
    }

    /** The entered distance in whole meters, or null while nothing has been entered. */
    fun toDistance(): Distance? = steps?.let { Distance(unit.stepsToMeters(it)) }
}
