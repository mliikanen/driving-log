# Spec Delta

## REMOVED Requirements

### Requirement: Home screen lists vehicles
**Reason**: The Home screen becomes a landing screen with the app's main actions; the vehicle list is a screen of its own, reached from it (see the `vehicles` capability, "Vehicle list").
**Migration**: The behavior moves to "Home screen offers the main actions" (the top app bar, the vehicles tile and the add vehicle variant of it) and to the vehicle list screen.

## ADDED Requirements

### Requirement: Home screen offers the main actions
The system SHALL show on the Home screen the application name "Driving Log" in a top app bar, and below it a grid of four actions in two rows of two, each an icon with a short text below it: "Vehicles", "Log event", "Trip" and "Placeholder" (a question mark icon). The grid SHALL stay two by two in
portrait and in landscape, and every action SHALL be at least 48 dp square with its whole area tappable. The first action SHALL open the vehicle list; when the user has no vehicle it SHALL instead read "Add vehicle" (a plus icon) and open the add vehicle screen. An action that is not available
yet ("Log event", "Trip" and "Placeholder" until the changes that build them) SHALL be shown dimmed and SHALL NOT react to a tap; the dimmed action SHALL still be readable by a screen reader, with a name that says it is not available. The vehicle list SHALL NOT be shown on the Home screen.

#### Scenario: Home screen with vehicles
- **WHEN** the Home screen is displayed and the user has added a vehicle
- **THEN** the top app bar shows "Driving Log" and the grid shows "Vehicles", "Log event", "Trip" and "Placeholder", two in each row

#### Scenario: Home screen without vehicles
- **WHEN** the Home screen is displayed and the user has not added any vehicle
- **THEN** the first action reads "Add vehicle" with a plus icon, and tapping it displays the add vehicle screen

#### Scenario: Open the vehicles
- **WHEN** the user taps "Vehicles"
- **THEN** the vehicle list is displayed, and going back from it returns to the Home screen

#### Scenario: An action that is not available
- **WHEN** the user taps "Trip"
- **THEN** nothing happens, and the action is shown dimmed

#### Scenario: The grid in landscape
- **WHEN** the user rotates the device to landscape while the Home screen is displayed
- **THEN** the four actions are still shown two by two and each can be tapped

#### Scenario: The first action does not flash
- **WHEN** the application starts and the vehicles are not yet loaded
- **THEN** the first action is not shown as "Add vehicle" until it is known that there are no vehicles
