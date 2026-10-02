# Proposal

## Why

Logging a refueling today offers all ten fuel types regardless of the vehicle, including combinations that can
never actually apply to it (a Diesel owner sees Regular petrol, LPG, Hydrogen, and so on). Giving each vehicle its
own fuel type — a coarse, engine-level category, not the precise pump-side choice made each time a refueling is
logged — lets the refueling form narrow its own fuel type list to what that vehicle can actually take, the same way
`vehicle-type` already narrows a vehicle's placeholder icon from one fixed, per-vehicle property.

## What Changes

- Every vehicle gets a fuel type from a fixed, coarse set: Petrol, Diesel, LPG, CNG, Hydrogen, Other — chosen when
  the vehicle is added (preselected Petrol), changeable later on the edit screen, and never clearable, mirroring
  `vehicle-type`'s own shape exactly (a selectable set, no "none," offered as its own small property, not folded
  into an existing one).
- Logging a refueling for a vehicle now offers only the refueling-logging fuel types that vehicle's fuel type
  allows: Petrol offers Regular petrol, Premium petrol, and E85/flex fuel; Diesel offers Diesel, Premium diesel,
  and Biodiesel; LPG offers LPG; CNG offers CNG; Hydrogen offers Hydrogen. Every group also offers "Other"
  (refueling-logging's own catch-all) as an escape hatch, and a vehicle whose fuel type is "Other" offers every
  fuel type, unfiltered — covering a vehicle whose real fuel doesn't fit the coarse set, or a genuinely mixed-fuel
  vehicle (LPG/petrol bi-fuel, flex-fuel-on-either).
- **Modifies `refueling-logging`'s "The fuel type remembers the last choice, independent of the vehicle"**: the
  remembered choice is still one single global value (not a separate memory per vehicle — the requirement's title
  stays accurate in that sense), but it is now only preselected when the current vehicle's fuel type actually
  offers it; otherwise the first fuel type that vehicle's filter offers is preselected instead, without losing or
  overwriting the remembered global choice unless the user actually saves with a different one.
- Existing vehicles (saved before this change) are given the fuel type Petrol, the same migration shape
  `vehicle-type` already used for "Car."

## Capabilities

### New Capabilities
- `vehicle-fuel-type`: a vehicle's own coarse fuel/engine category — the fixed set, how it's chosen and changed,
  and how it narrows refueling-logging's fuel type choices. Mirrors `vehicle-type`'s shape and requirement
  structure.

### Modified Capabilities
- `refueling-logging`: "The fuel type remembers the last choice, independent of the vehicle" now also narrows the
  offered list to the vehicle's fuel type and falls back to the first offered type when the remembered choice isn't
  one of them.

## Impact

- New `VehicleFuelType` enum (`shared/src/commonMain/.../vehicle/domain/`), distinct from the existing, more
  granular `FuelType` (the refueling-time choice) — same file location and `code`/`label`/`fromCode` shape as
  `VehicleType`.
- `Vehicle.sq`: a new `vehicle_fuel_type` column (`TEXT NOT NULL DEFAULT 'PETROL'`), a new migration (`11.sqm`,
  following `10.sqm`), and the accompanying `generateMaestroFixtures` re-run every checked-in fixture needs once a
  migration lands (`docs/test-fixtures.md`).
- `Vehicle` domain model, `SqlDelightVehicleRepository`/`VehicleRepository`, `AddVehicleProcessor`/Screen,
  `EditVehicleProcessor`/Screen: a new fuel-type choice alongside the existing type/color choices, following the
  same add-form-preselects/edit-form-shows-current/never-clearable shape `vehicle-type` already has.
- `LogEventProcessor`/`LogEventScreen`'s `FuelTypeSelector`: takes the signed-in vehicle's fuel type and filters
  which `FuelType` options it offers, plus the fallback-when-not-offered logic for the remembered global choice.

**Explicitly out of scope**: an "Electric" fuel type and anything related to logging a charge instead of a
refueling — electric vehicles have no refueling-shaped event in this app at all yet, and adding one is a
substantially bigger change (a new event kind, different units, no "filled up" concept) than this proposal's scope.
A vehicle that is actually electric can use "Other" today, same as any fuel this set doesn't name.
