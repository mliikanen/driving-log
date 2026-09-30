package com.mikonoma.drivinglog.vehicle.domain

import kotlinx.serialization.Serializable

/**
 * A refueling's fuel type, from a fixed set covering common US and European fuels (`add-refueling-logging`). AdBlue
 * (diesel exhaust fluid) is deliberately excluded — it is not a propulsion fuel (see `add-adblue-tracking`). The
 * [code] is what is stored: it never changes once released and does not depend on the language or the device. The
 * [label] is the English name shown for now, like the rest of the app. The order here is the order of the choice.
 */
@Serializable
enum class FuelType(val code: String, val label: String) {
    REGULAR_PETROL("REGULAR_PETROL", "Regular petrol/gasoline"),
    PREMIUM_PETROL("PREMIUM_PETROL", "Premium petrol/gasoline"),
    DIESEL("DIESEL", "Diesel"),
    PREMIUM_DIESEL("PREMIUM_DIESEL", "Premium diesel"),
    BIODIESEL("BIODIESEL", "Biodiesel"),
    E85("E85", "E85/flex fuel"),
    LPG("LPG", "LPG"),
    CNG("CNG", "CNG"),
    HYDROGEN("HYDROGEN", "Hydrogen"),
    OTHER("OTHER", "Other"),
    ;

    companion object {
        /** The type of a stored [code], or null for none: a code from a newer version of the app, an empty one or a null is "no type", never an error. */
        fun fromCode(code: String?): FuelType? = entries.firstOrNull { it.code == code }
    }
}
