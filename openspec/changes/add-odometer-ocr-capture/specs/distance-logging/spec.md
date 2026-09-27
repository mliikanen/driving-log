# Spec Delta

## MODIFIED Requirements

### Requirement: A distance is logged as a trip distance or as a new odometer count
The system SHALL let the user choose, on the log event form, between "Trip distance" and "New odometer". "Trip distance"
SHALL be preselected. With "Trip distance" the number entered is the distance travelled. With "New odometer" the number
entered is the odometer count now, and the distance logged is that count minus the previous known odometer at the entry's
date and time (or, when no odometer is known at that time, an odometer anchor is saved instead). Each way SHALL keep its own typed number when the user switches between them. Accepting a candidate from the form's "Scan a reading" action
(`odometer-ocr-capture`, "Accepting a candidate sets the field and the way") SHALL set the active field's number and
SHALL switch the way to match the candidate's classification, the same as if the user had switched by hand and typed it.

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

#### Scenario: Accepting a scanned odometer reading switches the way
- **WHEN** the form is on "Trip distance" and the user scans a photo, and accepts a candidate classified as an odometer reading
- **THEN** the form switches to "New odometer" and the field shows the accepted value

#### Scenario: Accepting a scanned trip reading switches the way
- **WHEN** the form is on "New odometer" and the user scans a photo, and accepts a candidate classified as a trip reading
- **THEN** the form switches to "Trip distance" and the field shows the accepted value
