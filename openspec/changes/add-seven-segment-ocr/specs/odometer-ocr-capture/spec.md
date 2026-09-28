# Spec Delta

## ADDED Requirements

### Requirement: Readings in seven-segment digits are detected
The system SHALL detect, in the scanned photo, numeric readings shown in seven-segment (LCD) digits as well as those
shown in ordinary printed digits, both offline and on the device, and SHALL treat a reading detected either way the
same: it SHALL become a candidate by the same rules, be classified by the same label and magnitude rules, and be
presented for review in the same way. A reading SHALL be presented once even when both kinds of recognition find it.
On iOS the scan action is not offered (`add-odometer-ocr-capture`), so none of this applies there.

#### Scenario: An LCD odometer reading is found
- **WHEN** the user scans a photo of a motorcycle's LCD display showing "ODO 5034 Km"
- **THEN** 5034 is presented as a candidate classified as an odometer reading

#### Scenario: An LCD trip reading is found
- **WHEN** the user scans a photo of an LCD display showing "TRIP 168.1 Km"
- **THEN** 168.1 is presented as a candidate classified as a trip reading

#### Scenario: A printed reading is still found
- **WHEN** the user scans a photo of a car cluster showing "ODO 71140 km" in ordinary digits
- **THEN** 71140 is presented as a candidate classified as an odometer reading, exactly as without seven-segment detection

#### Scenario: One reading, one candidate
- **WHEN** a reading is found both as printed digits and as seven-segment digits
- **THEN** it is presented as one candidate, not two

### Requirement: The user can mark where the reading is
The system SHALL offer a "Mark the reading" action on the candidate review screen and on the "no reading found" state.
It SHALL let the user drag a box over the photo; once the user confirms the box, the system SHALL look for candidates
only inside it and SHALL present those for review, classified as usual, in place of the candidates shown before.
When the box holds no plausible reading, the system SHALL say so and let the user mark again, choose another photo or
leave. Leaving the marking without confirming a box SHALL return to what was shown before, unchanged.

#### Scenario: Marking finds a reading the automatic pass missed
- **WHEN** the automatic pass found no reading and the user marks a box over "ODO 5368 Km" on the photo and confirms it
- **THEN** 5368 is presented as a candidate classified as an odometer reading

#### Scenario: Marking replaces the candidates shown
- **WHEN** candidates were shown and the user marks a box over another part of the photo and confirms it
- **THEN** only the candidates inside the marked box are shown

#### Scenario: Nothing in the marked box
- **WHEN** the user marks a box that holds no plausible reading and confirms it
- **THEN** the system says no reading was found in the marked area, and the user can mark again, choose another photo or leave

#### Scenario: Cancel marking
- **WHEN** the user starts marking and leaves without confirming a box
- **THEN** the review screen shows what it showed before marking, unchanged

### Requirement: A marked region is kept with the detections
The system SHALL, when the accepted candidate came from a marked box, keep that box (its position in the photo) as
part of the scan's detection result, stored and discarded together with the photo and the rest of the detections as
`add-odometer-ocr-capture` defines. Like the rest of the detections, the box SHALL NOT appear in any user-facing
screen.

#### Scenario: The marked box is saved with the entry
- **WHEN** the user marks a box, accepts a candidate found in it and saves the entry
- **THEN** the stored detection result includes the marked box

#### Scenario: No marked box without marking
- **WHEN** the user accepts a candidate from the automatic pass, without marking, and saves the entry
- **THEN** the stored detection result has no marked box
