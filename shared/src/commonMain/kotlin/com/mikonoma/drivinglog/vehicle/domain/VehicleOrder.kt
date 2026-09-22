package com.mikonoma.drivinglog.vehicle.domain

/**
 * The order vehicles are shown in wherever more than one is listed (the vehicle list, the log form's vehicle selector): by name,
 * without regard to letter case (SQLite's NOCASE only folds ASCII, so this is done in Kotlin), then by when the vehicle was added.
 * One comparator, so the list and the selector cannot drift apart.
 */
val VehicleNameOrder: Comparator<Vehicle> = compareBy({ it.name.lowercase() }, { it.createdAt })
