# Spec Delta

## MODIFIED Requirements

### Requirement: The log is not changed by editing the vehicle
The system SHALL leave a vehicle's log unchanged when the vehicle's name, license plate or picture is edited.
Existing log events SHALL NOT be modified or removed by any action, **except the one explicit edit action
`event-details`'s "A 'Distance' or 'Odometer reading' event's note can be edited" requirement adds**: changing or
clearing a "Distance" or "Odometer reading" event's note from its details screen. That action SHALL change only the
note of the one event being edited and SHALL NOT alter any other field of it or any other event. Every other action
still leaves the log exactly as it is; the log is otherwise only ever added to.

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
The system SHALL let a "Distance" event or an "Odometer reading" (anchor) event carry an optional note, entered on
the log event form when the event is logged (`distance-logging`, "A pending note is saved with the event"), kept
exactly as entered. Once an event is saved, its note SHALL only be changed through the details screen's "Edit"
action (`event-details`, "A 'Distance' or 'Odometer reading' event's note can be edited"); no other action changes
it. The "Initial odometer" event created when a vehicle is added SHALL NOT carry a note.

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
