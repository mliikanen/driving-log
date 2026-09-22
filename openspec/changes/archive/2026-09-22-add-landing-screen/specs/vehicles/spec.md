# Spec Delta

## MODIFIED Requirements

### Requirement: Vehicle list
The system SHALL show every vehicle the user has added on the vehicle list screen, which the Home screen's "Vehicles" action opens and which is titled "Vehicles" with a back arrow that returns to the Home screen. The list is ordered by name without regard to letter
case, and SHALL show each vehicle's small picture (or the placeholder when it has none), its name and, when it has one, its license plate. When there are no vehicles the
system SHALL show an empty state that invites the user to add a vehicle. The vehicle list screen SHALL offer an action to
add a vehicle.

#### Scenario: Vehicles are listed alphabetically
- **WHEN** the user has vehicles named "van", "Bike" and "Family car"
- **THEN** the vehicle list shows them in the order "Bike", "Family car", "van"

#### Scenario: Vehicles show their picture
- **WHEN** the user has a vehicle with a picture and a vehicle without one
- **THEN** the list shows the small picture for the first and the placeholder for the second

#### Scenario: Empty vehicle list
- **WHEN** the vehicle list screen is displayed and the user has not added any vehicle
- **THEN** the screen shows an empty state and the action to add a vehicle

#### Scenario: Open the add screen
- **WHEN** the user taps the add vehicle action on the vehicle list screen
- **THEN** the add vehicle screen is displayed

#### Scenario: Back to the Home screen
- **WHEN** the user taps the back arrow of the vehicle list screen
- **THEN** the Home screen is displayed
