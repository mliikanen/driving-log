# Spec Delta

## MODIFIED Requirements

### Requirement: Add a vehicle
The system SHALL allow the user to add a vehicle by entering a name (required), a license plate (optional), a picture
(optional, as specified in the `vehicle-picture` capability), the vehicle's type (preselected as Car, changeable, as specified in the `vehicle-type`
capability), the vehicle's color (preselected as the application's main theme color, changeable, as specified in the `vehicle-color`
capability), choosing
the vehicle's odometer unit and entering its current odometer reading in the odometer field (required: the user
SHALL type at least one digit, and a typed 0 is a valid reading). After a successful save the system SHALL return to
the vehicle list showing the new vehicle. The system SHALL allow several vehicles to
share the same name or license plate.

#### Scenario: Add a vehicle with all fields
- **WHEN** the user enters the name "Family car" and the plate "ABC-123", chooses the type "Car" and the unit "Kilometers", types the odometer 45200 and saves
- **THEN** the vehicle "Family car" with plate "ABC-123" appears in the vehicle list
- **AND** its details screen shows a current odometer of 45200 km

#### Scenario: Add a vehicle with a color
- **WHEN** the user enters the name "Family car", chooses the color "Teal", types the odometer 45200 and saves
- **THEN** the vehicle "Family car" is saved with the color "Teal", as specified in the `vehicle-color` capability

#### Scenario: Add a vehicle with a picture
- **WHEN** the user enters the name "Family car", chooses the type "Car", adds and crops a picture, types the odometer 45200 and saves
- **THEN** the vehicle "Family car" appears in the vehicle list with its picture, as specified in the `vehicle-picture` capability

#### Scenario: Add a vehicle with a name, the default unit and an odometer of zero
- **WHEN** the user enters the name "Van", chooses the type "Van", leaves the preselected unit unchanged, types 0 as the odometer and saves
- **THEN** the vehicle "Van" appears in the vehicle list without a license plate, with the placeholder of its type and with the default color
- **AND** its details screen shows a current odometer of 0 in the preselected unit

#### Scenario: Odometer is required
- **WHEN** the user enters the name "Van", chooses the type "Van" but types no digit in the odometer field and tries to save
- **THEN** the system shows an error on the odometer field, stays on the add screen and does not save anything

#### Scenario: The odometer error clears when the user types
- **WHEN** the odometer error is shown and the user types a digit in the odometer field
- **THEN** the odometer error is no longer shown

#### Scenario: Name and odometer are both missing
- **WHEN** the user tries to save with an empty name and no odometer digits
- **THEN** the system shows the error on the name field and the error on the odometer field, and does not save anything

#### Scenario: Name is required
- **WHEN** the user chooses a type and an odometer but tries to save a vehicle with an empty name
- **THEN** the system shows an error on the name field, stays on the add screen and does not save anything

#### Scenario: Duplicate names are allowed
- **WHEN** the user adds a vehicle named "Van", choosing a type, while another vehicle named "Van" already exists
- **THEN** both vehicles appear in the vehicle list

#### Scenario: Cancel adding
- **WHEN** the user leaves the add screen without saving
- **THEN** no vehicle is added

### Requirement: Edit a vehicle
The system SHALL allow the user to change a vehicle's name, license plate, type (as specified in the `vehicle-type` capability), color (as specified in the `vehicle-color` capability), picture (as specified in the `vehicle-picture` capability) from its details screen. The edit screen
SHALL start with the current values, SHALL apply the same trimming and name validation as adding a vehicle, and SHALL
NOT allow changing the odometer reading or the odometer unit. After a successful save the system SHALL return to the details screen showing
the new values, and the vehicle list SHALL show them too.

#### Scenario: Change name and plate
- **WHEN** the user edits "Family car" to the name " Estate car " and the plate "XYZ-789 " and saves
- **THEN** the details screen shows the name "Estate car" and the plate "XYZ-789"
- **AND** the vehicle list shows "Estate car" with the plate "XYZ-789"

#### Scenario: Change the type
- **WHEN** the user edits a vehicle of the type "Car", chooses the type "Van" and saves
- **THEN** the vehicle has the type "Van", as specified in the `vehicle-type` capability

#### Scenario: Change the color
- **WHEN** the user edits a vehicle, chooses the color "Red" and saves
- **THEN** the vehicle has the color "Red", as specified in the `vehicle-color` capability

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
- **THEN** the screen offers only the name, the license plate, the type, the color and the picture for editing

#### Scenario: Cancel editing
- **WHEN** the user changes the fields and then leaves the edit screen without saving
- **THEN** the vehicle keeps its previously saved name, plate, type, color and picture
