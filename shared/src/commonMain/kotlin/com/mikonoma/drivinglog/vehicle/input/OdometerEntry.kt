package com.mikonoma.drivinglog.vehicle.input

import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import kotlinx.serialization.Serializable

/**
 * A microwave-style odometer entry: digits enter at the right-hand end and shift the earlier digits left.
 *
 * [steps] is the value counted in the unit's step: whole kilometers or miles, or tenths for the tenths units.
 * So with a tenths unit the digits 1, 2, 3 give 1, 12, 123 steps, shown as 0.1, 1.2, 12.3. Only digits can
 * ever be entered, so an entry is never invalid.
 */
@Serializable
data class OdometerEntry(val unit: OdometerUnit, val steps: Long = 0) {
    init {
        require(steps in 0..unit.maxSteps) { "Entry out of range: $steps" }
    }

    /** The digits of [steps] without leading zeros, "0" for zero. This is the text of the input field. */
    val digits: String get() = steps.toString()

    fun press(digit: Int): OdometerEntry {
        require(digit in 0..9) { "Not a digit: $digit" }
        val next = steps * 10 + digit
        return if (next > unit.maxSteps) this else copy(steps = next)
    }

    fun backspace(): OdometerEntry = copy(steps = steps / 10)

    fun clear(): OdometerEntry = copy(steps = 0)

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
     */
    fun withUnit(newUnit: OdometerUnit): OdometerEntry {
        val rescaled = when {
            unit.hasTenths == newUnit.hasTenths -> steps
            newUnit.hasTenths -> steps * 10
            else -> (steps + 5) / 10
        }
        return OdometerEntry(newUnit, rescaled.coerceAtMost(newUnit.maxSteps))
    }

    fun toDistance(): Distance = Distance(unit.stepsToMeters(steps))
}
