# Spec Delta

## ADDED Requirements

### Requirement: Distance and odometer-anchor events can carry photos
The system SHALL let a "Distance" event or an "Odometer reading" (anchor) event carry from 0 to 5 photos, attached
on the log event form when the event is logged (`distance-logging`, "Attached photos are saved with the event"), or
added or removed later through the details screen's "Edit" action (`add-event-editing`, `event-pictures`'s own
delta of `event-details`). The "Initial odometer" event created when a vehicle is added SHALL NOT carry any photo.

#### Scenario: A distance entry with photos
- **WHEN** the user logs a trip distance with 2 photos attached
- **THEN** the log's new "Distance" event holds those 2 photos

#### Scenario: Photos are optional
- **WHEN** the user logs a distance entry without attaching any photo
- **THEN** the log's new event holds no photo

## MODIFIED Requirements

### Requirement: A note's presence is shown as an icon on the event row
The system SHALL show a small icon cluster, anchored at the bottom-end (bottom-trailing) corner of the row, in the
recent events on the vehicle's details screen (`vehicle-log`, "Recent events on the details screen") and in the full
log (`vehicle-log`, "Full log"), for any event that has a non-empty note, one or more photos, or both. The note icon
SHALL be shown first (when the event has a note), followed by the photo icon (when the event has at least one
photo); an event with neither SHALL show neither icon, and the row SHALL show no other, larger indicator (no
thumbnail, no "hero" image) — the icons are presence-only. The row SHALL NOT show the note's text or a photo's
content; reading either back is done from the event's details screen (`event-details`).

#### Scenario: A row with a note
- **WHEN** the recent events or the full log include a "Distance" event that has a note
- **THEN** that event's row shows the note icon, at the row's bottom-end corner

#### Scenario: A row without a note
- **WHEN** an event has no note and no photo
- **THEN** its row shows neither icon

#### Scenario: The note's text is not on the row
- **WHEN** an event with a note is shown in either list
- **THEN** the row shows the icon but not the note's text

#### Scenario: A row with photos only
- **WHEN** an event has one or more photos and no note
- **THEN** its row shows only the photo icon, at the row's bottom-end corner

#### Scenario: A row with both
- **WHEN** an event has both a note and one or more photos
- **THEN** its row shows both icons together at the bottom-end corner, the note icon first

#### Scenario: No thumbnail or hero image in the row
- **WHEN** an event with photos is shown in either list
- **THEN** the row shows the photo icon only, never a thumbnail or a larger image of any attached photo
