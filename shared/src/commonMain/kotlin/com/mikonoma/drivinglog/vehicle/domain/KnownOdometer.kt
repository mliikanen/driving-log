package com.mikonoma.drivinglog.vehicle.domain

import kotlin.time.Instant

/**
 * The odometer known at [at]: the reading of the latest odometer reading (the initial odometer or a later one) at or before [at], plus the distance of every
 * distance entry after it up to and including [at]. Null when no odometer reading is at or before [at]. A refueling takes no part: a
 * refueling logged with mileage counts only through its entry's own odometer reading or distance entry (`atomic-log-events`).
 *
 * [eventsOldestFirst] must be in log order ([inLogOrder]): by instant, then by entry in the order the entries were added, then by kind
 * within an entry. Entries at the same instant therefore count in the order they were added, and an entry dated before the latest
 * odometer reading never counts, because that reading replaces the running total.
 */
fun knownOdometerAt(eventsOldestFirst: List<VehicleEvent>, at: Instant): Distance? {
    var meters: Long? = null
    for (event in eventsOldestFirst) {
        if (event.occurredAt.instant > at) break
        when (event) {
            is VehicleEvent.OdometerReading -> meters = event.reading.meters
            is VehicleEvent.DistanceEntry -> meters = meters?.plus(event.distance.meters)
            is VehicleEvent.Refueling -> Unit
        }
    }
    return meters?.let { Distance(it) }
}

/** The current odometer: the known odometer at the end of the log. */
fun currentOdometer(eventsOldestFirst: List<VehicleEvent>): Distance? = knownOdometerAt(eventsOldestFirst, Instant.DISTANT_FUTURE)
