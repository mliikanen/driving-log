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
        is VehicleEvent.InitialOdometer -> "Initial odometer"
        is VehicleEvent.OdometerAnchor -> "Odometer reading"
        is VehicleEvent.DistanceEntry -> "Distance"
        is VehicleEvent.Refueling -> "Refueling"
    }
