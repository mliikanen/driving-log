# Spec Delta

## MODIFIED Requirements

### Requirement: A trip distance must be entered and above zero
The system SHALL require a number in the active field: while it is empty, the form's Save action SHALL be disabled
and SHALL NOT be tappable. A trip distance SHALL be more than zero: once the field is non-empty, the system SHALL
show an error on the field and SHALL NOT save the entry when a typed trip distance is zero.

#### Scenario: Empty field
- **WHEN** the active field has nothing typed
- **THEN** the form's Save action is disabled

#### Scenario: Zero distance
- **WHEN** the user types 0 as the trip distance and saves
- **THEN** the system shows an error that the distance must be more than zero and adds nothing

#### Scenario: The error clears when the user types
- **WHEN** an error is shown on the field and the user types a digit
- **THEN** the error is no longer shown

### Requirement: A new odometer count without a known odometer is saved as an odometer anchor
When the user chooses "New odometer" and no odometer is known at the entry's date and time, the system SHALL save the typed
count as an odometer anchor event, and SHALL NOT save a distance event. An odometer anchor is an odometer-setting event: it
sets the odometer to the typed count at the entry's time, and it takes part in the previous known odometer and the current
odometer like the initial odometer event does (the latest odometer-setting event wins, and distances after it add to it). The
form SHALL say that no odometer is known at that time and that the count will be saved as a new odometer starting point, SHALL
show no distance, and SHALL allow saving once a count is typed. The count SHALL be entered and unit-converted like any other,
the future check SHALL apply, and a count of zero SHALL be accepted. While the field is empty, the form's Save action SHALL be
disabled, as it is for a trip distance. The tenths choice used SHALL be remembered like for a distance entry.

#### Scenario: New odometer before the initial odometer
- **WHEN** a vehicle was added with an initial odometer of 45200 km, and the user chooses "New odometer", a time a week before the initial odometer event, types 44000 and saves
- **THEN** the log contains an odometer anchor of 44,000 km at that time and no distance event, and the vehicle's current odometer is still 45200 km

#### Scenario: The form explains it
- **WHEN** the user chooses "New odometer" and a time before the initial odometer event
- **THEN** the form says that no odometer is known at that time and that the count will be saved as a new odometer starting point, shows no distance, and saving is possible once a count is typed

#### Scenario: Later entries build on the anchor
- **WHEN** an odometer anchor of 44000 km exists a week before the initial odometer event, and the user chooses "New odometer" for a time three days before the initial odometer event
- **THEN** the form shows a previous known odometer of 44,000 km

#### Scenario: Trip distances after the anchor count up to the initial odometer
- **WHEN** an odometer anchor of 44000 km exists a week before the initial odometer event, a trip distance of 30 km is logged three days before it, and the user chooses "New odometer" for two days before it
- **THEN** the form shows a previous known odometer of 44,030 km

#### Scenario: An anchor at a time where an odometer is known is not created
- **WHEN** an odometer is known at the entry's time and the user chooses "New odometer" and types a higher count
- **THEN** a distance event is saved, as usual

#### Scenario: Nothing typed
- **WHEN** the user chooses "New odometer" with no odometer known at the time and nothing is typed
- **THEN** the form's Save action is disabled
