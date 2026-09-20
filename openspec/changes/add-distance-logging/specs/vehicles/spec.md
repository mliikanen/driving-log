# Spec Delta

## MODIFIED Requirements

### Requirement: Vehicle details screen
The system SHALL show a details screen when the user selects a vehicle in the list. The details screen SHALL show the
vehicle's name, its license plate when it has one, its current odometer in the vehicle's unit, its most recent log events as
specified in the `vehicle-log` capability, and actions to log a distance, to open the full log and to edit the vehicle.
Going back SHALL return to the vehicle list.

#### Scenario: Open a vehicle
- **WHEN** the user taps "Family car" in the vehicle list
- **THEN** the details screen shows the name "Family car", the plate, the current odometer and the recent log events

#### Scenario: Open the log distance form
- **WHEN** the user taps "Log distance" on the details screen
- **THEN** the log distance form is displayed, as specified in the `distance-logging` capability

#### Scenario: Back to the list
- **WHEN** the user navigates back from the details screen
- **THEN** the vehicle list is displayed
