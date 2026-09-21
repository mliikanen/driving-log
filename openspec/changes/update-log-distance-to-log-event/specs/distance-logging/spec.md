# Spec Delta

## RENAMED Requirements

- FROM: `### Requirement: Log a distance from the vehicle details screen`
- TO: `### Requirement: Log an event from the vehicle details screen`

## MODIFIED Requirements

### Requirement: Log an event from the vehicle details screen
The system SHALL offer a "Log event" action on the vehicle details screen that opens a form for one entry, which today is a distance entry. The form's title SHALL be "Log event", the same name as the Home screen's action that opens it.
Saving a valid entry SHALL return to the details screen, where the entry appears in the recent events and the current
odometer includes it. Leaving the form without saving SHALL add nothing.

#### Scenario: Open the form
- **WHEN** the user taps "Log event" on a vehicle's details screen
- **THEN** the log event form for that vehicle is displayed, titled "Log event"

#### Scenario: Save an entry
- **WHEN** the user enters a valid entry and saves
- **THEN** the vehicle's details screen is displayed and shows the new entry among the recent events

#### Scenario: Leave without saving
- **WHEN** the user leaves the form without saving
- **THEN** no entry is added and the vehicle's current odometer is unchanged
