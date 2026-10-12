# Spec Delta

## MODIFIED Requirements

### Requirement: An accepted scan's photo and detections are kept only if the entry is saved
The system SHALL keep the scanned photo and the full detection result (every candidate found: its position in the
photo, its recognized text and its classification, not only the accepted one) once the user accepts a candidate, and
SHALL store them under the resulting event only once the log event form is saved. When the form saves more than one
event (a refueling with mileage, `vehicle-log`'s "The log stores one fact per event, grouped into entries"), the
scan SHALL be stored under the event whose number it filled: a fuel-amount scan under the "Refueling" event, a
mileage scan under the "Distance" or "Odometer reading" event. A mileage scan whose mileage was cleared before the
refueling was saved filled nothing that is saved, and SHALL NOT be stored. For a reading tapped in the live scanner, the photo SHALL be the camera frame the reading was detected in, and the detection result that frame's.
Leaving the form without saving, after accepting a scan, SHALL discard the kept photo and detections along with the
rest of the unsaved entry. Scanning again before saving SHALL keep only the latest accepted scan. Neither the photo nor
the detections SHALL appear in any user-facing screen; they exist only to review a misdetection after the fact. Once
stored, the photo and detections SHALL be kept until they themselves are removed: removing the event they belong to
SHALL NOT remove them.

#### Scenario: Saved together with the event
- **WHEN** the user accepts a candidate and saves the entry
- **THEN** the photo and its full detection result are stored, linked to the new event

#### Scenario: A fuel-amount scan is linked to the refueling event
- **WHEN** the user accepts a scanned fuel amount, types a trip distance and saves the refueling
- **THEN** the photo and its detections are linked to the new "Refueling" event, not to the "Distance" event

#### Scenario: A mileage scan is linked to the mileage event
- **WHEN** the user types a fuel amount, accepts a scanned odometer reading for the mileage and saves the refueling
- **THEN** the photo and its detections are linked to the entry's "Distance" or "Odometer reading" event, not to the "Refueling" event

#### Scenario: A cleared mileage scan is not kept
- **WHEN** the user accepts a scanned odometer reading for a refueling's mileage, clears the mileage field, types a fuel amount and saves
- **THEN** the refueling is saved with no mileage, and neither the photo nor its detections are stored

#### Scenario: A live reading keeps its frame
- **WHEN** the user taps a reading in the live scanner and saves the entry
- **THEN** the camera frame it was detected in and that frame's full detection result are stored, linked to the new event

#### Scenario: Discarded when the form is abandoned
- **WHEN** the user accepts a candidate and then leaves the form without saving
- **THEN** neither the photo nor its detections are stored anywhere

#### Scenario: A second scan replaces the first
- **WHEN** the user accepts a candidate from one scan, then scans again and accepts a candidate from a second photo, then saves
- **THEN** only the second photo and its detections are stored

#### Scenario: Kept when the event is removed
- **WHEN** an event saved from an accepted scan is removed
- **THEN** its photo and detections are still stored

#### Scenario: Not shown to the user
- **WHEN** an event saved from an accepted scan is viewed anywhere in the application (its row, its details screen, the full log)
- **THEN** neither the photo nor any detection is shown
