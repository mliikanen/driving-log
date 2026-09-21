# Spec Delta

## MODIFIED Requirements

### Requirement: Edit a vehicle
The system SHALL allow the user to change a vehicle's name, license plate, type (as specified in the `vehicle-type` capability), picture (as specified in the `vehicle-picture` capability) and, when it has no picture, its main color (as specified in the `vehicle-palette` capability) from its details screen. The edit screen
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

#### Scenario: Change the picture
- **WHEN** the user chooses and crops another picture on the edit screen and saves
- **THEN** the details screen and the vehicle list show the new picture

#### Scenario: Choose a main color
- **WHEN** the user edits a vehicle that has no picture, chooses the main color #1E88E5 and saves
- **THEN** the vehicle has the main color #1E88E5 and a palette derived from it, as specified in the `vehicle-palette` capability

#### Scenario: Remove the plate
- **WHEN** the user clears the license plate of a vehicle and saves
- **THEN** the vehicle no longer shows a license plate

#### Scenario: Name cannot be emptied
- **WHEN** the user clears the name, or replaces it with only spaces, and saves
- **THEN** the system shows the name error, stays on the edit screen and keeps the previously saved values

#### Scenario: Odometer and unit are not editable
- **WHEN** the user opens the edit screen of a vehicle
- **THEN** the screen offers only the name, the license plate, the type, the picture and, when there is no picture, the main color for editing

#### Scenario: Cancel editing
- **WHEN** the user changes the fields and then leaves the edit screen without saving
- **THEN** the vehicle keeps its previously saved name, plate, type, picture and main color
