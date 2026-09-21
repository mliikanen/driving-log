# Spec Delta

## MODIFIED Requirements

### Requirement: Add a vehicle
The system SHALL allow the user to add a vehicle by entering a name (required), a license plate (optional), a picture
(optional, as specified in the `vehicle-picture` capability), choosing
the vehicle's odometer unit and entering its current odometer reading in the odometer field (required: the user
SHALL type at least one digit, and a typed 0 is a valid reading). After a successful save the system SHALL return to
the vehicle list showing the new vehicle. The system SHALL allow several vehicles to
share the same name or license plate.

#### Scenario: Add a vehicle with all fields
- **WHEN** the user enters the name "Family car" and the plate "ABC-123", chooses the unit "Kilometers", types the odometer 45200 and saves
- **THEN** the vehicle "Family car" with plate "ABC-123" appears in the vehicle list
- **AND** its details screen shows a current odometer of 45200 km

#### Scenario: Add a vehicle with a picture
- **WHEN** the user enters the name "Family car", adds and crops a picture, types the odometer 45200 and saves
- **THEN** the vehicle "Family car" appears in the vehicle list with its picture, as specified in the `vehicle-picture` capability

#### Scenario: Add a vehicle with a name, the default unit and an odometer of zero
- **WHEN** the user enters the name "Van", leaves the preselected unit unchanged, types 0 as the odometer and saves
- **THEN** the vehicle "Van" appears in the vehicle list without a license plate and with the picture placeholder
- **AND** its details screen shows a current odometer of 0 in the preselected unit

#### Scenario: Odometer is required
- **WHEN** the user enters the name "Van" but types no digit in the odometer field and tries to save
- **THEN** the system shows an error on the odometer field, stays on the add screen and does not save anything

#### Scenario: The odometer error clears when the user types
- **WHEN** the odometer error is shown and the user types a digit in the odometer field
- **THEN** the odometer error is no longer shown

#### Scenario: Name and odometer are both missing
- **WHEN** the user tries to save with an empty name and no odometer digits
- **THEN** the system shows the error on the name field and the error on the odometer field, and does not save anything

#### Scenario: Name is required
- **WHEN** the user tries to save a vehicle with an empty name
- **THEN** the system shows an error on the name field, stays on the add screen and does not save anything

#### Scenario: Duplicate names are allowed
- **WHEN** the user adds a vehicle named "Van" while another vehicle named "Van" already exists
- **THEN** both vehicles appear in the vehicle list

#### Scenario: Cancel adding
- **WHEN** the user leaves the add screen without saving
- **THEN** no vehicle is added

### Requirement: Vehicle list
The system SHALL show every vehicle the user has added on the Home screen, ordered by name without regard to letter
case, and SHALL show each vehicle's small picture (or the placeholder when it has none), its name and, when it has one, its license plate. When there are no vehicles the
system SHALL show an empty state that invites the user to add a vehicle. The Home screen SHALL offer an action to
add a vehicle.

#### Scenario: Vehicles are listed alphabetically
- **WHEN** the user has vehicles named "van", "Bike" and "Family car"
- **THEN** the vehicle list shows them in the order "Bike", "Family car", "van"

#### Scenario: Vehicles show their picture
- **WHEN** the user has a vehicle with a picture and a vehicle without one
- **THEN** the list shows the small picture for the first and the placeholder for the second

#### Scenario: Empty vehicle list
- **WHEN** the user has not added any vehicle
- **THEN** the Home screen shows an empty state and the action to add a vehicle

#### Scenario: Open the add screen
- **WHEN** the user taps the add vehicle action on the Home screen
- **THEN** the add vehicle screen is displayed

### Requirement: Vehicle details screen
The system SHALL show a details screen when the user selects a vehicle in the list. The details screen SHALL show the
vehicle's large picture at the top (or the placeholder when it has none), its name, its license plate when it has one, its current odometer in the vehicle's unit, its most recent log events as
specified in the `vehicle-log` capability, and actions to log a distance, to open the full log and to edit the vehicle.
Going back SHALL return to the vehicle list.

#### Scenario: Open a vehicle
- **WHEN** the user taps "Family car" in the vehicle list
- **THEN** the details screen shows the name "Family car", the plate, the current odometer and the recent log events

#### Scenario: The picture is shown
- **WHEN** the user opens the details screen of a vehicle with a picture
- **THEN** the screen shows the large picture above the name

#### Scenario: Open the log distance form
- **WHEN** the user taps "Log distance" on the details screen
- **THEN** the log distance form is displayed, as specified in the `distance-logging` capability

#### Scenario: Back to the list
- **WHEN** the user navigates back from the details screen
- **THEN** the vehicle list is displayed

### Requirement: Edit a vehicle
The system SHALL allow the user to change a vehicle's name, license plate and picture (as specified in the `vehicle-picture` capability) from its details screen. The edit screen
SHALL start with the current values, SHALL apply the same trimming and name validation as adding a vehicle, and SHALL
NOT allow changing the odometer reading or the odometer unit. After a successful save the system SHALL return to the details screen showing
the new values, and the vehicle list SHALL show them too.

#### Scenario: Change name and plate
- **WHEN** the user edits "Family car" to the name " Estate car " and the plate "XYZ-789 " and saves
- **THEN** the details screen shows the name "Estate car" and the plate "XYZ-789"
- **AND** the vehicle list shows "Estate car" with the plate "XYZ-789"

#### Scenario: Change the picture
- **WHEN** the user chooses and crops another picture on the edit screen and saves
- **THEN** the details screen and the vehicle list show the new picture

#### Scenario: Remove the plate
- **WHEN** the user clears the license plate of a vehicle and saves
- **THEN** the vehicle no longer shows a license plate

#### Scenario: Name cannot be emptied
- **WHEN** the user clears the name, or replaces it with only spaces, and saves
- **THEN** the system shows the name error, stays on the edit screen and keeps the previously saved values

#### Scenario: Odometer and unit are not editable
- **WHEN** the user opens the edit screen of a vehicle
- **THEN** the screen offers only the name, the license plate and the picture for editing

#### Scenario: Cancel editing
- **WHEN** the user changes the fields and then leaves the edit screen without saving
- **THEN** the vehicle keeps its previously saved name, plate and picture
