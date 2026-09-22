# Spec Delta

## ADDED Requirements

### Requirement: An event row opens its details
The system SHALL make every event row tappable, in the recent events on the vehicle's details screen (`vehicle-log`,
"Recent events on the details screen") and in the full log (`vehicle-log`, "Full log"), opening that event's details
screen (`event-details`).

#### Scenario: Tap a row in recent events
- **WHEN** the user taps an event row in the recent events section
- **THEN** that event's details screen opens

#### Scenario: Tap a row in the full log
- **WHEN** the user taps an event row in the full log
- **THEN** that event's details screen opens
