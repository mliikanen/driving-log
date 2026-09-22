# Spec Delta

## MODIFIED Requirements

### Requirement: The log is not changed by editing the vehicle
The system SHALL leave a vehicle's log unchanged when the vehicle's name, license plate or picture is edited.
Existing log events SHALL NOT be modified or removed by any action, **except the one explicit edit action this
capability adds**: correcting a previously logged event's figure, moment, unit or note (and, once it exists, its
picture) through the details screen (`event-details`). That action SHALL change only the one event being corrected,
in whatever way the design settles (in place, or as a correction that supersedes it — see this change's design.md),
and SHALL NOT alter any other event. Every other action still leaves the log exactly as it is; the log is otherwise
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

#### Scenario: Correcting an event changes only that event
- **WHEN** the user corrects a previously logged event's figure through the details screen and saves the correction
- **THEN** every other event in the log is exactly as it was, and only the corrected event's information reflects the change (whether by that event's row now holding it or by a later correction record superseding it)
