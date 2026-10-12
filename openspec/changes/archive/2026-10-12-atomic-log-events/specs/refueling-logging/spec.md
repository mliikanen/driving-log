# Spec Delta

## MODIFIED Requirements

### Requirement: Mileage is optional for a refueling
The system SHALL show the same "Trip distance"/"New odometer" choice on a refueling form that a "Distance" entry
offers, but SHALL NOT require a value in it to save: the form's Save action is gated by the fuel amount alone
(`refueling-logging`, "The fuel amount must be entered and above zero"), never by the mileage section being empty.
When the user does type a mileage value, it is validated exactly as a "Distance" entry's would be
(`distance-logging`'s "A distance is logged as a trip distance or as a new odometer count," "The previous known
odometer," "The new odometer must be higher than the previous known odometer," and both lower-odometer-confirmation
requirements all apply unchanged), and it is saved as its own event in the refueling's entry (`vehicle-log`, "The
log stores one fact per event, grouped into entries"): the same "Distance" or "Odometer reading" event a "Distance"
entry with that mileage would have saved, which sets or advances the vehicle's odometer. The "Refueling" event itself
never does. A refueling saved with no mileage typed SHALL NOT change the vehicle's odometer at all.

#### Scenario: Saving with a trip distance
- **WHEN** the user chooses "Refueling," types a trip distance of 30 km, a fuel amount and saves
- **THEN** the log contains a "Refueling" event and, in the same entry, a "Distance" event of 30 km, and the vehicle's current odometer advances by 30 km

#### Scenario: Saving with a new odometer count
- **WHEN** the user chooses "Refueling," chooses "New odometer," types a valid count, a fuel amount and saves
- **THEN** the log contains a "Refueling" event and, in the same entry, the "Distance" (or, with no odometer known at that time, "Odometer reading") event that count makes, and the vehicle's current odometer is that count

#### Scenario: Saving with a confirmed lower odometer count
- **WHEN** the user chooses "Refueling," chooses "New odometer," types a count lower than the current odometer, saves and confirms
- **THEN** the log contains a "Refueling" event and, in the same entry, an "Odometer reading" event of that count

#### Scenario: Saving with no mileage
- **WHEN** the user chooses "Refueling," leaves the mileage section untouched, types a fuel amount and saves
- **THEN** the log contains an entry with only a "Refueling" event and the vehicle's current odometer is unchanged

#### Scenario: The same odometer checks apply when mileage is given
- **WHEN** the user chooses "Refueling," chooses "New odometer," types a count equal to the odometer already known at that time
- **THEN** the system refuses it the same way it would for a "Distance" entry, and saves nothing

### Requirement: Attached fuel data is saved with the event, or discarded with the rest of the form
Saving a valid refueling SHALL store its fuel amount, fuel type and "filled up" state as a "Refueling" event, its
mileage (if any) as a second event, and its note (if any) and photos (if any) once, shared by both events, all
together as one new entry, or nothing at all. Leaving the form without saving SHALL discard all of it, the same way
leaving a "Distance" entry's form discards everything typed. The fuel amount, fuel type and "filled up" state SHALL
be kept across a rotation or a process restart while the form is open, the same way a pending note or an attached
photo already is.

#### Scenario: Everything is saved together
- **WHEN** the user fills in the fuel amount, fuel type, unchecks "filled up," adds a note and a photo, and saves
- **THEN** the log contains one new entry with a "Refueling" event holding the fuel data, and the note and photo

#### Scenario: Mileage, note and photos are saved together
- **WHEN** the user fills in a refueling with a trip distance, a note and a photo, and saves
- **THEN** the log contains one new entry with a "Refueling" event and a "Distance" event, and the note and the photo are stored once and shown from both

#### Scenario: A failed save stores nothing
- **WHEN** saving a refueling with a mileage fails part way
- **THEN** neither event, nor the note or any photo, is stored

#### Scenario: Leaving the form drops everything
- **WHEN** the user fills in refueling fields and leaves the form without saving
- **THEN** no event is added and nothing is stored

#### Scenario: Fields survive a rotation
- **WHEN** the user has typed a fuel amount and chosen a fuel type, and rotates the device before saving
- **THEN** the form still shows that amount and fuel type
