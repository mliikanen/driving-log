## MODIFIED Requirements

### Requirement: Refueling can be logged from the log event form
The system SHALL offer "Refueling" as a second choice in the log event form's "Kind" selector (`distance-logging`),
alongside "Distance." Choosing it SHALL show a fuel amount field, a fuel type selector, a "filled up" checkbox, an
optional mileage section (the "Trip distance"/"New odometer" choice a "Distance" entry offers), the note element and
the photo strip. The date, time, time zone, vehicle selector (from the Home screen), saving and leaving SHALL work as
for a "Distance" entry.

#### Scenario: Choosing Refueling shows its fields
- **WHEN** the user opens the log event form and chooses "Refueling" in the Kind selector
- **THEN** the form shows a fuel amount field, a fuel type selector, a "filled up" checkbox, an optional mileage
  section, the note element and the photo strip

#### Scenario: Saving a refueling
- **WHEN** the user enters a valid fuel amount, chooses a fuel type and saves
- **THEN** the vehicle's log contains a new "Refueling" event with that amount, fuel type and the "filled up" state
  shown on the form

### Requirement: The fuel amount must be entered and above zero
The system SHALL require a number in the fuel amount field: while it is empty, the form's Save action SHALL be
disabled (not tappable), whatever the optional mileage section holds. The amount SHALL be more than zero: for a typed
zero, the system SHALL show an error on the field and SHALL NOT save the entry. The fuel amount field's label SHALL
carry a trailing "*", the required-field mark `distance-logging` uses; the mileage section's fields SHALL NOT.

#### Scenario: Empty fuel amount disables Save
- **WHEN** the user has chosen "Refueling" and the fuel amount field is empty
- **THEN** the form's Save action is disabled, whether or not the optional mileage section has anything typed

#### Scenario: A typed mileage does not enable Save on its own
- **WHEN** the user has chosen "Refueling," typed a mileage reading, and the fuel amount field is still empty
- **THEN** the form's Save action stays disabled

#### Scenario: Zero fuel amount
- **WHEN** the user types 0 as the fuel amount and saves
- **THEN** the system shows an error that the amount must be more than zero and adds nothing

#### Scenario: The required field is marked
- **WHEN** the user has chosen "Refueling" and opens the log event form
- **THEN** the fuel amount field's label carries a trailing "*", and the optional mileage section's fields do not

### Requirement: The fuel amount's unit remembers the last choice, independent of the vehicle
The system SHALL let the user choose the fuel amount's unit on the form: liters or gallons. It SHALL default to the
unit the user chose last, anywhere in the application, whatever the vehicle or the units of logged events: one global
preference, stored apart from any vehicle or event and never derived from the log. Changing the unit SHALL keep the
digits typed. Saving a refueling SHALL make its unit the remembered choice.

#### Scenario: Defaults to the last choice
- **WHEN** the user last chose gallons while logging a refueling for any vehicle, and opens the log event form for a
  different vehicle and chooses "Refueling"
- **THEN** the fuel amount field starts in gallons

#### Scenario: Not derived from the vehicle or the log
- **WHEN** a vehicle's own odometer unit is kilometers and no refueling has ever been logged for it
- **THEN** the fuel amount field still starts in whichever unit (liters or gallons) was last chosen elsewhere, not
  in a unit derived from the vehicle's odometer unit or from that vehicle's own log

#### Scenario: Changing the unit keeps the digits
- **WHEN** the user has typed 42.3 with liters selected and switches to gallons
- **THEN** the field still shows the digits, now in gallons

#### Scenario: Saving updates the remembered choice
- **WHEN** the user chooses gallons, logs a refueling and saves, then later opens the form again and chooses "Refueling"
- **THEN** the fuel amount field starts in gallons

#### Scenario: The remembered choice survives a restart
- **WHEN** a fuel unit choice was remembered, the app is closed completely and opened again
- **THEN** the log event form's fuel amount field still starts in that unit

### Requirement: The fuel type remembers the last choice, independent of the vehicle
The system SHALL preselect the fuel type the user chose last, anywhere in the application, when the vehicle being
logged for offers it. The remembered choice SHALL be one global preference, stored apart from any vehicle or event,
never derived from the log and never kept per vehicle. When the vehicle does not offer it, the first fuel type the
vehicle offers SHALL be preselected instead, leaving the remembered choice unchanged. Saving a refueling SHALL make
its fuel type the remembered choice.

#### Scenario: Defaults to the last choice
- **WHEN** the user last chose Diesel while logging a refueling for a vehicle whose fuel type offers Diesel, and
  opens the log event form for a different vehicle whose fuel type also offers Diesel, and chooses "Refueling"
- **THEN** Diesel is preselected as the fuel type

#### Scenario: Not derived from the vehicle or the log
- **WHEN** a vehicle has never had a refueling logged for it
- **THEN** the fuel type still starts as whichever type was last chosen elsewhere (narrowed to what that vehicle's
  fuel type offers), not a default derived from that vehicle's own log

#### Scenario: The remembered choice falls back when the vehicle does not offer it
- **WHEN** the user last chose LPG, and opens the log event form for a vehicle of the fuel type "Diesel" and
  chooses "Refueling"
- **THEN** Diesel (the first fuel type that vehicle offers) is preselected instead of LPG, and the remembered LPG
  choice is unchanged

#### Scenario: Saving updates the remembered choice
- **WHEN** the user chooses Premium petrol, logs a refueling and saves, then later opens the form again for a
  vehicle that offers Premium petrol and chooses "Refueling"
- **THEN** Premium petrol is preselected as the fuel type

#### Scenario: The remembered choice survives a restart
- **WHEN** a fuel type choice was remembered, the app is closed completely and opened again
- **THEN** the log event form's fuel type selector still starts on that type, subject to the same fallback

### Requirement: Mileage is optional for a refueling
A refueling form SHALL offer the "Trip distance"/"New odometer" choice a "Distance" entry offers, but SHALL NOT
require a value in it: Save is gated by the fuel amount alone. A typed mileage SHALL be validated as a "Distance"
entry's is (`distance-logging`) and saved in the refueling's entry as the "Distance" or "Odometer reading" event such
an entry would save, which sets or advances the odometer; the "Refueling" event never does. With no mileage, a
refueling SHALL NOT change the odometer.

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
Saving a valid refueling SHALL store, as one new entry or not at all: its fuel amount, fuel type and "filled up"
state as a "Refueling" event, its mileage (if any) as a second event, and its note and photos (if any) once, shared
by both. Leaving the form without saving SHALL discard all of it, as for a "Distance" entry. The fuel amount, fuel
type and "filled up" state SHALL be kept across a rotation or a process restart while the form is open, as a pending
note is.

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

## ADDED Requirements

### Requirement: The fuel type is chosen from a fixed list
The system SHALL let the user choose a refueling's fuel type from a fixed list: Regular petrol/gasoline, Premium
petrol/gasoline, Diesel, Premium diesel, Biodiesel, E85/flex fuel, LPG, CNG, Hydrogen, Other, narrowed to what the
vehicle being logged for offers (`vehicle-fuel-type`, "The fuel type filters which refueling fuel types are offered").

#### Scenario: The whole list for an unfiltered vehicle
- **WHEN** the user chooses "Refueling" for a vehicle of the fuel type "Other"
- **THEN** the fuel type selector offers all ten fuel types of the list, and no other
