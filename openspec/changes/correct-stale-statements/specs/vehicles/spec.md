## MODIFIED Requirements

### Requirement: Add a vehicle
The system SHALL let the user add a vehicle with a name (required), an odometer unit, the current odometer reading in
the odometer field (required: at least one digit; a typed 0 is valid), a license plate (optional), a type (preselected
as Car, `vehicle-type`), a fuel type (preselected as Petrol, `vehicle-fuel-type`) and a color (preselected as the main theme color, `vehicle-color`). After a successful save
the system SHALL return to the screen the form was opened from: the vehicle list, which shows the new vehicle, or
the Home screen when it was opened from there. Several vehicles SHALL be allowed to share a name
or a license plate.

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
- **WHEN** the user enters the name "Van", chooses the type "Van" but types no digit in the odometer field
- **THEN** the add screen's Save action is disabled

#### Scenario: The odometer error clears when the user types
- **WHEN** Save is disabled because the odometer field is empty and the user types a digit
- **THEN** Save becomes enabled, provided the name is filled in too

#### Scenario: Name and odometer are both missing
- **WHEN** the user has typed no name and no odometer digits
- **THEN** the add screen's Save action is disabled

#### Scenario: Name is required
- **WHEN** the user chooses a type and types an odometer but has entered no name
- **THEN** the add screen's Save action is disabled

#### Scenario: Duplicate names are allowed
- **WHEN** the user adds a vehicle named "Van", choosing a type, while another vehicle named "Van" already exists
- **THEN** both vehicles appear in the vehicle list

#### Scenario: Cancel adding
- **WHEN** the user leaves the add screen without saving
- **THEN** no vehicle is added

#### Scenario: The dismiss action is a close icon
- **WHEN** the user opens the add-vehicle screen
- **THEN** the top-left action of the screen's top app bar is a close "X", not a back arrow

#### Scenario: Required fields are marked
- **WHEN** the user opens the add-vehicle form
- **THEN** the name and odometer labels each carry a trailing "*", the license plate, type, fuel type and color fields do not, and a line near the form's actions reads "* indicates a required field"

#### Scenario: The required fields come before type and color
- **WHEN** the user opens the add-vehicle form
- **THEN** the fields appear in the order picture, name, license plate, unit, odometer, type, fuel type, color

### Requirement: The add vehicle form's fields come in a fixed order
The add vehicle form SHALL present its fields in this order: picture, name, license plate, unit, odometer, type,
fuel type, color. The two required fields (name, odometer) are grouped with the identity fields ahead of type, fuel
type and color, which
always have a default and can never be left empty.

#### Scenario: Color is the last field
- **WHEN** the user scrolls the add vehicle form to its end
- **THEN** the color choice is the last field
