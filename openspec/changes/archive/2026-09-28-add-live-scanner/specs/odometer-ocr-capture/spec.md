# Spec Delta

## MODIFIED Requirements

### Requirement: A photo is chosen to scan for a reading
The system SHALL offer a "Scan a reading" action on the log event form. Tapping it SHALL open the live scanner ("The
live scanner shows readings as the camera sees them"). The live scanner SHALL offer a photo action that opens the same
system photo chooser already used for a vehicle's picture (including the device's camera app, through the system's own
intent or source sheet — no permission of this application's own is requested for it). Choosing a photo SHALL proceed
to detection and the photo review; cancelling the chooser SHALL return to the live scanner, and leaving the photo review
without accepting a candidate SHALL return to the live scanner too. On iOS, which has no on-device recognizer yet, the
action SHALL NOT be shown.

#### Scenario: Open the scanner
- **WHEN** the user taps "Scan a reading" on the log event form
- **THEN** the live scanner opens

#### Scenario: Open the chooser
- **WHEN** the user taps the live scanner's photo action
- **THEN** the system's photo chooser (including the camera) opens

#### Scenario: No scan on iOS
- **WHEN** the user opens the log event form on iOS
- **THEN** no "Scan a reading" action is shown

#### Scenario: Cancel the chooser
- **WHEN** the user opens the chooser from the live scanner and cancels it without choosing a photo
- **THEN** the live scanner is shown again

#### Scenario: Leave the photo review
- **WHEN** the user chose a photo and leaves its review without accepting a candidate
- **THEN** the live scanner is shown again

### Requirement: Accepting a candidate sets the field and the way
The system SHALL, when the user confirms a selected candidate on the photo review, or taps a reading in the live
scanner, set the log event form's active numeric field to the candidate's value, and switch the form's way to "New
odometer" for an odometer-like candidate or "Trip distance" for a trip-meter-like one (`distance-logging`, "A distance
is logged as a trip distance or as a new odometer count"), replacing whatever the field and way held before, and SHALL
return to the log event form.

#### Scenario: Accept an odometer reading
- **WHEN** the user confirms a candidate classified as an odometer reading
- **THEN** the form switches to "New odometer" and the field shows the candidate's value

#### Scenario: Accept a trip reading
- **WHEN** the user confirms a candidate classified as a trip reading
- **THEN** the form switches to "Trip distance" and the field shows the candidate's value

#### Scenario: Tap a live odometer reading
- **WHEN** the user taps a reading classified as an odometer reading in the live scanner
- **THEN** the scanner closes, the form switches to "New odometer" and the field shows the reading's value

### Requirement: An accepted scan's photo and detections are kept only if the entry is saved
The system SHALL keep the scanned photo and the full detection result (every candidate found: its position in the
photo, its recognized text and its classification, not only the accepted one) once the user accepts a candidate, and
SHALL store them under the resulting event only once the log event form is saved. For a reading tapped in the live
scanner, the photo SHALL be the camera frame the reading was detected in, and the detection result that frame's.
Leaving the form without saving, after accepting a scan, SHALL discard the kept photo and detections along with the
rest of the unsaved entry. Scanning again before saving SHALL keep only the latest accepted scan. Neither the photo nor
the detections SHALL appear in any user-facing screen; they exist only to review a misdetection after the fact. Once
stored, the photo and detections SHALL be kept until they themselves are removed: removing the event they belong to
SHALL NOT remove them.

#### Scenario: Saved together with the event
- **WHEN** the user accepts a candidate and saves the entry
- **THEN** the photo and its full detection result are stored, linked to the new event

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

## ADDED Requirements

### Requirement: The live scanner shows readings as the camera sees them
The system SHALL show, in the live scanner, the back camera's live preview filling the screen, and SHALL draw over it a
box around every candidate reading detected in the recent camera frames, with its classification and value as text
("ODO 71140", "TRIP 168.1"), detected offline and on the device, and classified by the same rules as a scanned photo's
candidates. The boxes SHALL follow the view as it changes, and a reading SHALL stay shown briefly after the camera loses
it rather than flicker on and off between frames. Tapping a reading's box or text SHALL accept it ("Accepting a
candidate sets the field and the way"). The live scanner SHALL offer a close action, and it and back navigation SHALL
leave the scanner without accepting anything: the log event form is then unchanged and nothing is kept.

#### Scenario: Readings are boxed live
- **WHEN** the camera is pointed at a dashboard showing "ODO 71140 km"
- **THEN** 71140 is boxed on the preview with the text "ODO 71140"

#### Scenario: Leave without picking
- **WHEN** the user closes the live scanner or navigates back without tapping a reading
- **THEN** the log event form is shown exactly as it was, and nothing is kept

#### Scenario: Nothing readable in view
- **WHEN** no candidate reading is detected in the camera's view
- **THEN** the preview is shown without boxes, and the close and photo actions remain available

### Requirement: The camera permission is asked for when the scanner is first opened
The system SHALL ask for the camera permission when the user taps "Scan a reading" and it has not been granted, and not
before. When it is refused, the live scanner SHALL open without a preview, SHALL say that the live scanner needs camera
access and that it can be allowed in the device settings, and SHALL keep its photo and close actions working. When it is
granted later (in the device settings, or by being asked again), the next time the scanner opens it SHALL show the
preview. On iOS the scan action is not offered, so none of this applies there.

#### Scenario: Asked on first use
- **WHEN** the user taps "Scan a reading" and has never been asked for the camera
- **THEN** the system asks for the camera permission

#### Scenario: Refused
- **WHEN** the user refuses the camera permission
- **THEN** the live scanner opens without a preview, says it needs camera access and that it can be allowed in the device settings, and its photo action still opens the system's photo chooser

#### Scenario: Granted
- **WHEN** the user grants the camera permission
- **THEN** the live scanner shows the camera's preview
