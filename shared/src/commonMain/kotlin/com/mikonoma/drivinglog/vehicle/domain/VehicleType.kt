package com.mikonoma.drivinglog.vehicle.domain

import kotlinx.serialization.Serializable

/**
 * The kind of a vehicle, from a small fixed set. The [code] is what is stored: it never changes once released and does not depend on the
 * language or the device. The [label] is the English name shown for now, like the rest of the app. The order here is the order of the choice.
 */
@Serializable
enum class VehicleType(val code: String, val label: String) {
    CAR("CAR", "Car"),
    SUV("SUV", "SUV"),
    VAN("VAN", "Van"),
    TRUCK("TRUCK", "Truck"),
    BUS("BUS", "Bus"),
    MOTORCYCLE("MOTORCYCLE", "Motorcycle"),
    SCOOTER("SCOOTER", "Scooter"),
    OTHER("OTHER", "Other"),
    ;

    companion object {
        /** The type of a stored [code], or null for none: a code from a newer version of the app, an empty one or a null is "no type", never an error. */
        fun fromCode(code: String?): VehicleType? = entries.firstOrNull { it.code == code }
    }
}
