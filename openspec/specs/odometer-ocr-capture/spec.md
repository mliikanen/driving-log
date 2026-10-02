# odometer-ocr-capture Specification

## Purpose

Lets a user fill the log event form's odometer or trip-distance field from a photo instead of typing: a photo is
scanned offline for numeric readings, each is classified as odometer- or trip-meter-like, and the user picks which
one (if any) to accept. The photo and every detection are kept privately for debugging, never shown to the user.

## Requirements

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

### Requirement: A photo is chosen to scan a refueling's fuel amount
The system SHALL offer a "Scan a reading" action next to the log event form's fuel amount field (shown when
"Refueling" is chosen in the Kind selector), alongside the mileage section's own, unchanged one ("A photo is chosen
to scan for a reading"). Tapping it SHALL open the live scanner, under the same rules as the mileage section's
action: the same photo chooser, the same cancel/leave behavior, and not shown on iOS.

#### Scenario: Open the scanner for the fuel amount
- **WHEN** the user taps "Scan a reading" next to the log event form's fuel amount field
- **THEN** the live scanner opens

#### Scenario: Leaving without accepting changes nothing
- **WHEN** the user opens the live scanner from the fuel amount field's action and leaves without accepting a candidate
- **THEN** the log event form is shown exactly as it was, and nothing is kept

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

### Requirement: A detected reading is classified as a fuel amount, by label only
The system SHALL classify a candidate as a fuel-amount reading when a recognized text label for a volume is next to
it in the photo ("LITRAA", "LITARA", "LITROV", "dm³", "L", "LITERS", "LITRES", "GAL", "GALLON" or "GALLONS"), using
the same label-adjacency rule odometer/trip classification uses ("A detected reading is classified as an odometer or
a trip-meter reading"). Unlike an odometer or trip reading, a fuel amount SHALL NOT be classified by magnitude: a
fuel pump or receipt display typically shows the amount dispensed and its total price as two numbers of similar size
with nothing but their labels to tell them apart, so there is no magnitude fallback and no plausible-range check. A
candidate with no recognized volume label next to it SHALL NOT be presented as a fuel-amount candidate. On a display
where a price or an amount sits as close to the one recognized volume label as the real reading does, that number
MAY also be classified as a fuel-amount candidate alongside it — the same adjacency rule that finds the real reading
does not distinguish them by what else they mean, only by which recognized label is nearest; this capability's own
"Several candidates" is how the user resolves this.

#### Scenario: Classified by a volume label
- **WHEN** a candidate has the recognized text "LITRAA" or "GAL" next to it in the photo
- **THEN** it is classified as a fuel-amount reading

#### Scenario: An unlabeled number is not a candidate
- **WHEN** a candidate has no recognized label next to it in the photo
- **THEN** it is not presented as a fuel-amount candidate, however close its magnitude is to a plausible fuel amount

#### Scenario: A price can be classified too, when it shares the volume label's adjacency
- **WHEN** a photo shows the fuel amount and the total price close enough together that both are nearest to the
  same recognized volume label
- **THEN** both are presented as fuel-amount candidates, and the user picks the correct one by its value

### Requirement: The user picks a candidate to accept, or leaves without picking one
The system SHALL show the chosen photo with a box drawn around every candidate and its classification ("ODO",
"TRIP" or "FUEL") shown as text next to the box, every box and its text in a neutral color until the user taps it,
and in a distinct, confirmable color once tapped. Tapping a candidate SHALL select it in place of whichever was
selected before, if any. The system SHALL let the user confirm the selected candidate, or navigate back without
confirming one, in which case the log event form is unchanged and nothing is kept.

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

#### Scenario: A fuel-amount candidate among others
- **WHEN** a photo has both an odometer-like and a fuel-amount-like candidate
- **THEN** each is shown with its own box, the odometer one labeled "ODO" and the fuel-amount one labeled "FUEL"

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

### Requirement: Accepting a fuel-amount candidate sets the fuel amount field and its unit
The system SHALL, when the user confirms a selected fuel-amount candidate on the photo review, or taps one in the
live scanner, set the log event form's fuel amount field to the candidate's value, replacing whatever it held
before, and SHALL return to the log event form. Unlike accepting an odometer or trip candidate, this SHALL NOT
change the mileage section's way or field: the fuel amount field and the mileage section are independent, each with
its own scan action. When the label that classified the candidate names a unit (a liter word or symbol, or a
gallon word), the system SHALL also set the fuel amount's unit to match, replacing whatever it held before; when
the label names neither, the unit SHALL be left as it was.

#### Scenario: Accept a fuel-amount reading from the review screen
- **WHEN** the user confirms a candidate classified as a fuel-amount reading
- **THEN** the fuel amount field shows the candidate's value, and the mileage section is unchanged

#### Scenario: Tap a live fuel-amount reading
- **WHEN** the user taps a reading classified as a fuel-amount reading in the live scanner
- **THEN** the scanner closes and the fuel amount field shows the reading's value

#### Scenario: The unit is preselected from the label
- **WHEN** the user accepts a fuel-amount candidate whose recognized label is "LITRAA"
- **THEN** the fuel amount's unit is set to liters

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
