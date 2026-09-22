# Spec Delta

## MODIFIED Requirements

### Requirement: Vehicle details screen
The system SHALL show a details screen when the user selects a vehicle in the list. The details screen SHALL show the
vehicle's large picture at the top (or the placeholder when it has none), its name, its license plate when it has one, its current odometer in the vehicle's unit, its most recent log events as
specified in the `vehicle-log` capability, and actions to log an event, to open the full log and to edit the vehicle.
Going back SHALL return to the vehicle list.

#### Scenario: Open a vehicle
- **WHEN** the user taps "Family car" in the vehicle list
- **THEN** the details screen shows the name "Family car", the plate, the current odometer and the recent log events

#### Scenario: The picture is shown
- **WHEN** the user opens the details screen of a vehicle with a picture
- **THEN** the screen shows the large picture above the name

#### Scenario: Open the log distance form
- **WHEN** the user taps "Log event" on the details screen
- **THEN** the log event form is displayed, as specified in the `distance-logging` capability

#### Scenario: Back to the list
- **WHEN** the user navigates back from the details screen
- **THEN** the vehicle list is displayed
