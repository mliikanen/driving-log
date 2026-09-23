# Spec Delta

## MODIFIED Requirements

### Requirement: Log an event from the vehicle details screen
The system SHALL offer a "Log event" action on the vehicle details screen that opens a form for one distance entry.
Saving a valid entry SHALL return to the details screen, where the entry appears in the recent events and the current
odometer includes it. Leaving the form without saving SHALL add nothing. The form's dismiss action, in the top-left of
its top app bar, SHALL be a close "X" (a Material full-screen dialog's dismiss icon), not a back arrow, since leaving
the form always discards whatever was entered.

#### Scenario: Open the form
- **WHEN** the user taps "Log event" on a vehicle's details screen
- **THEN** the log event form for that vehicle is displayed

#### Scenario: Save an entry
- **WHEN** the user enters a valid entry and saves
- **THEN** the vehicle's details screen is displayed and shows the new entry among the recent events

#### Scenario: Leave without saving
- **WHEN** the user leaves the form without saving
- **THEN** no entry is added and the vehicle's current odometer is unchanged

#### Scenario: The dismiss action is a close icon
- **WHEN** the user opens the log event form, from a vehicle's details screen or from the Home screen
- **THEN** the top-left action of the form's top app bar is a close "X", not a back arrow
