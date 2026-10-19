## MODIFIED Requirements

### Requirement: The log is not changed by editing the vehicle
The system SHALL leave a vehicle's log unchanged when the vehicle's name, license plate, picture, type, fuel type or color is edited.
Existing log events SHALL NOT be modified or removed by any action except one: changing an entry's note and photos
from an event's details screen (`event-details`, "An event's note and photos can be edited"), which SHALL change only
that entry's note and photos and nothing else. The log is otherwise
only ever added to.

#### Scenario: Editing does not add events
- **WHEN** the user edits the name and plate of a vehicle and saves
- **THEN** the vehicle's log contains the same events as before

#### Scenario: Changing the picture does not add events
- **WHEN** the user changes or removes the picture of a vehicle and saves
- **THEN** the vehicle's log contains the same events as before

#### Scenario: Logging a distance only adds an event
- **WHEN** the user logs a distance entry
- **THEN** the vehicle's log contains every event it had before, unchanged, and one new "Distance" event

#### Scenario: Editing a note changes only that note
- **WHEN** the user edits a "Distance" event's note through its details screen and navigates back
- **THEN** every other event in the log, and every other field of the edited event, is exactly as it was before

**Platform note:** the behavior is the same on iOS; it is verified on Android now and on iOS once the Xcode project exists.

### Requirement: Distance and odometer-anchor events can carry a note
The system SHALL let a "Distance" or an "Odometer reading" (anchor) event carry an optional note, entered on the log
event form (`distance-logging`, "A pending note is saved with the event") and kept exactly as entered. Once the event
is saved, its note SHALL change only through the details screen's "Edit" action (`event-details`, "An event's note and
photos can be edited"). The "Initial odometer" event SHALL NOT carry a note.

#### Scenario: A distance entry with a note
- **WHEN** the user logs a trip distance with the note "borrowed to Sam"
- **THEN** the log's new "Distance" event holds that note

#### Scenario: An odometer anchor with a note
- **WHEN** the user logs a new odometer count with a note, at a time when no odometer is known yet
- **THEN** the log's new "Odometer reading" event holds that note

#### Scenario: A note is optional
- **WHEN** the user logs a distance entry without ever adding a note
- **THEN** the log's new event holds no note

#### Scenario: A note can be corrected after saving
- **WHEN** the user changes a saved "Distance" event's note through its details screen's "Edit" action
- **THEN** the log holds that event with the changed note, and no other event or field is affected

### Requirement: Distance and odometer-anchor events can carry photos
The system SHALL let a "Distance" event or an "Odometer reading" (anchor) event carry from 0 to 5 photos, attached
on the log event form when the event is logged (`distance-logging`, "Attached photos are saved with the event"), or
added or removed later through the details screen's "Edit" action (`event-details`, "An event's note and
photos can be edited"). The "Initial odometer" event created when a vehicle is added SHALL NOT carry any photo.

#### Scenario: A distance entry with photos
- **WHEN** the user logs a trip distance with 2 photos attached
- **THEN** the log's new "Distance" event holds those 2 photos

#### Scenario: Photos are optional
- **WHEN** the user logs a distance entry without attaching any photo
- **THEN** the log's new event holds no photo
