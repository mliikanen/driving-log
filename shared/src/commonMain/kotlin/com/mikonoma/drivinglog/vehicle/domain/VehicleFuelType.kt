package com.mikonoma.drivinglog.vehicle.domain

import kotlinx.serialization.Serializable

/**
 * A vehicle's own coarse fuel/engine category, from a small fixed set (`vehicle-fuel-type`) — distinct from and
 * coarser than [FuelType], the precise choice made each time a refueling is logged. The [code] is what is stored:
 * it never changes once released and does not depend on the language or the device. The [label] is the English
 * name shown for now, like the rest of the app. The order here is the order of the choice.
 */
@Serializable
enum class VehicleFuelType(val code: String, val label: String) {
    PETROL("PETROL", "Petrol"),
    DIESEL("DIESEL", "Diesel"),
    LPG("LPG", "LPG"),
    CNG("CNG", "CNG"),
    HYDROGEN("HYDROGEN", "Hydrogen"),
    OTHER("OTHER", "Other"),
    ;

    companion object {
        /** The type of a stored [code], or null for none: a code from a newer version of the app, an empty one or a null is "no type", never an error. */
        fun fromCode(code: String?): VehicleFuelType? = entries.firstOrNull { it.code == code }
    }
}

/**
 * The refueling fuel types `refueling-logging` offers for a vehicle of this fuel type (`vehicle-fuel-type`'s "The
 * fuel type filters which refueling fuel types are offered"): a coarse engine category narrows the precise,
 * per-fill-up choice. [VehicleFuelType.OTHER] offers every one, unfiltered — the escape hatch for a fuel this set
 * doesn't name, or a genuinely mixed-fuel vehicle. A static, hand-written mapping (design.md): a new [FuelType]
 * entry needs a deliberate decision about which group(s) it joins, rather than appearing nowhere or everywhere.
 */
fun VehicleFuelType.allowedFuelTypes(): Set<FuelType> = when (this) {
    VehicleFuelType.PETROL -> setOf(FuelType.REGULAR_PETROL, FuelType.PREMIUM_PETROL, FuelType.E85, FuelType.OTHER)
    VehicleFuelType.DIESEL -> setOf(FuelType.DIESEL, FuelType.PREMIUM_DIESEL, FuelType.BIODIESEL, FuelType.OTHER)
    VehicleFuelType.LPG -> setOf(FuelType.LPG, FuelType.OTHER)
    VehicleFuelType.CNG -> setOf(FuelType.CNG, FuelType.OTHER)
    VehicleFuelType.HYDROGEN -> setOf(FuelType.HYDROGEN, FuelType.OTHER)
    VehicleFuelType.OTHER -> FuelType.entries.toSet()
}
