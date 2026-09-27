package com.mikonoma.drivinglog.vehicle.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.locale.TimeFormat
import com.mikonoma.drivinglog.ui.EventIcons
import com.mikonoma.drivinglog.ui.PhotoIcons
import com.mikonoma.drivinglog.ui.theme.DrivingLogTheme
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.format.eventRowContent
import kotlinx.datetime.TimeZone

/**
 * One row of the log, shared by the recent events and the full log. The texts come from [eventRowContent]. Tapping
 * it opens the event's details screen (add-event-details-view). The note and photo presence icons
 * (`add-event-notes`, `add-event-pictures`) are shown, note first, as a small cluster anchored at the row's
 * bottom-end corner — not [ListItem]'s `leadingContent`, which this used to be the note icon's only occupant, since
 * two presence icons need to sit together rather than each claim a whole slot of the row's own layout.
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
    Box(modifier.clickable { onClick(event.id) }) {
        ListItem(
            headlineContent = { Text(content.label) },
            supportingContent = {
                Column {
                    Text(content.moment)
                    content.loggedOdometer?.let { Text(it) }
                }
            },
            // Only a distance is drawn in the distance accent: an odometer reading (initial or anchor) is not a distance.
            trailingContent = {
                Text(content.trailing, color = if (content.isDistance) DrivingLogTheme.domain.distance else Color.Unspecified)
            },
        )
        if (content.hasNote || content.hasPhotos) {
            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Presence only: reading a saved note or a photo back is done from the details screen this row opens.
                if (content.hasNote) {
                    Icon(
                        EventIcons.Note,
                        contentDescription = "Has a note",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("event_note_icon"),
                    )
                }
                if (content.hasPhotos) {
                    Icon(
                        PhotoIcons.Camera,
                        contentDescription = "Has photos",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("event_photo_icon"),
                    )
                }
            }
        }
    }
}
