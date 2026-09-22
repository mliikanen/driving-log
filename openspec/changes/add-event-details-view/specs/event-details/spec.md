# Spec Delta

## Purpose
Shows everything the system stored about one logged event, read-only, once tapped from a vehicle's recent events or
full log. A stub: the requirement below is the proposed shape, settled together with the open questions in the design.

## ADDED Requirements

### Requirement: A logged event's details can be viewed
The system SHALL open a read-only details screen for an event when its row is tapped, showing its kind, its full
date, time and time zone, its figure, and its note when it has one, in full. Going back SHALL return to whichever
list opened it.

#### Scenario: Open an event's details
- **WHEN** the user taps a "Distance" event's row in the recent events or the full log
- **THEN** the details screen shows that event's date, time, time zone, distance and note (if any), in full

#### Scenario: Back returns to the list
- **WHEN** the user navigates back from the details screen
- **THEN** the list that opened it is shown again, as it was
