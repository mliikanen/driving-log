package com.mikonoma.drivinglog.vehicle.format

import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.locale.TimeFormat
import com.mikonoma.drivinglog.vehicle.domain.FuelUnit
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.ui.label
import kotlinx.datetime.TimeZone

/** The texts of one log row. Kept apart from the composable so they can be tested without a screen. */
data class EventRowContent(
    val label: String,
    /** When it happened, in the zone it was entered in (the zone name follows when it is not the device's zone). */
    val moment: String,
    /** An odometer reading's reading, a distance entry's distance with a plus sign, or a refueling's fuel amount. */
    val trailing: String,
    /** A distance entry logged by odometer: the count that was typed. */
    val loggedOdometer: String?,
    /** True for a distance entry: its [trailing] figure is a distance, which the theme draws in the distance accent color. */
    val isDistance: Boolean = false,
    /** True when the event's entry has a non-empty note (`add-event-notes`), on every row of the entry; the row shows an icon, never the note's text. */
    val hasNote: Boolean = false,
    /** True when the event's entry has one or more photos (`add-event-pictures`), on every row of the entry; the row shows an icon, never a thumbnail. */
    val hasPhotos: Boolean = false,
)

fun eventRowContent(
    event: VehicleEvent,
    unit: OdometerUnit,
    symbols: NumberSymbols,
    deviceZone: TimeZone,
    timeFormat: TimeFormat = TimeFormat(),
): EventRowContent = when (event) {
    is VehicleEvent.OdometerReading -> EventRowContent(
        label = event.label,
        moment = formatMoment(event.occurredAt, deviceZone, timeFormat),
        trailing = formatOdometer(event.reading, unit, symbols),
        loggedOdometer = null,
        hasNote = event.note != null,
        hasPhotos = event.photoIds.isNotEmpty(),
    )

    is VehicleEvent.DistanceEntry -> EventRowContent(
        label = event.label,
        moment = formatMoment(event.occurredAt, deviceZone, timeFormat),
        trailing = "+" + formatOdometer(event.distance, unit, symbols),
        loggedOdometer = event.loggedOdometer?.let { "Odometer " + formatOdometer(it, unit, symbols) },
        isDistance = true,
        hasNote = event.note != null,
        hasPhotos = event.photoIds.isNotEmpty(),
    )

    is VehicleEvent.Refueling -> EventRowContent(
        label = event.label,
        moment = formatMoment(event.occurredAt, deviceZone, timeFormat),
        // Shown in the unit it was entered in, never a plus sign: a refueling is not itself a distance
        // (`vehicle-log`'s "Recent events on the details screen").
        trailing = formatFuelAmount(event.amount, event.unit, symbols),
        loggedOdometer = null,
        hasNote = event.note != null,
        hasPhotos = event.photoIds.isNotEmpty(),
    )
}
