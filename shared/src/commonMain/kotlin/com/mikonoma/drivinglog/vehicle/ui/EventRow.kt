package com.mikonoma.drivinglog.vehicle.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.locale.TimeFormat
import com.mikonoma.drivinglog.ui.EventIcons
import com.mikonoma.drivinglog.ui.theme.DrivingLogTheme
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.format.eventRowContent
import kotlinx.datetime.TimeZone

/**
 * One row of the log, shared by the recent events and the full log. The texts come from [eventRowContent]. Tapping
 * it opens the event's details screen (add-event-details-view).
 */
@Composable
fun EventRow(
    event: VehicleEvent,
    unit: OdometerUnit,
    symbols: NumberSymbols,
    deviceZone: TimeZone,
    timeFormat: TimeFormat,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = eventRowContent(event, unit, symbols, deviceZone, timeFormat)
    ListItem(
        headlineContent = { Text(content.label) },
        supportingContent = {
            Column {
                Text(content.moment)
                content.loggedOdometer?.let { Text(it) }
            }
        },
        // Presence only (add-event-notes): reading a saved note back is done from the details screen this row opens.
        leadingContent = if (content.hasNote) {
            { Icon(EventIcons.Note, contentDescription = "Has a note", modifier = Modifier.testTag("event_note_icon")) }
        } else null,
        // Only a distance is drawn in the distance accent: an odometer reading (initial or anchor) is not a distance.
        trailingContent = {
            Text(content.trailing, color = if (content.isDistance) DrivingLogTheme.domain.distance else Color.Unspecified)
        },
        modifier = modifier.clickable { onClick(event.id) },
    )
}
