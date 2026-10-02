# Spec Delta

## Purpose

Tells vehicles apart by their fuel/engine category: every vehicle has a fuel type from a small fixed set (petrol,
diesel, LPG, CNG, hydrogen, other), preselected as Petrol when the vehicle is added and changeable at any time, and
the fuel type narrows which of refueling-logging's own fuel types are offered when a refueling is logged for it.

## ADDED Requirements

### Requirement: A vehicle has a fuel type from a fixed set
The system SHALL give every vehicle a fuel type from this fixed set, shown in this order: "Petrol", "Diesel", "LPG",
"CNG", "Hydrogen" and "Other". The system SHALL store the fuel type as a code that does not depend on the language
or the device (`PETROL`, `DIESEL`, `LPG`, `CNG`, `HYDROGEN`, `OTHER`), and SHALL treat a code it does not know, such
as one written by a newer version of the app, as the fuel type "Other". Every vehicle SHALL always have exactly one
fuel type: the data model SHALL NOT allow a vehicle without one. This is the vehicle's own coarse fuel/engine
category, distinct from and coarser than the specific fuel type chosen each time a refueling is logged for it
(`refueling-logging`'s own, more precise list). The names are English for now, like the rest of the app.

#### Scenario: The six fuel types are offered
- **WHEN** the user opens the fuel type choice on the add vehicle screen
- **THEN** it offers "Petrol", "Diesel", "LPG", "CNG", "Hydrogen" and "Other", in that order

#### Scenario: The fuel type is stored as a code
- **WHEN** a vehicle of the fuel type "Diesel" is saved
- **THEN** what is stored for the fuel type is the code `DIESEL`, whatever the device language

#### Scenario: An unknown stored code
- **WHEN** a vehicle's stored fuel type is a code the app does not know
- **THEN** the vehicle is shown with the fuel type "Other" and nothing fails

### Requirement: The fuel type is chosen when a vehicle is added
The system SHALL show the fuel type choice on the add vehicle screen as a selectable list, with "Petrol" selected at
first, so that a vehicle always has a fuel type when it is saved. The choice SHALL have no "none" option: exactly
one choice is selected at all times, and choosing another SHALL replace it.

#### Scenario: Petrol is preselected
- **WHEN** the user opens the add vehicle screen
- **THEN** "Petrol" is the only selected fuel type

#### Scenario: Choose a fuel type
- **WHEN** the user chooses "Diesel"
- **THEN** "Diesel" is the only selected fuel type

#### Scenario: Change the choice before saving
- **WHEN** the user chooses "Diesel" and then "LPG"
- **THEN** "LPG" is the only selected fuel type and the vehicle is saved with the fuel type "LPG"

#### Scenario: Saving without touching the choice
- **WHEN** the user enters a name and an odometer, leaves the fuel type as it was and saves
- **THEN** the vehicle is saved with the fuel type "Petrol"

#### Scenario: The choice survives a rotation
- **WHEN** the user chooses a fuel type on the add screen and rotates the device
- **THEN** the same fuel type is still selected

### Requirement: The fuel type can be changed later but not cleared
The system SHALL let the user change a vehicle's fuel type on its edit screen, showing the vehicle's current fuel
type as selected and applying the change when the edit is saved. The system SHALL NOT let the user remove a fuel
type: the choice has no "none" option, so a vehicle that has a fuel type always has one, and the edit SHALL NOT be
saved without one. Changing the fuel type SHALL NOT change the vehicle's odometer, unit, picture, type, color or
log, and SHALL NOT change any refueling already logged for it.

#### Scenario: The current fuel type is selected
- **WHEN** the user opens the edit screen of a vehicle of the fuel type "CNG"
- **THEN** "CNG" is the selected fuel type

#### Scenario: Change the fuel type
- **WHEN** the user edits a vehicle of the fuel type "Petrol", chooses "Diesel" and saves
- **THEN** the vehicle has the fuel type "Diesel" on its details screen

#### Scenario: Cancel editing
- **WHEN** the user chooses another fuel type on the edit screen and leaves without saving
- **THEN** the vehicle keeps its saved fuel type

#### Scenario: Only the fuel type changes
- **WHEN** the user changes only the fuel type of a vehicle and saves
- **THEN** the vehicle's name, plate, odometer, unit, picture, type, color and log are unchanged

#### Scenario: Past refuelings are unaffected
- **WHEN** the user changes the fuel type of a vehicle that already has refuelings logged, from "Petrol" to "Diesel"
- **THEN** every refueling already logged for it keeps the fuel type it was saved with

### Requirement: Vehicles that existed before are given the fuel type Petrol
The system SHALL give every vehicle that was saved before fuel types existed the fuel type "Petrol" when the app is
updated. The user SHALL be able to change that fuel type on the vehicle's edit screen like any other.

#### Scenario: An existing vehicle after the update
- **WHEN** the app is updated and a vehicle that was saved before fuel types existed is shown
- **THEN** it has the fuel type "Petrol"

#### Scenario: Change the fuel type of an existing vehicle
- **WHEN** the user opens the edit screen of a vehicle that was saved before fuel types existed, chooses "LPG" and
  saves
- **THEN** the vehicle has the fuel type "LPG"

#### Scenario: An existing vehicle's other edits keep its fuel type
- **WHEN** the user renames a vehicle that was saved before fuel types existed and saves
- **THEN** the vehicle is saved with the new name and the fuel type "Petrol"

### Requirement: The fuel type filters which refueling fuel types are offered
The system SHALL use a vehicle's fuel type to narrow which of refueling-logging's fuel types are offered when
logging a refueling for it: "Petrol" offers Regular petrol/gasoline, Premium petrol/gasoline and E85/flex fuel;
"Diesel" offers Diesel, Premium diesel and Biodiesel; "LPG" offers LPG; "CNG" offers CNG; "Hydrogen" offers Hydrogen.
Every one of those groups SHALL also offer "Other" (refueling-logging's own catch-all). A vehicle whose fuel type is
"Other" SHALL offer every refueling fuel type, unfiltered.

#### Scenario: A Petrol vehicle's refueling choices
- **WHEN** the user logs a refueling for a vehicle of the fuel type "Petrol"
- **THEN** the fuel type selector offers only Regular petrol/gasoline, Premium petrol/gasoline, E85/flex fuel and
  Other

#### Scenario: A Diesel vehicle's refueling choices
- **WHEN** the user logs a refueling for a vehicle of the fuel type "Diesel"
- **THEN** the fuel type selector offers only Diesel, Premium diesel, Biodiesel and Other

#### Scenario: A single-fuel vehicle's refueling choices
- **WHEN** the user logs a refueling for a vehicle of the fuel type "LPG"
- **THEN** the fuel type selector offers only LPG and Other

#### Scenario: An Other vehicle's refueling choices
- **WHEN** the user logs a refueling for a vehicle of the fuel type "Other"
- **THEN** the fuel type selector offers every fuel type, unfiltered

### Requirement: The fuel type is stored with the vehicle and works offline
The system SHALL keep a vehicle's fuel type on the device with the vehicle, so that it is still there after the app
is closed and reopened, and SHALL offer choosing and changing it without a network connection.

**Platform note:** the behavior is the same on iOS; it is verified on Android now and on iOS once the Xcode project
exists.

#### Scenario: The fuel type survives a restart
- **WHEN** the user adds a vehicle of the fuel type "Hydrogen", closes the app completely and opens it again
- **THEN** the vehicle still has the fuel type "Hydrogen"

#### Scenario: Offline use
- **WHEN** the device has no network connection and the user adds a vehicle and changes its fuel type
- **THEN** every step succeeds and no network error is shown
