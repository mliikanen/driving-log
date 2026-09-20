package com.mikonoma.drivinglog.vehicle.domain

import kotlin.time.Instant

/**
 * The odometer known at [at]: the reading of the latest odometer-setting event (the initial odometer or an odometer anchor) at or before [at], plus the distance of every
 * distance entry after it up to and including [at]. Null when no odometer-setting event is at or before [at].
 *
 * [eventsOldestFirst] must be ordered by instant and, for the same instant, by when the events were added. Events at the same
 * instant therefore count in the order they were added, and an entry dated before the latest odometer-setting event never
 * counts, because that event replaces the running total.
 */
fun knownOdometerAt(eventsOldestFirst: List<VehicleEvent>, at: Instant): Distance? {
    var meters: Long? = null
    for (event in eventsOldestFirst) {
        if (event.occurredAt.instant > at) break
        val reading = event.odometer
        when {
            // Any event that sets the odometer is a baseline: the initial odometer or an odometer anchor.
            reading != null -> meters = reading.meters
            event is VehicleEvent.DistanceEntry -> meters = meters?.plus(event.distance.meters)
        }
    }
    return meters?.let { Distance(it) }
}

/** The current odometer: the known odometer at the end of the log. */
fun currentOdometer(eventsOldestFirst: List<VehicleEvent>): Distance? =
    knownOdometerAt(eventsOldestFirst, Instant.DISTANT_FUTURE)
