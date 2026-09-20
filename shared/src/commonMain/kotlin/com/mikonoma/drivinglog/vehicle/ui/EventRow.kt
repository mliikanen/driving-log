package com.mikonoma.drivinglog.vehicle.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.format.formatMoment
import com.mikonoma.drivinglog.vehicle.format.formatOdometer
import kotlinx.datetime.TimeZone

/**
 * One row of the log, shared by the recent events and the full log: the event type, when it happened in the zone it was
 * entered in, and its reading (an initial odometer) or its distance with a plus sign (a distance entry), all in the vehicle's unit.
 */
@Composable
fun EventRow(
    event: VehicleEvent,
    unit: OdometerUnit,
    symbols: NumberSymbols,
    deviceZone: TimeZone,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(event.label) },
        supportingContent = {
            Column {
                Text(formatMoment(event.occurredAt, deviceZone))
                if (event is VehicleEvent.DistanceEntry && event.loggedOdometer != null) {
                    Text("Odometer " + formatOdometer(event.loggedOdometer, unit, symbols))
                }
            }
        },
        trailingContent = {
            when (event) {
                is VehicleEvent.InitialOdometer -> Text(formatOdometer(event.reading, unit, symbols))
                is VehicleEvent.DistanceEntry -> Text("+" + formatOdometer(event.distance, unit, symbols))
            }
        },
        modifier = modifier,
    )
}
