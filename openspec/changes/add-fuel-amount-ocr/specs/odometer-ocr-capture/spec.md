# Spec Delta

## ADDED Requirements

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

### Requirement: A detected reading is classified as a fuel amount, by label only
The system SHALL classify a candidate as a fuel-amount reading when a recognized text label for a volume is next to
it in the photo ("L", "LITERS", "LITRES", "GAL", "GALLON", "GALLONS" or "VOLUME"), using the same label-adjacency
rule odometer/trip classification uses (`odometer-ocr-capture`, "A detected reading is classified as an odometer or
a trip-meter reading"). Unlike an odometer or trip reading, a fuel amount SHALL NOT be classified by magnitude: a
fuel pump or receipt display typically shows the amount dispensed and its total price as two numbers of similar
size with nothing but their labels to tell them apart, so there is no magnitude fallback and no plausible-range
check. A candidate with no recognized volume label next to it — including one next to a price-shaped label such as
"$", "PRICE", "TOTAL" or "COST" — SHALL NOT be presented as a fuel-amount candidate.

#### Scenario: Classified by a volume label
- **WHEN** a candidate has the recognized text "L" or "GAL" next to it in the photo
- **THEN** it is classified as a fuel-amount reading

#### Scenario: An unlabeled number is not a candidate
- **WHEN** a candidate has no recognized label next to it in the photo
- **THEN** it is not presented as a fuel-amount candidate, however close its magnitude is to a plausible fuel amount

#### Scenario: A price number is not a candidate
- **WHEN** a candidate has the recognized text "$" or "TOTAL" next to it in the photo
- **THEN** it is not presented as a fuel-amount candidate

### Requirement: Accepting a fuel-amount candidate sets the fuel amount field
The system SHALL, when the user confirms a selected fuel-amount candidate on the photo review, or taps one in the
live scanner, set the log event form's fuel amount field to the candidate's value, replacing whatever it held
before, and SHALL return to the log event form. Unlike accepting an odometer or trip candidate, this SHALL NOT
change the mileage section's way or field: the fuel amount field and the mileage section are independent, each with
its own scan action.

#### Scenario: Accept a fuel-amount reading from the review screen
- **WHEN** the user confirms a candidate classified as a fuel-amount reading
- **THEN** the fuel amount field shows the candidate's value, and the mileage section is unchanged

#### Scenario: Tap a live fuel-amount reading
- **WHEN** the user taps a reading classified as a fuel-amount reading in the live scanner
- **THEN** the scanner closes and the fuel amount field shows the reading's value

## MODIFIED Requirements

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
