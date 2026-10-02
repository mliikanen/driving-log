# refueling-logging Specification

## Purpose

Lets the user log a refueling — a fuel amount, a fuel type, whether the tank was filled up, and, optionally, a
mileage reading — as the log event form's second kind, alongside "Distance."

## Requirements

### Requirement: Refueling can be logged from the log event form
The system SHALL offer "Refueling" as a second choice in the log event form's "Kind" selector (`distance-logging`,
"The kind of event is chosen"), alongside "Distance." Choosing it SHALL show: a fuel amount field, a fuel type
selector, a "filled up" checkbox, an optional mileage section (the same "Trip distance"/"New odometer" choice a
"Distance" entry offers), the note element and the photo strip. The date, time, time zone, vehicle selector (from
the Home screen) and the form's save/leave mechanics are unchanged from a "Distance" entry's.

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
disabled and SHALL NOT be tappable, independent of whether the refueling's optional mileage section has anything
typed in it. The fuel amount SHALL be more than zero: once the field is non-empty, the system SHALL show an error on
the field and SHALL NOT save the entry when a typed amount is zero. The fuel amount field's label SHALL carry a
trailing "*", matching the required-field convention `distance-logging`'s "A trip distance must be entered and above
zero" already establishes; the optional mileage section's fields carry no such asterisk.

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
The system SHALL let the user choose the fuel amount's unit on the form: liters or gallons. The choice SHALL default
to whichever the user chose last, anywhere in the application, regardless of which vehicle is being logged for or
what unit any previously logged event used — a single, global preference, stored separately from any vehicle or
event and never derived from the log. Changing the unit SHALL keep the digits typed, as changing the distance unit
already does. Saving a refueling SHALL update the remembered choice to the unit used.

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
The system SHALL let the user choose a fuel type from a fixed list: Regular petrol/gasoline, Premium petrol/gasoline,
Diesel, Premium diesel, Biodiesel, E85/flex fuel, LPG, CNG, Hydrogen, Other — narrowed to what the vehicle being
logged for offers (`vehicle-fuel-type`'s "The fuel type filters which refueling fuel types are offered"). The
remembered choice itself SHALL remain a single, global preference, stored separately from any vehicle or event and
never derived from the log — not a separate remembered value per vehicle. The choice SHALL default to whichever the
user chose last, anywhere in the application, when the current vehicle offers it; when it does not (the vehicle's
fuel type filters it out), the first fuel type that vehicle offers SHALL be preselected instead, without changing
the remembered global choice. Saving a refueling SHALL update the remembered global choice to the fuel type used.

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

### Requirement: The "filled up" checkbox defaults to checked
The system SHALL show a "filled up" checkbox on the refueling form, checked by default for every new refueling. The
user SHALL be able to uncheck it for a partial fill. The checked or unchecked state SHALL be saved with the event and
SHALL NOT be remembered across events — each new refueling starts checked again, regardless of the state the
previous one was saved with.

#### Scenario: Starts checked
- **WHEN** the user opens the log event form and chooses "Refueling"
- **THEN** the "filled up" checkbox is checked

#### Scenario: Unchecking for a partial fill
- **WHEN** the user unchecks "filled up" and saves
- **THEN** the saved "Refueling" event records that it was not a full fill-up

#### Scenario: Each refueling starts checked again
- **WHEN** the user saves a refueling with "filled up" unchecked, then opens the log event form again and chooses "Refueling"
- **THEN** the "filled up" checkbox is checked again, not remembering the previous entry's unchecked state

### Requirement: Mileage is optional for a refueling
The system SHALL show the same "Trip distance"/"New odometer" choice on a refueling form that a "Distance" entry
offers, but SHALL NOT require a value in it to save: the form's Save action is gated by the fuel amount alone
(`refueling-logging`, "The fuel amount must be entered and above zero"), never by the mileage section being empty.
When the user does type a mileage value, it is validated and saved exactly as a "Distance" entry's would be
(`distance-logging`'s "A distance is logged as a trip distance or as a new odometer count," "The previous known
odometer," "The new odometer must be higher than the previous known odometer," and both lower-odometer-confirmation
requirements all apply unchanged), and the resulting "Refueling" event sets or advances the vehicle's odometer the
same way a "Distance" entry or an odometer anchor already does. A refueling saved with no mileage typed SHALL NOT
change the vehicle's odometer at all.

#### Scenario: Saving with a trip distance
- **WHEN** the user chooses "Refueling," types a trip distance of 30 km, a fuel amount and saves
- **THEN** the log contains a "Refueling" event that also advances the vehicle's current odometer by 30 km

#### Scenario: Saving with a new odometer count
- **WHEN** the user chooses "Refueling," chooses "New odometer," types a valid count, a fuel amount and saves
- **THEN** the log contains a "Refueling" event that also sets the vehicle's current odometer to that count

#### Scenario: Saving with no mileage
- **WHEN** the user chooses "Refueling," leaves the mileage section untouched, types a fuel amount and saves
- **THEN** the log contains a "Refueling" event and the vehicle's current odometer is unchanged

#### Scenario: The same odometer checks apply when mileage is given
- **WHEN** the user chooses "Refueling," chooses "New odometer," types a count equal to the odometer already known at that time
- **THEN** the system refuses it the same way it would for a "Distance" entry, and saves nothing

### Requirement: Attached fuel data is saved with the event, or discarded with the rest of the form
Saving a valid refueling SHALL store its fuel amount, fuel type, "filled up" state, mileage (if any), note (if any)
and photos (if any) together as one new event. Leaving the form without saving SHALL discard all of it, the same way
leaving a "Distance" entry's form discards everything typed. The fuel amount, fuel type and "filled up" state SHALL
be kept across a rotation or a process restart while the form is open, the same way a pending note or an attached
photo already is.

#### Scenario: Everything is saved together
- **WHEN** the user fills in the fuel amount, fuel type, unchecks "filled up," adds a note and a photo, and saves
- **THEN** the log contains one new "Refueling" event with all of that

#### Scenario: Leaving the form drops everything
- **WHEN** the user fills in refueling fields and leaves the form without saving
- **THEN** no event is added and nothing is stored

#### Scenario: Fields survive a rotation
- **WHEN** the user has typed a fuel amount and chosen a fuel type, and rotates the device before saving
- **THEN** the form still shows that amount and fuel type
