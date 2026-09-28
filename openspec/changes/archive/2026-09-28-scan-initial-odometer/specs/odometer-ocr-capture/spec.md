# Spec Delta

## ADDED Requirements

### Requirement: A scan accepted when adding a vehicle is kept with its initial odometer
The system SHALL keep the photo (or, from the live scanner, the camera frame) and the full detection result of a reading
accepted on the add-vehicle form, and SHALL store them with the vehicle's initial odometer event when the vehicle is
saved, under the same rules as a scan accepted on the log event form: leaving the add-vehicle form without saving SHALL
discard them, scanning again before saving SHALL keep only the latest accepted scan, they SHALL NOT appear in any
user-facing screen, and removing the event SHALL NOT remove them.

#### Scenario: Saved with the new vehicle
- **WHEN** the user accepts a scanned reading on the add-vehicle form and saves the vehicle
- **THEN** the photo or frame and its full detection result are stored, linked to the vehicle's initial odometer event

#### Scenario: Discarded when the vehicle is not added
- **WHEN** the user accepts a scanned reading on the add-vehicle form and leaves it without saving
- **THEN** neither the photo nor its detections are stored anywhere

#### Scenario: A typed odometer keeps no scan
- **WHEN** the user types the odometer by hand and saves the vehicle
- **THEN** no scan is stored with its initial odometer event
