# Spec Delta

## MODIFIED Requirements

### Requirement: The kind of event is chosen
The system SHALL show a "Kind" selector at the top of the log event form: a dropdown, in the same style as the
vehicle selector, listing the kinds of event the form can log — "Distance" and "Refueling" (`refueling-logging`,
"Refueling can be logged from the log event form"). "Distance" SHALL be preselected. The selector SHALL be an
enabled, tappable Material 3 component whenever it lists more than one kind — as it always does now that
"Refueling" exists — and SHALL instead be a disabled Material 3 component (shown in Material's disabled colors,
exactly as the Home screen's not-yet-available actions are) and SHALL NOT react to a tap whenever it would list only
one kind, the same rule that kept it disabled before "Refueling" existed. Choosing a kind SHALL show that kind's
fields on the form below it. When the form is opened from the Home screen the Kind selector SHALL share one row
with the vehicle selector, the two of equal width and height, the Kind selector first (on the left). When the form
is opened from a vehicle's details screen (where there is no vehicle selector) the Kind selector SHALL take the row
alone.

#### Scenario: The kind selector is shown on both routes, disabled
- **WHEN** the form offers only one kind of event (not the case today, since "Distance" and "Refueling" are both always offered)
- **THEN** the kind selector shows that one kind selected and disabled, on both routes the form can be opened from

#### Scenario: Distance is preselected
- **WHEN** the user opens the log event form
- **THEN** "Distance" is selected in the Kind selector and the Distance fields are shown

#### Scenario: Choosing Refueling switches the form
- **WHEN** the user opens the Kind selector and chooses "Refueling"
- **THEN** the form shows the refueling fields (`refueling-logging`, "Refueling can be logged from the log event form")
  in place of the Distance fields

#### Scenario: Sharing the row with the vehicle selector
- **WHEN** the form is opened from the Home screen
- **THEN** the Kind selector and the vehicle selector are shown side by side, each half the row's width and the same height, the Kind selector on the left

#### Scenario: Alone from the details screen
- **WHEN** the form is opened from a vehicle's details screen
- **THEN** the Kind selector is shown alone, the full width of the row, and no vehicle selector is shown

#### Scenario: Tapping the disabled selector does nothing
- **WHEN** the kind selector is disabled (because the form offers only one kind) and the user taps it
- **THEN** nothing happens: it does not open, and the form is unchanged

### Requirement: A trip distance must be entered and above zero
The system SHALL require a number in the active field of a "Distance" entry: while it is empty, the form's Save
action SHALL be disabled and SHALL NOT be tappable. A trip distance SHALL be more than zero: once the field is
non-empty, the system SHALL show an error on the field and SHALL NOT save the entry when a typed trip distance is
zero. The field's label ("Trip distance" or "New odometer", whichever is active) SHALL carry a trailing "*", and the
form SHALL show a line near its Save action explaining the convention: "* indicates a required field". This
requirement governs the "Distance" kind's own field only; a refueling's optional mileage section does not gate Save
the same way, and its label carries no such asterisk (`refueling-logging`, "The fuel amount must be entered and
above zero" states the field that does gate Save for a refueling, and its own label is what carries the "*").

#### Scenario: Empty field
- **WHEN** the user has chosen "Distance" and the active field has nothing typed
- **THEN** the form's Save action is disabled

#### Scenario: Zero distance
- **WHEN** the user types 0 as the trip distance and saves
- **THEN** the system shows an error that the distance must be more than zero and adds nothing

#### Scenario: The error clears when the user types
- **WHEN** an error is shown on the field and the user types a digit
- **THEN** the error is no longer shown

#### Scenario: The required field is marked
- **WHEN** the user has chosen "Distance" and opens the log event form
- **THEN** the active field's label ("Trip distance" or "New odometer") carries a trailing "*", and a line near the Save action reads "* indicates a required field"
