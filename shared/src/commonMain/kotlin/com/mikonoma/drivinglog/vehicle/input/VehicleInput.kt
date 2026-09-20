package com.mikonoma.drivinglog.vehicle.input

/** Name and plate after trimming and validation. */
data class VehicleFields(val name: String, val licensePlate: String?)

sealed interface VehicleFieldsResult {
    data class Valid(val fields: VehicleFields) : VehicleFieldsResult
    data object NameRequired : VehicleFieldsResult
}

/**
 * Removes leading and trailing whitespace (inner whitespace is kept). A name that is empty afterwards is
 * rejected; a plate that is empty afterwards means the vehicle has no plate.
 */
fun validateVehicleFields(rawName: String, rawLicensePlate: String): VehicleFieldsResult {
    val name = rawName.trim()
    if (name.isEmpty()) return VehicleFieldsResult.NameRequired
    val plate = rawLicensePlate.trim().ifEmpty { null }
    return VehicleFieldsResult.Valid(VehicleFields(name, plate))
}
