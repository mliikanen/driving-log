# Spec Delta

## RENAMED Requirements

- FROM: `### Requirement: Log a distance from the vehicle details screen`
- TO: `### Requirement: Log an event from the vehicle details screen`

## MODIFIED Requirements

### Requirement: Log an event from the vehicle details screen
The system SHALL offer a "Log event" action on the vehicle details screen that opens a form for one distance entry.
Saving a valid entry SHALL return to the details screen, where the entry appears in the recent events and the current
odometer includes it. Leaving the form without saving SHALL add nothing.

#### Scenario: Open the form
- **WHEN** the user taps "Log event" on a vehicle's details screen
- **THEN** the log event form for that vehicle is displayed

#### Scenario: Save an entry
- **WHEN** the user enters a valid entry and saves
- **THEN** the vehicle's details screen is displayed and shows the new entry among the recent events

#### Scenario: Leave without saving
- **WHEN** the user leaves the form without saving
- **THEN** no entry is added and the vehicle's current odometer is unchanged

### Requirement: A distance is logged as a trip distance or as a new odometer count
The system SHALL let the user choose, on the log event form, between "Trip distance" and "New odometer". "Trip distance"
SHALL be preselected. With "Trip distance" the number entered is the distance travelled. With "New odometer" the number
entered is the odometer count now, and the distance logged is that count minus the previous known odometer at the entry's
date and time (or, when no odometer is known at that time, an odometer anchor is saved instead). Each way SHALL keep its own typed number when the user switches between them.

#### Scenario: Log by trip distance
- **WHEN** a vehicle's current odometer is 45200 km and the user logs a trip distance of 30 km and saves
- **THEN** the log contains a distance entry of 30 km and the vehicle's current odometer is 45230 km

#### Scenario: Log by new odometer
- **WHEN** a vehicle's current odometer is 45200 km and the user chooses "New odometer", types 45250 and saves
- **THEN** the log contains a distance entry of 50 km and the vehicle's current odometer is 45250 km

#### Scenario: Preselected way
- **WHEN** the user opens the log event form
- **THEN** "Trip distance" is selected

#### Scenario: Each way keeps its own number
- **WHEN** the user types 30 as a trip distance, switches to "New odometer", types 45250, and switches back
- **THEN** the trip distance still shows 30 and the odometer field still shows 45250

### Requirement: A distance can be logged from the Home screen
The system SHALL make the Home screen's Log event action (an icon with no visible text, named "Log event" for a screen reader; `app-shell`, "Home screen offers the main actions") open the log event form when the user has at least one vehicle. With no vehicle the action SHALL stay a disabled Material 3 component, exactly as the other not-yet-available Home screen actions are (`app-shell`, "Home screen offers the main actions"), and SHALL NOT react to a tap. Saving a valid entry SHALL return to the Home screen; leaving the form without saving SHALL add nothing.

#### Scenario: Open the form from the Home screen
- **WHEN** the user has a vehicle and taps the Home screen's Log event action
- **THEN** the log event form is displayed with a vehicle selector at the top

#### Scenario: No vehicle
- **WHEN** the user has no vehicle and taps the Home screen's Log event action
- **THEN** nothing happens, and the action is shown as disabled

#### Scenario: Save from the Home screen route
- **WHEN** the user opened the form from the Home screen, enters a valid entry and saves
- **THEN** the Home screen is displayed, and the entry is in the chosen vehicle's log

#### Scenario: Leave without saving
- **WHEN** the user opened the form from the Home screen and leaves it without saving
- **THEN** the Home screen is displayed and no entry is added

### Requirement: The vehicle is chosen with a selector when logging starts from the Home screen
When the log event form is opened from the Home screen, the system SHALL show a vehicle selector at the top of the form: a dropdown that shows the chosen vehicle (its picture or icon and its name) and lists every vehicle the user has, in the order of the vehicle list. Choosing a vehicle SHALL make the form
about that vehicle: the previous known odometer, the checks of a new odometer count and the log the entry will join are that vehicle's, and the unit starts as that vehicle's (its unit family and its remembered choice of tenths). The digits already typed, the way (trip distance or new odometer), the date, the time and the time zone SHALL be kept when the vehicle is changed, and the typed digits are converted to the new unit as when the unit is changed by hand. The chosen vehicle SHALL survive a rotation of the device and the restart of the process. When the form is opened from a vehicle's details screen the selector SHALL NOT be shown and the vehicle SHALL be that vehicle.

#### Scenario: The selector lists the vehicles
- **WHEN** the user has vehicles named "van", "Bike" and "Family car" and opens the form from the Home screen and opens the selector
- **THEN** it lists "Bike", "Family car" and "van", in that order

#### Scenario: Choose another vehicle
- **WHEN** the user has typed a trip distance for "Family car" and chooses "Bike" in the selector
- **THEN** the form is about "Bike": its previous known odometer is shown, the typed digits are still there, and saving adds the entry to "Bike"

#### Scenario: The unit follows the chosen vehicle
- **WHEN** the user chooses a vehicle whose odometer is in miles after one whose odometer is in kilometers
- **THEN** the unit starts as miles (with that vehicle's remembered choice of tenths) and the typed digits are kept

#### Scenario: From the details screen there is no selector
- **WHEN** the user taps "Log event" on a vehicle's details screen
- **THEN** the form has no vehicle selector and is about that vehicle

#### Scenario: The choice survives a rotation
- **WHEN** the user has chosen a vehicle in the selector and rotates the device
- **THEN** the form shows the same vehicle and what was typed

## ADDED Requirements

### Requirement: The kind of event is chosen
The system SHALL show a "Kind" selector at the top of the log event form: a dropdown, in the same style as the vehicle selector, listing the kinds of event the form can log — today only "Distance", which SHALL be selected. When the form is opened from the Home screen the Kind selector SHALL share one row with the vehicle selector, the two of equal width and height, the Kind selector first (on the left). When the form is opened from a vehicle's details screen (where there is no vehicle selector) the Kind selector SHALL take the row alone. Choosing the only kind SHALL leave the form as it is; further kinds and what choosing them does are added by later changes.

#### Scenario: The kind selector is shown on both routes
- **WHEN** the user opens the log event form, from the Home screen or from a vehicle's details screen
- **THEN** a "Kind" selector is shown at the top of the form, showing "Distance" selected

#### Scenario: Sharing the row with the vehicle selector
- **WHEN** the form is opened from the Home screen
- **THEN** the Kind selector and the vehicle selector are shown side by side, each half the row's width and the same height, the Kind selector on the left

#### Scenario: Alone from the details screen
- **WHEN** the form is opened from a vehicle's details screen
- **THEN** the Kind selector is shown alone, the full width of the row, and no vehicle selector is shown

#### Scenario: Choosing the only kind changes nothing
- **WHEN** the user opens the Kind selector and chooses "Distance" again
- **THEN** the form is unchanged
