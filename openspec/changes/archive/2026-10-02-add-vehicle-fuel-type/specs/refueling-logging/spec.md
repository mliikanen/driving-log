# Spec Delta

## MODIFIED Requirements

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
