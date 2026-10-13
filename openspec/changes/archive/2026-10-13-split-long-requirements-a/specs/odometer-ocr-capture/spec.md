## MODIFIED Requirements

### Requirement: A photo is chosen to scan for a reading
The log event form SHALL offer a "Scan a reading" action that opens the live scanner, which SHALL offer a photo action
opening the system photo chooser used for a vehicle's picture (the camera app included; this application requests no
permission for it). Choosing a photo SHALL lead to detection and the photo review; cancelling the chooser, or leaving
the review without accepting, SHALL return to the live scanner. On iOS, with no on-device recognizer yet, the action
SHALL NOT be shown.

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

### Requirement: A detected reading is classified as an odometer or a trip-meter reading
The system SHALL classify each candidate by a recognized label next to it in the photo: "ODO", "ODOMETER" or "TOTAL
DISTANCE" as odometer-like, "TRIP", "TRIP A", "TRIP B" or "T" as trip-like. With no such label, it SHALL use the
candidate's magnitude against the odometer known at the entry's time (`distance-logging`, "The previous known
odometer"): at or plausibly above it is odometer-like, clearly smaller and plausible as a trip is trip-like. A
candidate with neither SHALL NOT be presented.

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
The system SHALL classify a candidate as a fuel-amount reading when a recognized volume label is next to it in the
photo ("LITRAA", "LITARA", "LITROV", "dm³", "L", "LITERS", "LITRES", "GAL", "GALLON" or "GALLONS"), by the adjacency
rule odometer and trip classification use. It SHALL NOT use magnitude: a pump or receipt shows the amount and the
price as similar numbers told apart only by their labels. A candidate with no volume label next to it SHALL NOT be
presented as a fuel-amount candidate.

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
The system SHALL show the chosen photo with a box around every candidate and its classification ("ODO", "TRIP" or
"FUEL") as text next to it, in a neutral color until tapped and in a distinct, confirmable color once tapped. Tapping
a candidate SHALL select it in place of any earlier selection. The user SHALL be able to confirm the selected
candidate, or navigate back without confirming one, leaving the log event form unchanged and keeping nothing.

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

### Requirement: Accepting a fuel-amount candidate sets the fuel amount field and its unit
When the user confirms a selected fuel-amount candidate on the photo review, or taps one in the live scanner, the
system SHALL set the fuel amount field to its value, replacing what it held, and return to the log event form,
without changing the mileage section's way or field. When the label that classified it names a unit (a liter word
or symbol, or a gallon word), the fuel amount's unit SHALL be set to match; otherwise the unit SHALL stay as it was.

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
Once the user accepts a candidate, the system SHALL keep the scanned photo and the full detection result (every
candidate's position, recognized text and classification, not only the accepted one), and SHALL store them under the
resulting event only when the form is saved. For a reading tapped in the live scanner, they SHALL be that camera frame
and its detections. Leaving the form unsaved SHALL discard them; scanning again before saving SHALL keep only the
latest accepted scan.

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

### Requirement: Readings in seven-segment digits are detected
The system SHALL detect, offline and on the device, readings shown in seven-segment (LCD) digits as well as in
ordinary printed digits, and SHALL treat both alike: the same candidate, classification and review rules. A reading
found by both kinds of recognition SHALL be presented once. On iOS the scan action is not offered
(`odometer-ocr-capture`, "A photo is chosen to scan for a reading"), so none of this applies there.

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
The live scanner SHALL show the back camera's preview full screen, boxing every candidate reading recently detected,
with its classification and value as text, detected offline on the device and classified as a scanned photo's are. The
boxes SHALL follow the view; a reading SHALL stay shown briefly after the camera loses it without flickering. Tapping
a reading SHALL accept it. It SHALL offer a close action; it and back navigation SHALL accept and keep nothing,
leaving the form unchanged.

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
The system SHALL ask for the camera permission when the user taps "Scan a reading" and it has not been granted, and
not before. If refused, the live scanner SHALL open without a preview, say that it needs camera access and that it
can be allowed in the device settings, and keep its photo and close actions working. Once it is granted later, the
scanner SHALL show the preview the next time it opens. On iOS the scan action is not offered.

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
The system SHALL keep the photo (or live scanner frame) and the full detection result of a reading accepted on the
add-vehicle form, and SHALL store them with the vehicle's initial odometer event when the vehicle is saved, under
the rules of a scan accepted on the log event form: leaving without saving discards them, scanning again keeps only
the latest accepted scan, no user-facing screen shows them, and removing the event does not remove them.

#### Scenario: Saved with the new vehicle
- **WHEN** the user accepts a scanned reading on the add-vehicle form and saves the vehicle
- **THEN** the photo or frame and its full detection result are stored, linked to the vehicle's initial odometer event

#### Scenario: Discarded when the vehicle is not added
- **WHEN** the user accepts a scanned reading on the add-vehicle form and leaves it without saving
- **THEN** neither the photo nor its detections are stored anywhere

#### Scenario: A typed odometer keeps no scan
- **WHEN** the user types the odometer by hand and saves the vehicle
- **THEN** no scan is stored with its initial odometer event

## ADDED Requirements

### Requirement: A number as near the volume label may also be a fuel-amount candidate
The system SHALL tell fuel-amount candidates apart only by which recognized label is nearest, not by what else a
number means: on a display where a price or another amount sits as close to the one recognized volume label as the
real reading does, that number MAY also be classified as a fuel-amount candidate. The system SHALL let the user
resolve this by picking a candidate ("The user picks a candidate to accept, or leaves without picking one").

#### Scenario: The user picks the amount over the price
- **WHEN** a photo's fuel amount 42.30 and total price 75.12 are both presented as fuel-amount candidates, and the
  user selects 42.30 and confirms
- **THEN** the fuel amount field shows 42.30

### Requirement: A scan is stored under the event whose number it filled
When the log event form saves more than one event (a refueling with mileage, `vehicle-log`'s "The log stores one
fact per event, grouped into entries"), the system SHALL store an accepted scan under the event whose number it
filled: a fuel-amount scan under the "Refueling" event, a mileage scan under the "Distance" or "Odometer reading"
event. A mileage scan whose mileage was cleared before saving filled nothing saved, and SHALL NOT be stored.

#### Scenario: A mileage scan that set a first odometer
- **WHEN** the user types a fuel amount, accepts a scanned odometer reading for the mileage at a time when no
  odometer is known yet, and saves the refueling
- **THEN** the photo and its detections are linked to the entry's "Odometer reading" event

### Requirement: A stored scan is never shown and outlives its event
The system SHALL NOT show a stored scan's photo or detections on any user-facing screen; they exist only to review a
misdetection after the fact. Once stored, they SHALL be kept until they themselves are removed: removing the event
they belong to SHALL NOT remove them.

#### Scenario: A scanned refueling's details show no scan
- **WHEN** the user opens the details of a refueling whose fuel amount was accepted from a scan and that has one
  attached photo
- **THEN** the details screen shows that one attached photo and neither the scanned photo nor any detection
