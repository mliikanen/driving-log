## MODIFIED Requirements

### Requirement: A vehicle has a fuel type from a fixed set
The system SHALL give every vehicle a fuel type from this fixed set, in this order: "Petrol", "Diesel", "LPG", "CNG",
"Hydrogen" and "Other". Every vehicle SHALL have exactly one fuel type; the data model SHALL NOT allow a vehicle
without one. It is the vehicle's own coarse fuel/engine category, coarser than and separate from the fuel type chosen
each time a refueling is logged (`refueling-logging`). The names are English for now, like the rest of the app.

#### Scenario: The six fuel types are offered
- **WHEN** the user opens the fuel type choice on the add vehicle screen
- **THEN** it offers "Petrol", "Diesel", "LPG", "CNG", "Hydrogen" and "Other", in that order

#### Scenario: The fuel type is stored as a code
- **WHEN** a vehicle of the fuel type "Diesel" is saved
- **THEN** what is stored for the fuel type is the code `DIESEL`, whatever the device language

#### Scenario: An unknown stored code
- **WHEN** a vehicle's stored fuel type is a code the app does not know
- **THEN** the vehicle is shown with the fuel type "Other" and nothing fails

### Requirement: The fuel type can be changed later but not cleared
The system SHALL let the user change a vehicle's fuel type on its edit screen, showing the current fuel type as
selected and applying the change when the edit is saved. It SHALL NOT let the user remove a fuel type: the choice has
no "none" option, and the edit SHALL NOT be saved without one. Changing the fuel type SHALL NOT change the vehicle's
odometer, unit, picture, type, color or log, nor any refueling already logged for it.

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

### Requirement: The fuel type filters which refueling fuel types are offered
The system SHALL use a vehicle's fuel type to narrow which of `refueling-logging`'s fuel types are offered when
logging a refueling for it: "Petrol" offers Regular petrol/gasoline, Premium petrol/gasoline and E85/flex fuel;
"Diesel" offers Diesel, Premium diesel and Biodiesel; "LPG", "CNG" and "Hydrogen" each offer the fuel of that name.
Each of these groups SHALL also offer "Other". A vehicle whose fuel type is "Other" SHALL offer every refueling fuel
type, unfiltered.

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

## ADDED Requirements

### Requirement: A fuel type is stored as a code that does not depend on the language
The system SHALL store a vehicle's fuel type as a code that does not depend on the language or the device: `PETROL`,
`DIESEL`, `LPG`, `CNG`, `HYDROGEN` or `OTHER`. It SHALL treat a code it does not know, such as one written by a newer
version of the app, as the fuel type "Other".

#### Scenario: Every fuel type's code
- **WHEN** vehicles of each of the six fuel types are saved and read back
- **THEN** their stored codes are `PETROL`, `DIESEL`, `LPG`, `CNG`, `HYDROGEN` and `OTHER`, and each reads back as the
  fuel type it was saved with
