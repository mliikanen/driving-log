# Spec Delta

## ADDED Requirements

### Requirement: Distance and odometer-anchor events can carry a note
The system SHALL let a "Distance" event or an "Odometer reading" (anchor) event carry an optional note, entered on
the log event form when the event is logged (`distance-logging`, "A pending note is saved with the event"), kept
exactly as entered. Once an event is saved, its note SHALL NOT be changed or removed by any action available today —
editing a previously logged event, including its note, is a separate, upcoming change. The "Initial odometer" event
created when a vehicle is added SHALL NOT carry a note.

#### Scenario: A distance entry with a note
- **WHEN** the user logs a trip distance with the note "borrowed to Sam"
- **THEN** the log's new "Distance" event holds that note

#### Scenario: An odometer anchor with a note
- **WHEN** the user logs a new odometer count with a note, at a time when no odometer is known yet
- **THEN** the log's new "Odometer reading" event holds that note

#### Scenario: A note is optional
- **WHEN** the user logs a distance entry without ever adding a note
- **THEN** the log's new event holds no note

### Requirement: A note's presence is shown as an icon on the event row
The system SHALL show a small icon on any event row — in the recent events on the vehicle's details screen
(`vehicle-log`, "Recent events on the details screen") and in the full log (`vehicle-log`, "Full log") — whose event
has a non-empty note, and SHALL show no such icon on a row whose event has none. The row SHALL NOT show the note's
text; reading a saved note back is a separate, upcoming change.

#### Scenario: A row with a note
- **WHEN** the recent events or the full log include a "Distance" event that has a note
- **THEN** that event's row shows the note icon

#### Scenario: A row without a note
- **WHEN** an event has no note
- **THEN** its row shows no note icon

#### Scenario: The note's text is not on the row
- **WHEN** an event with a note is shown in either list
- **THEN** the row shows the icon but not the note's text
