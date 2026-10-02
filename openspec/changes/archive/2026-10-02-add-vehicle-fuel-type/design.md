# Design

## Context

`vehicle-type` is the exact precedent for a small, fixed, per-vehicle categorical property: a `VehicleType` enum
(`code`/`label`, `fromCode` treating an unknown code as `OTHER`), a `NOT NULL DEFAULT 'CAR'` column, a selectable
choice on the add screen (preselected, no "none") carried onto the edit screen, and a migration note for existing
rows. This change adds a second such property, `VehicleFuelType`, following the same shape — see proposal.md for
why it's a separate property rather than folded into `vehicle-type` (a different axis: fuel/engine, not kind of
vehicle; `vehicle-color` already established that a vehicle can have more than one such independent property).

The one piece without a direct precedent is the filtering itself. `LogEventProcessor` (vehicle logging) already
combines two sources reactively: `repository.observeLastFuelUnit()`/`observeLastFuelType()` (the global remembered
preference, `add-refueling-logging`) and `selected: Flow<Pair<VehicleDetails?, List<VehicleEvent>>>` (the
currently-selected vehicle, which changes live if the form was opened from the Home screen with no fixed vehicle —
`chooseVehicle = vehicleId.isEmpty()`). `VehicleDetails.vehicle` already carries `type` and `color`; once `Vehicle`
gains `fuelType`, this same reactive path exposes it with no new plumbing for "get the current vehicle's data into
this processor" — only the combination logic (global preference × current vehicle's allowed set) is new.

See proposal.md for the full behavioral scope; this covers how it's built.

## Goals / Non-Goals

**Goals:**
- Reuse `vehicle-type`'s exact shape for `VehicleFuelType` itself (enum, column, add/edit screen behavior,
  migration) rather than inventing a new pattern for a property that is structurally identical.
- Make the refueling fuel type filter reactive to the *currently selected* vehicle, not just the vehicle the form
  opened with — the vehicle selector can change while the form is open.

**Non-Goals:** everything proposal.md's "Explicitly out of scope" section lists (an Electric fuel type, EV
charging) — not restated here.

## Decisions

**1. `VehicleFuelType` is its own enum, not a new case folded into the existing `FuelType`.** They answer different
questions at different times (what the vehicle *can* take, set once and rarely changed, vs. what was actually put
in it *this time*, chosen at every refueling) and already have different value sets (six coarse categories vs. ten
precise ones, with no 1:1 mapping — `OTHER` on one side doesn't mean `OTHER` on the other, it means "unfiltered").
Collapsing them into one type would force every call site to know which "mode" a `FuelType` value was being used in.

**2. The filter is a plain function from `VehicleFuelType` to `Set<FuelType>`, called at the UI/processor boundary,
not stored or cached anywhere.** `VehicleFuelType.PETROL -> {REGULAR_PETROL, PREMIUM_PETROL, E85, OTHER}`, `.DIESEL
-> {DIESEL, PREMIUM_DIESEL, BIODIESEL, OTHER}`, `.LPG -> {LPG, OTHER}`, `.CNG -> {CNG, OTHER}`, `.HYDROGEN ->
{HYDROGEN, OTHER}`, `.OTHER -> FuelType.entries.toSet()` (unfiltered). This is pure, total, and needs no persistence
of its own — it's recomputed from whichever vehicle is currently selected, every time that changes.

**3. The remembered global fuel type (`add-refueling-logging`) and the current vehicle's filter are combined at the
same place `LogEventProcessor` already combines the global preference with the opened-state defaults** — extending
the existing `fuelPreferences`/`selected` reactive combination, not a new, separate mechanism. When the globally
remembered type is in the current vehicle's filtered set, it's used as today; when it isn't, the first entry of the
filtered set is used instead, without writing back to the remembered preference (only an actual save does that,
per `refueling-logging`'s own requirement). Because `selected` already updates live when the in-form vehicle
selector changes, switching vehicles while the form is open re-evaluates the filter and, if the currently-chosen
fuel type is no longer offered, re-defaults the same way opening the form fresh would.

**4. `FuelTypeSelector` takes the allowed set as a parameter, not the vehicle.** It stays a dumb selector over
whatever `FuelType` values it's given; `LogEventProcessor`/`LogEventState` computes the filtered list and passes it
down, the same separation of concerns the rest of the form already has (the processor decides what's valid, the
screen just renders the state it's given).

## Risks / Trade-offs

- **A vehicle's fuel type and its actually-logged refueling fuel types can still disagree** (changing a vehicle's
  fuel type after refuelings were logged under a different one, or a genuinely mixed-fuel vehicle using "Other" to
  escape the filter entirely). → Accepted, explicitly: `vehicle-fuel-type`'s own requirement states changing it
  never touches past refuelings, and "Other" exists specifically as the unfiltered escape hatch for exactly this
  case, rather than trying to model bi-fuel vehicles properly.
- **The filtered set for a given `VehicleFuelType` is a static, hand-written mapping** (not derived from `FuelType`
  itself), so adding a new `FuelType` entry later (as `add-adblue-tracking`'s own stub might, or further regional
  fuels) needs a matching, deliberate decision about which `VehicleFuelType` group(s) it joins — it will not
  silently appear nowhere or everywhere. → Acceptable; this is a one-line decision each time, not a structural gap.

## Migration Plan

Additive only: a new column with a default, a new migration (`11.sqm`, following `10.sqm`), no change to any
existing column or stored event. `./gradlew :shared:generateMaestroFixtures` must be re-run once the migration
lands (`docs/test-fixtures.md` — any schema migration makes every checked-in fixture stale). If this needs
reverting, reverting the commit is sufficient — existing vehicles keep reading as "Petrol" either way, and no data
is destroyed by adding or removing the column.
