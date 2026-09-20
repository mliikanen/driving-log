# Spec Delta

## RENAMED Requirements

- FROM: `### Requirement: Home screen shows the app name and an empty state`
- TO: `### Requirement: Home screen lists vehicles`

## MODIFIED Requirements

### Requirement: Home screen lists vehicles
The system SHALL show the application name "Driving Log" in a top app bar on the Home screen, and SHALL show the
user's vehicles in the content area as specified in the `vehicles` capability. When the user has no vehicles, the
content area SHALL show an empty state that invites the user to add a vehicle. The Home screen SHALL offer an action
to add a vehicle.

#### Scenario: Home screen with vehicles
- **WHEN** the Home screen is displayed and the user has added vehicles
- **THEN** the top app bar shows "Driving Log"
- **AND** the content area lists the vehicles

#### Scenario: Empty Home screen content
- **WHEN** the Home screen is displayed and the user has not added any vehicle
- **THEN** the top app bar shows "Driving Log"
- **AND** the content area shows the empty state and the action to add a vehicle
