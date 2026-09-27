# Spec Delta

## MODIFIED Requirements

### Requirement: The new odometer must be higher than the previous known odometer
When a previous known odometer exists at the entry's date and time, the system SHALL refuse a new odometer count
that equals it, showing an error on the odometer field that names it, and SHALL NOT save the entry. A count that is
lower than it is not refused outright — see "A lower new odometer count is saved only after confirmation."

#### Scenario: Lower count
- **WHEN** the previous known odometer is 45230 km and the user types 45100 as the new odometer and saves
- **THEN** the system asks the user to confirm before saving, rather than refusing outright — see "A lower new odometer count is saved only after confirmation"

#### Scenario: Equal count
- **WHEN** the previous known odometer is 45230 km and the user types 45230 as the new odometer and saves
- **THEN** the system shows an error naming 45,230 km, stays on the form and adds nothing

## ADDED Requirements

### Requirement: A lower new odometer count is saved only after confirmation
When a previous known odometer exists at the entry's date and time and the user types a new odometer count lower
than it (strictly lower — an equal count is still refused outright, see "The new odometer must be higher than the
previous known odometer") and saves, the system SHALL show a confirmation dialog naming both the typed count and the
previous known odometer, and SHALL NOT save anything until the user confirms. Confirming SHALL save the typed count
as an odometer anchor (`vehicle-log`, "Odometer anchor events"), the same way a new odometer count is saved when none
was known before (`distance-logging`, "A new odometer count without a known odometer is saved as an odometer
anchor") — it becomes the vehicle's current odometer going forward, exactly like any other anchor, with no
constraint that it be higher than what it replaces. Dismissing or cancelling the dialog SHALL save nothing and SHALL
leave the form exactly as it was, with the typed count still there.

#### Scenario: Confirming saves it as an anchor
- **WHEN** the previous known odometer is 45230 km, the user types 44000, saves, and confirms the dialog
- **THEN** the log contains an odometer anchor of 44,000 km at that time, and the vehicle's current odometer is now 44,000 km

#### Scenario: The dialog names both values
- **WHEN** the previous known odometer is 45230 km and the user types 44000 and saves
- **THEN** the confirmation dialog names both 44,000 km and 45,230 km

#### Scenario: Cancelling saves nothing
- **WHEN** the confirmation dialog is shown and the user cancels or dismisses it
- **THEN** nothing is saved, the form is shown exactly as it was, and the typed count (44,000) is still in the field

#### Scenario: Later entries build on the lower anchor
- **WHEN** a confirmed odometer anchor of 44000 km replaces a previous known odometer of 45230 km, and the user later logs a trip distance of 30 km
- **THEN** the vehicle's current odometer is 44,030 km
