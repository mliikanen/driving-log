# Spec Delta

## MODIFIED Requirements

### Requirement: A trip distance must be entered and above zero
The system SHALL require a number in the active field: while it is empty, the form's Save action SHALL be disabled
and SHALL NOT be tappable. A trip distance SHALL be more than zero: once the field is non-empty, the system SHALL
show an error on the field and SHALL NOT save the entry when a typed trip distance is zero. The field's label ("Trip
distance" or "New odometer", whichever is active) SHALL carry a trailing "*", and the form SHALL show a line near
its Save action explaining the convention: "* indicates a required field".

#### Scenario: Empty field
- **WHEN** the active field has nothing typed
- **THEN** the form's Save action is disabled

#### Scenario: Zero distance
- **WHEN** the user types 0 as the trip distance and saves
- **THEN** the system shows an error that the distance must be more than zero and adds nothing

#### Scenario: The error clears when the user types
- **WHEN** an error is shown on the field and the user types a digit
- **THEN** the error is no longer shown

#### Scenario: The required field is marked
- **WHEN** the user opens the log event form
- **THEN** the active field's label ("Trip distance" or "New odometer") carries a trailing "*", and a line near the Save action reads "* indicates a required field"
