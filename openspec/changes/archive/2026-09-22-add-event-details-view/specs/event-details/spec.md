# Spec Delta

## Purpose
Shows everything the system currently stores about one logged event, read-only, re-observed live from the log, once
tapped from a vehicle's recent events or full log.

## ADDED Requirements

### Requirement: A logged event's details can be viewed
The system SHALL open a read-only details screen for an event when its row is tapped, for every event kind. The
screen SHALL show the event's kind, its full date, time and time zone (as the row itself already does), its figure
(the reading or distance the row shows), and its note in full, untruncated, when it has one. An "Initial odometer"
event SHALL show no note section, since it can never carry one. Going back SHALL return to whichever list opened it.

#### Scenario: Open a distance event's details
- **WHEN** the user taps a "Distance" event's row in the recent events or the full log
- **THEN** the details screen shows that event's date, time, time zone, distance and note (if any), in full

#### Scenario: Open the initial odometer event's details
- **WHEN** the user taps the "Initial odometer" event's row
- **THEN** the details screen shows its date, time, time zone and reading, and no note section

#### Scenario: Back returns to the list
- **WHEN** the user navigates back from the details screen
- **THEN** the list that opened it is shown again, as it was

### Requirement: The details screen reflects the event live
The system SHALL show the details screen's content from the event as currently stored, not from data captured at the
moment the row was tapped, so that a change made elsewhere to the same event (once such a change is possible) is
reflected without the user having to navigate away and back.

#### Scenario: The screen is not stale
- **WHEN** the details screen for an event is open
- **THEN** its content is read from the event's current stored state, not from a snapshot taken when the screen opened
