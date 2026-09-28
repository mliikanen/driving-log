# Spec Delta

## ADDED Requirements

### Requirement: The current odometer can be scanned when adding a vehicle
The system SHALL offer a "Scan a reading" action under the add-vehicle form's odometer field that opens the same live
scanner, with the same camera permission and the same photo flow from it, as the log event form's
(`odometer-ocr-capture`). A new vehicle has no known odometer to tell readings apart by size, so on this form the system
SHALL offer, as the vehicle's odometer and labeled "ODO", a number labeled as the odometer, and an unlabeled number with a
distance unit after it whatever its size; it SHALL NOT offer a number labeled as a trip ("TRIP", "TRIP A", "TRIP B" or
"T"), nor an unlabeled number without a unit of 2000 or less (a dial's scale, a clock). Accepting a reading (tapping it in the live scanner, or confirming it on
the photo review) SHALL set the odometer field to its value, SHALL switch the odometer unit to the one with tenths of the
same family (kilometers or miles) when the reading has a tenth, SHALL NOT change the unit's family, and SHALL return to
the add-vehicle form with every other field as it was. Leaving the scanner without accepting a reading SHALL leave the
form unchanged. The edit-vehicle form, whose odometer cannot be changed, SHALL NOT offer the action. On iOS the action
SHALL NOT be shown, as on the log event form.

#### Scenario: Scan the current odometer
- **WHEN** the user adding a vehicle taps "Scan a reading", and taps the reading "ODO 71140" in the scanner
- **THEN** the add-vehicle form's odometer field shows 71140, and the name, plate, type and color are as they were

#### Scenario: An unlabeled small reading is offered as the odometer
- **WHEN** the scanned view shows an unlabeled "120 km" on a new vehicle's dashboard
- **THEN** it is offered as "ODO 120"

#### Scenario: An unlabeled reading without a unit is offered when large
- **WHEN** the scanned view shows an unlabeled "3056" with no unit, and a speedometer dial's "120" and "240"
- **THEN** 3056 is offered as "ODO 3056", and 120 and 240 are not offered

#### Scenario: A trip reading is not offered
- **WHEN** the scanned view shows "TRIP 168.1 Km"
- **THEN** 168.1 is not offered

#### Scenario: A reading with a tenth switches to tenths
- **WHEN** the unit is kilometers and the user accepts a reading of 45200.3
- **THEN** the unit becomes kilometers with 100 m and the field shows 45200.3

#### Scenario: Leave the scanner without a reading
- **WHEN** the user opens the scanner from the add-vehicle form and closes it without accepting a reading
- **THEN** the add-vehicle form is shown exactly as it was

#### Scenario: Not offered when editing
- **WHEN** the user opens the edit-vehicle form
- **THEN** no "Scan a reading" action is shown
