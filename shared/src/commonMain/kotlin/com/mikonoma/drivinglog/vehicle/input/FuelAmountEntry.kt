package com.mikonoma.drivinglog.vehicle.input

import com.mikonoma.drivinglog.vehicle.domain.FuelUnit
import com.mikonoma.drivinglog.vehicle.domain.Volume
import kotlinx.serialization.Serializable

/**
 * A microwave-style fuel amount entry, mirroring [OdometerEntry]'s digit-entry mechanics but always at two decimal
 * places (hundredths of whichever unit interprets [steps]). Unlike a distance/odometer entry, a fuel amount's
 * precision never depends on which unit (liters or gallons) is chosen (`add-refueling-logging`'s "The fuel amount
 * field is its own entry widget, not a reuse of OdometerEntry" design decision), so this type carries no unit of its
 * own — switching units elsewhere leaves [steps] untouched, matching "Changing the unit keeps the digits."
 *
 * `null` means nothing has been entered yet: the field is empty. That is different from a typed 0, a valid amount
 * this entry accepts, though `refueling-logging`'s own "must be above zero" rule refuses saving it.
 *
 * [zeroPrefix] remembers that the first digit typed was a 0 and more digits followed it, the same as
 * [OdometerEntry.zeroPrefix]. Only digits can ever be entered, so an entry is never invalid.
 */
@Serializable
data class FuelAmountEntry(val steps: Long? = null, val zeroPrefix: Boolean = false) {
    init {
        require(steps == null || steps in 0..MAX_STEPS) { "Entry out of range: $steps" }
        require(!zeroPrefix || (steps != null && steps > 0)) { "A zero prefix needs a value above zero" }
    }

    val isEmpty: Boolean get() = steps == null

    /** The text of the input field: "" when empty, "0" for a typed zero, otherwise the digits of [steps] with a "0" in front when there is a zero prefix. */
    val digits: String
        get() = when {
            steps == null -> ""
            zeroPrefix -> "0$steps"
            else -> steps.toString()
        }

    fun press(digit: Int): FuelAmountEntry {
        require(digit in 0..9) { "Not a digit: $digit" }
        val current = steps
        return when {
            current == null -> copy(steps = digit.toLong())
            current == 0L -> if (digit == 0) this else copy(steps = digit.toLong(), zeroPrefix = true)
            else -> {
                val next = current * 10 + digit
                if (next > MAX_STEPS) this else copy(steps = next)
            }
        }
    }

    fun backspace(): FuelAmountEntry {
        val current = steps ?: return this
        return when {
            current == 0L -> copy(steps = null)
            current >= 10 -> copy(steps = current / 10)
            zeroPrefix -> copy(steps = 0, zeroPrefix = false)
            else -> copy(steps = null)
        }
    }

    fun clear(): FuelAmountEntry = copy(steps = null, zeroPrefix = false)

    /** Applies a change of the field's text made with the system keyboard, reducing it to key presses. */
    fun applyEdit(newText: String): FuelAmountEntry {
        val newDigits = newText.filter { it in '0'..'9' }
        val oldDigits = digits
        return when {
            newDigits == oldDigits -> this
            newDigits.startsWith(oldDigits) -> newDigits.drop(oldDigits.length).fold(this) { entry, c -> entry.press(c - '0') }
            oldDigits.startsWith(newDigits) -> (1..oldDigits.length - newDigits.length).fold(this) { entry, _ -> entry.backspace() }
            else -> newDigits.fold(clear()) { entry, c -> entry.press(c - '0') }
        }
    }

    /** The entered amount in [unit], or null while nothing has been entered. */
    fun toVolume(unit: FuelUnit): Volume? = steps?.let { Volume(unit.stepsToMilliliters(it)) }

    companion object {
        /** 6 digits: up to 9,999.99 of the unit chosen, comfortably above any real fuel tank. */
        const val MAX_STEPS = 999_999L
    }
}
