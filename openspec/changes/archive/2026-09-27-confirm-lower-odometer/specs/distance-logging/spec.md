# Spec Delta

## MODIFIED Requirements

### Requirement: The new odometer must be higher than the previous known odometer
When a previous known odometer exists at the entry's date and time, the system SHALL refuse a new odometer count
that equals it, showing an error on the odometer field that names it, and SHALL NOT save the entry. A count that is
lower than it is not refused outright: see "A lower new odometer count that would replace the vehicle's current
odometer is saved only after confirmation" when the entry is for now, or for any moment at or after the vehicle's
latest logged event; see "A lower new odometer count added to history is saved without confirmation" when the entry
is backdated to a moment before the vehicle's latest logged event.

#### Scenario: Lower count
- **WHEN** the vehicle's current odometer is 45230 km and the user logs a new odometer count of 45100 for now (or any moment at or after the vehicle's latest logged event) and saves
- **THEN** the system asks the user to confirm before saving, rather than refusing outright — see "A lower new odometer count that would replace the vehicle's current odometer is saved only after confirmation"

#### Scenario: Lower count added to history
- **WHEN** the user logs a new odometer count for a date and time before the vehicle's latest logged event, and that count is lower than the odometer known at that earlier moment, and saves
- **THEN** the system saves it outright, without confirmation — see "A lower new odometer count added to history is saved without confirmation"

#### Scenario: Equal count
- **WHEN** the odometer known at the entry's date and time is 45230 km and the user types 45230 as the new odometer and saves
- **THEN** the system shows an error naming 45,230 km, stays on the form and adds nothing

## ADDED Requirements

### Requirement: A lower new odometer count that would replace the vehicle's current odometer is saved only after confirmation
When the user logs a new odometer count for now, or for any moment at or after the vehicle's latest logged event
(so the count would become the vehicle's new current odometer), and it is lower than the vehicle's current odometer
(strictly lower — an equal count is still refused outright, see "The new odometer must be higher than the previous
known odometer") and saves, the system SHALL show a confirmation dialog naming both the typed count and the current
odometer it would replace, and SHALL NOT save anything until the user confirms. Confirming SHALL save the typed count
as an odometer anchor (`vehicle-log`, "Odometer anchor events"), the same way a new odometer count is saved when none
was known before (`distance-logging`, "A new odometer count without a known odometer is saved as an odometer
anchor") — it becomes the vehicle's current odometer going forward, exactly like any other anchor, with no
constraint that it be higher than what it replaces. Dismissing or cancelling the dialog SHALL save nothing and SHALL
leave the form exactly as it was, with the typed count still there.

#### Scenario: Confirming saves it as an anchor
- **WHEN** the vehicle's current odometer is 45230 km, the user logs a new odometer count of 44000 for now, saves, and confirms the dialog
- **THEN** the log contains an odometer anchor of 44,000 km at that time, and the vehicle's current odometer is now 44,000 km

#### Scenario: The dialog names both values
- **WHEN** the vehicle's current odometer is 45230 km and the user logs a new odometer count of 44000 for now and saves
- **THEN** the confirmation dialog names both 44,000 km and 45,230 km

#### Scenario: Cancelling saves nothing
- **WHEN** the confirmation dialog is shown and the user cancels or dismisses it
- **THEN** nothing is saved, the form is shown exactly as it was, and the typed count (44,000) is still in the field

#### Scenario: Later entries build on the lower anchor
- **WHEN** a confirmed odometer anchor of 44000 km replaces a vehicle's current odometer of 45230 km, and the user later logs a trip distance of 30 km
- **THEN** the vehicle's current odometer is 44,030 km

### Requirement: A lower new odometer count added to history is saved without confirmation
When the user logs a new odometer count for a date and time before the vehicle's latest logged event, and that count
is lower than the odometer known at that earlier moment, the system SHALL save it outright as an odometer anchor at
that moment (`vehicle-log`, "Odometer anchor events"), without asking for confirmation. This is not a correction to
what the vehicle's odometer currently reads, so it needs none — the same as inserting any other backdated anchor,
higher or lower, already needs none: later events, if any, are recalculated forward from it exactly as they already
are for a higher backdated anchor (`vehicle-log`, "A later anchor replaces the running total"); this change does not
alter that. The equal case is unaffected — see "The new odometer must be higher than the previous known odometer."

#### Scenario: A backdated lower count is saved without confirmation
- **WHEN** the vehicle's latest logged event is later than the moment the user is logging for, the odometer known at that earlier moment is 45230 km, and the user types 45100 and saves
- **THEN** the system saves an odometer anchor of 45,100 km at that moment without showing a confirmation dialog
