# Spec Delta

## Purpose

Tells vehicles apart by kind: every vehicle has a type from a small fixed set (car, SUV, van, truck, bus, motorcycle, scooter,
other), chosen when the vehicle is added and changeable later, and the type decides which generic icon stands in for a vehicle
that has no picture.

## ADDED Requirements

### Requirement: A vehicle has a type from a fixed set
The system SHALL give every vehicle a type from this fixed set, each with a name and its own icon, shown in this order: "Car",
"SUV", "Van", "Truck", "Bus", "Motorcycle", "Scooter" and "Other". The system SHALL store the type as a code that does not depend on the
language or the device (`CAR`, `SUV`, `VAN`, `TRUCK`, `BUS`, `MOTORCYCLE`, `SCOOTER`, `OTHER`), and SHALL treat a code it does not
know, such as one written by a newer version of the app, as a vehicle without a type. The names are English for now, like the rest of the app.

#### Scenario: The eight types are offered
- **WHEN** the user opens the type choice on the add vehicle screen
- **THEN** it offers "Car", "SUV", "Van", "Truck", "Bus", "Motorcycle", "Scooter" and "Other", in that order, each with an icon

#### Scenario: The type is stored as a code
- **WHEN** a vehicle of the type "Van" is saved
- **THEN** what is stored for the type is the code `VAN`, whatever the device language

#### Scenario: An unknown stored code
- **WHEN** a vehicle's stored type is a code the app does not know
- **THEN** the vehicle is shown as a vehicle without a type and nothing fails

### Requirement: The type is chosen when a vehicle is added
The system SHALL show the type choice on the add vehicle screen as selectable tiles, each with the type's icon and name, with none
selected at first. The type SHALL be required: when the user tries to save without choosing one, the system SHALL show the error "Choose
the vehicle type" on the choice, stay on the add screen and save nothing, and the error SHALL go away when the user chooses a type.
Choosing another tile SHALL replace the choice.

#### Scenario: Nothing is preselected
- **WHEN** the user opens the add vehicle screen
- **THEN** none of the types is selected

#### Scenario: Choose a type
- **WHEN** the user taps the tile "Motorcycle"
- **THEN** "Motorcycle" is the only selected type

#### Scenario: Change the choice before saving
- **WHEN** the user taps "Motorcycle" and then "Scooter"
- **THEN** "Scooter" is the only selected type and the vehicle is saved with the type "Scooter"

#### Scenario: Saving without a type
- **WHEN** the user enters a name and an odometer and tries to save without choosing a type
- **THEN** the system shows "Choose the vehicle type", stays on the add screen and does not save anything

#### Scenario: The error clears
- **WHEN** the type error is shown and the user taps a type tile
- **THEN** the error is no longer shown

#### Scenario: The choice survives a rotation
- **WHEN** the user chooses a type on the add screen and rotates the device
- **THEN** the same type is still selected

### Requirement: The type can be changed later but not cleared
The system SHALL let the user change a vehicle's type on its edit screen, showing the vehicle's current type as selected and
applying the change when the edit is saved. The system SHALL NOT let the user remove a type: the choice has no "none" option, so a vehicle
that has a type always has one. Changing the type SHALL NOT change the vehicle's odometer, unit, picture or log. Leaving the edit
screen without saving SHALL keep the saved type.

#### Scenario: The current type is selected
- **WHEN** the user opens the edit screen of a vehicle of the type "Truck"
- **THEN** "Truck" is the selected type

#### Scenario: Change the type
- **WHEN** the user edits a vehicle of the type "Car", chooses "Van" and saves
- **THEN** the vehicle has the type "Van" on its details screen and in the vehicle list

#### Scenario: Cancel editing
- **WHEN** the user chooses another type on the edit screen and leaves without saving
- **THEN** the vehicle keeps its saved type

#### Scenario: Only the type changes
- **WHEN** the user changes only the type of a vehicle and saves
- **THEN** the vehicle's name, plate, odometer, unit, picture and log are unchanged

### Requirement: Vehicles that existed before have no type
The system SHALL keep vehicles that were saved before types existed working: they SHALL have no type, be shown with the generic car
icon, and offer the type choice on their edit screen with nothing selected. Saving such a vehicle's edit without choosing a type SHALL be
allowed and SHALL leave it without a type, so no unrelated edit is blocked; choosing a type SHALL give it that type from then on.

#### Scenario: An existing vehicle after the update
- **WHEN** the app is updated and a vehicle that was saved before types existed is shown
- **THEN** it has no type and shows the generic car icon where it has no picture

#### Scenario: Edit without choosing a type
- **WHEN** the user renames a vehicle that has no type, chooses no type and saves
- **THEN** the vehicle is saved with the new name and still has no type

#### Scenario: Give it a type
- **WHEN** the user opens the edit screen of a vehicle that has no type, chooses "Bus" and saves
- **THEN** the vehicle has the type "Bus" and shows the bus icon where it has no picture

### Requirement: The placeholder icon follows the type
Wherever the system shows a vehicle that has no picture (the vehicle list, the vehicle details screen, and the picture preview on the add
and edit screens, and any later picker of vehicles), it SHALL show the icon of the vehicle's type in place of the picture, and SHALL
show the generic car icon when the vehicle has no type. A picture, when the vehicle has one, SHALL be shown instead of the icon. The icon SHALL carry the accessibility
label "Vehicle type: " followed by the type's name (for example "Vehicle type: Van"), and "Vehicle type: none" when there is no type. The icons of the eight types SHALL be
different from one another.

#### Scenario: The list shows the icon of the type
- **WHEN** the vehicle list contains a vehicle of the type "Motorcycle" without a picture
- **THEN** its item shows the motorcycle icon, labelled "Vehicle type: Motorcycle"

#### Scenario: The details screen shows the icon of the type
- **WHEN** the user opens the details screen of a vehicle of the type "Van" without a picture
- **THEN** the screen shows the van icon at the top

#### Scenario: The form preview follows the choice
- **WHEN** the user chooses the type "Truck" on the add screen and has added no picture
- **THEN** the picture preview shows the truck icon

#### Scenario: A picture wins
- **WHEN** a vehicle of the type "Truck" has a picture
- **THEN** the vehicle list and the details screen show the picture and not the truck icon

#### Scenario: No type
- **WHEN** a vehicle has no type and no picture
- **THEN** it shows the generic car icon labelled "Vehicle type: none"

#### Scenario: Removing the picture brings the icon back
- **WHEN** the user removes the picture of a vehicle of the type "Bus" and saves
- **THEN** the vehicle is shown with the bus icon

### Requirement: The type is stored with the vehicle and works offline
The system SHALL keep a vehicle's type on the device with the vehicle, so that it is still there after the app is closed and reopened,
and SHALL offer choosing and changing it without a network connection.

**Platform note:** the behavior is the same on iOS; it is verified on Android now and on iOS once the Xcode project exists.

#### Scenario: The type survives a restart
- **WHEN** the user adds a vehicle of the type "Scooter", closes the app completely and opens it again
- **THEN** the vehicle still has the type "Scooter"

#### Scenario: Offline use
- **WHEN** the device has no network connection and the user adds a vehicle and changes its type
- **THEN** every step succeeds and no network error is shown
