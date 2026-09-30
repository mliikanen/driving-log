package com.mikonoma.drivinglog.vehicle.format

import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.vehicle.domain.FuelUnit
import com.mikonoma.drivinglog.vehicle.domain.Volume

/** Renders an amount counted in hundredths (fuel entries are always two decimal places), using the locale's
 * separators. Built from integers, so nothing is rounded twice. Shared by the live entry field and [formatFuelAmount]. */
fun formatFuelSteps(steps: Long, symbols: NumberSymbols): String {
    val whole = steps / 100
    val hundredths = (steps % 100).toString().padStart(2, '0')
    return group(whole, symbols.groupingSeparator) + symbols.decimalSeparator + hundredths
}

/** A stored fuel amount in [unit], always to two decimal places, with the unit abbreviation (`add-refueling-logging`). */
fun formatFuelAmount(volume: Volume, unit: FuelUnit, symbols: NumberSymbols): String =
    formatFuelSteps(unit.millilitersToSteps(volume.milliliters), symbols) + " " + unit.abbreviation
