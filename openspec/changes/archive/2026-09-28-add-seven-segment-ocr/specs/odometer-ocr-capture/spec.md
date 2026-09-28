# Spec Delta

## ADDED Requirements

### Requirement: Readings in seven-segment digits are detected
The system SHALL detect, in the scanned photo, numeric readings shown in seven-segment (LCD) digits as well as those
shown in ordinary printed digits, both offline and on the device, and SHALL treat a reading detected either way the
same: it SHALL become a candidate by the same rules, be classified by the same label and magnitude rules, and be
presented for review in the same way. A reading SHALL be presented once even when both kinds of recognition find it.
On iOS the scan action is not offered (`odometer-ocr-capture`, "A photo is chosen to scan for a reading"), so none of
this applies there.

#### Scenario: An LCD odometer reading is found
- **WHEN** the user scans a photo of a motorcycle's LCD display showing "ODO 5034 Km"
- **THEN** 5034 is presented as a candidate classified as an odometer reading

#### Scenario: An LCD trip reading is found
- **WHEN** the user scans a photo of an LCD display showing "TRIP 168.1 Km"
- **THEN** 168.1 is presented as a candidate classified as a trip reading

#### Scenario: A printed reading is still found
- **WHEN** the user scans a photo of a car cluster showing "ODO 71140 km" in ordinary digits
- **THEN** 71140 is presented as a candidate classified as an odometer reading, as before

#### Scenario: One reading, one candidate
- **WHEN** a reading is found both as printed text and as seven-segment digits
- **THEN** it is presented as one candidate, not two
