# Spec Delta

## MODIFIED Requirements

### Requirement: Home screen offers the main actions
The system SHALL show on the Home screen the application name "Driving Log" in a top app bar, and below it a grid of four actions in two rows of two, each an icon with no visible text. The icons are a car (vehicles), a pencil on a note ("Log event"), a route ("Trip") and a question mark ("Placeholder"), and every action SHALL have a name for a screen reader: "Vehicles", "Log event", "Trip" and "Placeholder". The grid SHALL stay two by two in
portrait and in landscape, and every action SHALL be at least 48 dp square with its whole area tappable. The first action SHALL always show the car icon; it SHALL open the vehicle list, or, when the user has no vehicle (hidden ones count: `vehicles`, "Hide a vehicle"), the add vehicle screen, and only its name for a screen reader changes with that ("Add vehicle" instead of "Vehicles"). An action that is not available
yet ("Log event", "Trip" and "Placeholder" until the changes that build them) SHALL be disabled Material 3 components: shown in Material's disabled colors and not in the colors of an available action, not reacting to a tap, and exposed to a screen reader as disabled (with their names still readable). The vehicle list SHALL NOT be shown on the Home screen.

#### Scenario: Home screen with vehicles
- **WHEN** the Home screen is displayed and the user has added a vehicle
- **THEN** the top app bar shows "Driving Log" and the grid shows the car, note, route and question mark icons, two in each row, with no text on the tiles

#### Scenario: Home screen without vehicles
- **WHEN** the Home screen is displayed and the user has not added any vehicle
- **THEN** the first action still shows the car icon, its name for a screen reader is "Add vehicle", and tapping it displays the add vehicle screen

#### Scenario: Open the vehicles
- **WHEN** the user has a vehicle and taps the first action
- **THEN** the vehicle list is displayed, and going back from it returns to the Home screen

#### Scenario: An action that is not available
- **WHEN** the user taps the trip action
- **THEN** nothing happens, and the action is shown in the disabled colors, unlike the available actions

#### Scenario: Available and disabled actions look different
- **WHEN** the Home screen is displayed with a vehicle, in the light and in the dark scheme
- **THEN** the car action is drawn in the theme's action colors, the other three in the theme's disabled colors, and the two are told apart by color as well as by the icon

#### Scenario: The grid in landscape
- **WHEN** the user rotates the device to landscape while the Home screen is displayed
- **THEN** the four actions are still shown two by two and each can be tapped

#### Scenario: The first action waits for the vehicles
- **WHEN** the application starts and the vehicles are not yet loaded
- **THEN** the first action does not react to a tap until it is known whether the user has vehicles, so it cannot open the wrong screen

#### Scenario: Every vehicle hidden
- **WHEN** the user has vehicles and has hidden all of them
- **THEN** the first action opens the vehicle list (with its "Hidden vehicles" entry), and "Log event" is not available
