package com.mikonoma.drivinglog.vehicle.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.format.eventRowContent
import kotlinx.datetime.TimeZone

/** One row of the log, shared by the recent events and the full log. The texts come from [eventRowContent]. */
@Composable
fun EventRow(
    event: VehicleEvent,
    unit: OdometerUnit,
    symbols: NumberSymbols,
    deviceZone: TimeZone,
    modifier: Modifier = Modifier,
) {
    val content = eventRowContent(event, unit, symbols, deviceZone)
    ListItem(
        headlineContent = { Text(content.label) },
        supportingContent = {
            Column {
                Text(content.moment)
                content.loggedOdometer?.let { Text(it) }
            }
        },
        trailingContent = { Text(content.trailing) },
        modifier = modifier,
    )
}
