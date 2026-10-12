package com.mikonoma.drivinglog.vehicle.ui

import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent

val OdometerUnit.label: String
    get() = when (this) {
        OdometerUnit.KILOMETERS -> "Kilometers"
        OdometerUnit.KILOMETERS_TENTHS -> "Kilometers with 100 m"
        OdometerUnit.MILES -> "Miles"
        OdometerUnit.MILES_TENTHS -> "Miles with tenths"
    }

val VehicleEvent.label: String
    get() = when (this) {
        // One kind of event (atomic-log-events): the initial odometer is the reading saved when the vehicle was added.
        is VehicleEvent.OdometerReading -> if (isInitial) "Initial odometer" else "Odometer reading"

        is VehicleEvent.DistanceEntry -> "Distance"

        is VehicleEvent.Refueling -> "Refueling"
    }
