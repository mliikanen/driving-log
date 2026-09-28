# Spec Delta

## Purpose

Lets a user fill the log event form's odometer or trip-distance field from a photo instead of typing: a photo is
scanned offline for numeric readings, each is classified as odometer- or trip-meter-like, and the user picks which
one (if any) to accept. The photo and every detection are kept privately for debugging, never shown to the user.

## ADDED Requirements

### Requirement: A photo is chosen to scan for a reading
The system SHALL offer a "Scan a reading" action on the log event form. Tapping it SHALL open the same system photo
chooser already used for a vehicle's picture (including the device's camera app, through the system's own intent or
source sheet — no permission of this application's own is requested for it). Choosing a photo SHALL proceed to
detection; cancelling the chooser SHALL leave the log event form exactly as it was. On iOS, which has no on-device
recognizer yet, the action SHALL NOT be shown.

#### Scenario: Open the chooser
- **WHEN** the user taps "Scan a reading" on the log event form
- **THEN** the system's photo chooser (including the camera) opens

#### Scenario: No scan on iOS
- **WHEN** the user opens the log event form on iOS
- **THEN** no "Scan a reading" action is shown

#### Scenario: Cancel the chooser
- **WHEN** the user opens the chooser and cancels it without choosing a photo
- **THEN** the log event form is shown exactly as it was, with nothing changed

### Requirement: Numeric readings are detected offline in the chosen photo
The system SHALL recognize text in the chosen photo using on-device OCR that works without a network connection, and
SHALL identify, among the recognized text, every numeric reading plausible as an odometer or trip-meter value as a
candidate. A photo with no plausible candidate SHALL say so and let the user choose another photo or leave.

#### Scenario: Offline detection
- **WHEN** the device has no network connection and the user scans a photo
- **THEN** candidates are detected and no network error is shown

#### Scenario: No plausible reading
- **WHEN** the user scans a photo with no numeric reading plausible as an odometer or trip-meter value
- **THEN** the system says no reading was found, and the user can choose another photo or leave

### Requirement: A detected reading is classified as an odometer or a trip-meter reading
The system SHALL classify each candidate as odometer-like or trip-meter-like using, in order: a recognized text label
next to it in the photo ("ODO", "ODOMETER" or "TOTAL DISTANCE" read as odometer-like, "TRIP", "TRIP A", "TRIP B"
or "T" as trip-like), and, when no such label is recognized next to it, the candidate's magnitude against the vehicle's
current known odometer at the entry's date and time (already defined by `distance-logging`, "The previous known
odometer"): a value at or plausibly above the known odometer reads as odometer-like, and a value clearly smaller,
plausible as a trip distance, reads as trip-meter-like. A candidate with neither a recognized label nor a plausible
magnitude either way SHALL NOT be presented to the user.

#### Scenario: Classified by label
- **WHEN** a candidate has the recognized text "ODO" next to it in the photo
- **THEN** it is classified as an odometer reading, regardless of its magnitude

#### Scenario: Classified by a label below it
- **WHEN** a candidate has the recognized text "Total distance" directly below it in the photo
- **THEN** it is classified as an odometer reading

#### Scenario: Classified by magnitude when there is no label
- **WHEN** a candidate has no recognized odometer/trip label next to it, and its value is close to the vehicle's current known odometer
- **THEN** it is classified as an odometer reading

#### Scenario: A small unlabeled value reads as a trip
- **WHEN** a candidate has no recognized label next to it, and its value is much smaller than the vehicle's current known odometer, in the range a trip distance would plausibly be
- **THEN** it is classified as a trip reading

#### Scenario: An implausible reading is not shown
- **WHEN** a numeric value recognized elsewhere in the photo (for example a clock or a temperature) has no odometer/trip label and is neither close to the known odometer nor plausible as a trip distance
- **THEN** it is not presented as a candidate

### Requirement: The user picks a candidate to accept, or leaves without picking one
The system SHALL show the chosen photo with a box drawn around every candidate and its classification ("ODO" or
"TRIP") shown as text next to the box, every box and its text in a neutral color until the user taps it, and in a
distinct, confirmable color once tapped. Tapping a candidate SHALL select it in place of whichever was selected
before, if any. The system SHALL let the user confirm the selected candidate, or navigate back without confirming
one, in which case the log event form is unchanged and nothing is kept.

#### Scenario: One candidate
- **WHEN** the photo has exactly one candidate
- **THEN** it is shown as one box with its classification text, in the neutral color

#### Scenario: Several candidates
- **WHEN** the photo has more than one candidate
- **THEN** each is shown with its own box and classification text

#### Scenario: Selecting a candidate
- **WHEN** the user taps a candidate's box or text
- **THEN** that candidate changes to the selected color and any previously selected candidate returns to the neutral color

#### Scenario: Leaving without accepting
- **WHEN** the user navigates back without confirming a selected candidate
- **THEN** the log event form is shown exactly as it was, and nothing is kept

### Requirement: Accepting a candidate sets the field and the way
The system SHALL, when the user confirms a selected candidate, set the log event form's active numeric field to the
candidate's value, and switch the form's way to "New odometer" for an odometer-like candidate or "Trip distance" for
a trip-meter-like one (`distance-logging`, "A distance is logged as a trip distance or as a new odometer count"),
replacing whatever the field and way held before.

#### Scenario: Accept an odometer reading
- **WHEN** the user confirms a candidate classified as an odometer reading
- **THEN** the form switches to "New odometer" and the field shows the candidate's value

#### Scenario: Accept a trip reading
- **WHEN** the user confirms a candidate classified as a trip reading
- **THEN** the form switches to "Trip distance" and the field shows the candidate's value

### Requirement: An accepted scan's photo and detections are kept only if the entry is saved
The system SHALL keep the scanned photo and the full detection result (every candidate found: its position in the
photo, its recognized text and its classification, not only the accepted one) once the user accepts a candidate, and
SHALL store them under the resulting event only once the log event form is saved. Leaving the form without saving,
after accepting a scan, SHALL discard the kept photo and detections along with the rest of the unsaved entry.
Scanning again before saving SHALL keep only the latest accepted scan. Neither the photo nor the detections SHALL
appear in any user-facing screen; they exist only to review a misdetection after the fact. Once stored, the photo and
detections SHALL be kept until they themselves are removed: removing the event they belong to SHALL NOT remove them.

#### Scenario: Saved together with the event
- **WHEN** the user accepts a candidate and saves the entry
- **THEN** the photo and its full detection result are stored, linked to the new event

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
