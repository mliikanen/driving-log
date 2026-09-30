# Design

## Context

`vehicle_event` (`VehicleEvent.sq`) is append-only today except two narrow exceptions: `updateEventNote` (changes
only the `note` column) and `touchEvent` (a no-op write `add-event-pictures` added purely to force SQLDelight's
reactive-query listeners — which register per table, from the SQL a query literally references — to also fire when
a *different* table, `event_picture`, changes). `VehicleEvent.kt`'s domain model is a closed sealed interface
(`InitialOdometer`, `OdometerAnchor`, `DistanceEntry`), and `KnownOdometer.kt`'s `knownOdometerAt`/`currentOdometer`
are pure functions over the whole ordered event list, fed by `observeLog`'s SQLDelight-backed reactive `Flow` — no
caching anywhere in that chain. `LogEventContract.kt`'s `LogKind` enum already exists specifically to be extended:
its own doc comment states the Kind selector "is disabled until a second one is added by a later change." A small
key-value table, `app_state` (`AppState.sq`), already holds one global, non-vehicle-scoped preference
(`last_logged_vehicle_id`), read/written by `observeLastLoggedVehicleId`/an upsert call in
`SqlDelightVehicleRepository.kt`.

See `proposal.md` for the full behavioral scope; this covers how it's built.

## Goals / Non-Goals

**Goals:**
- Reuse every one of the mechanisms above as-is — the Kind selector's extension point, the Way/`OdometerEntry`
  machinery, the note/photo save-discard machinery, `app_state`, `observeLog`'s reactivity — rather than building
  parallel versions of any of them.
- Keep a future fuel-consumption calculation safely buildable on top of this without inviting a stale-cache bug:
  nothing here stores or incrementally maintains a cross-event aggregate.

**Non-Goals:** everything `proposal.md`'s "Explicitly out of scope" section lists (cost/price, the consumption
calculation itself, both OCR follow-ups, AdBlue, settings-driven fuel-type filtering, the `VehicleEvent` model
rethink) — not restated here.

## Decisions

**1. `Refueling` is a new `VehicleEvent` sealed case, not a field added to `DistanceEntry`/`OdometerAnchor`.**
`note`/`photoIds` could be added to those two types as plain fields because every `DistanceEntry`/`OdometerAnchor`
always has *some* distance or reading. A refueling's mileage is optional, and neither existing type can represent
"no distance, no reading" (their core fields are non-null by construction) — so a refueling with no mileage cannot
be either of them. A new case is the only shape that represents all three states (trip-distance mileage, new-odometer
mileage, no mileage) without contorting an existing one. `generalize-vehicle-event-model` (filed separately) is
where a genuinely different representation gets considered; this proposal builds within today's model.

**2. Refueling's own fields live as new columns on the existing `vehicle_event` table, not a new table.**
Alternative considered: a separate `refueling_details` table (id -> fuel amount, fuel type, filled-up), joined in.
Rejected specifically because of the `event_picture` precedent: `observeLog`/`observeEvent`'s reactive queries only
re-fire on writes to the tables their own SQL references. A side table would silently need the same `touchEvent`
workaround `add-event-pictures` needed — or worse, be forgotten and cause the exact live-staleness bug that was
found and fixed there. Keeping refueling's data on `vehicle_event` itself means every existing reactive consumer
(`currentOdometer`, `knownOdometerAt`, the details screen, the row list, and any future consumption calculation)
sees a refueling write for free, with no new plumbing.
- New nullable columns: `fuel_amount_milliliters INTEGER`, `fuel_type TEXT`, `filled_up INTEGER` (0/1).
- Refueling's *optional mileage* needs no new columns at all: it reuses `odometer_meters` (set when logged as "New
  odometer"), `distance_meters`/`logged_odometer_meters` (set when logged as "Trip distance"), or leaves all three
  null (no mileage) — exactly the same columns `OdometerAnchor`/`DistanceEntry` already use, since a refueling's
  mileage *is* structurally identical to theirs.
- `type` gains a new value, `'REFUELING'`, read the same way `toDomain()` already discriminates the other three.

**3. Fuel unit and fuel type preferences are two new keys in the existing `app_state` table.**
No new table, no schema migration needed for these two specifically (only for the `vehicle_event` columns above) —
`upsertAppState`/`selectAppState` already exist and already support an arbitrary key. `last_fuel_unit` and
`last_fuel_type`, read/written the same way `last_logged_vehicle_id` is, mirror that precedent exactly rather than
inventing a new persistence shape for "a small global preference."

**4. Save-gating is two independent rules, not one generalized "active required field" concept.**
`distance-logging`'s existing rule (empty active field disables Save) and `refueling-logging`'s new rule (empty fuel
amount disables Save, independent of the optional mileage section) are implemented as separate checks in their
respective processor code paths, branching on `state.kind` — `LogEventProcessor` already holds `state.kind`, so this
is a natural extension rather than a new abstraction. Trying to unify them into one "required field" concept was
considered and rejected: the two rules genuinely check different fields for different reasons, and forcing a shared
abstraction over them would obscure that rather than clarify it.

**5. The fuel amount field is its own entry widget, not a reuse of `OdometerEntry`.**
`OdometerEntry`'s microwave-style digit entry is purpose-built for a distance/odometer value with a *unit-driven*
tenths-or-whole-number precision (its `OdometerUnit.hasTenths` flag). A fuel amount always wants two decimal places
regardless of liters-vs-gallons, which doesn't map onto that model. A small, separate two-decimal entry type is used
instead. Generalizing `OdometerEntry` to an arbitrary decimal count was considered and rejected as premature — it
has exactly one caller today (this one), and generalizing a single-caller abstraction ahead of a second real need is
exactly the kind of premature abstraction this project avoids elsewhere.

**6. Editing a saved refueling never touches its fuel fields or mileage — only note and photos, unchanged from
today's Edit scope.** This matches the existing precedent exactly: `vehicle_event` has exactly one update query
today (`updateEventNote`), and no "update a distance" or "update an odometer reading" query exists for any other
kind either. Extending Edit to fuel fields or mileage would be new precedent-breaking machinery this proposal does
not need; `event-details`'s delta states this explicitly so it isn't assumed away later.

## Risks / Trade-offs

- **Discriminating a refueling's mileage shape requires reading `type` alongside nullability of three existing
  columns, the same way telling `DistanceEntry` and `OdometerAnchor` apart already does.** → No new risk: `toDomain()`
  already branches this way for the existing three types; a `'REFUELING'` branch with an inner null-check for
  "which mileage shape, if any" is one more case of an existing, already-exercised pattern.
- **A future consumption-calculation change could be tempted to cache "distance since the last fill-up" for
  performance.** → Documented in `proposal.md` and here: derive it on demand from the full log every time, the same
  way `knownOdometerAt`/`currentOdometer` already are. A cached/incrementally-maintained aggregate would need to
  correctly detect every one of the four change types (an edit to a distance/odometer value, an edit to a refuel
  amount, an added event, a removed event) — any of which can be an arbitrarily-backdated insert with a
  non-local effect on the cached value. Recomputing the whole thing fresh sidesteps that correctness hazard
  entirely, the same way this project already avoids storing any other aggregate.
- **The schema migration invalidates the checked-in Maestro fixtures.** → Known, repeatable step per
  `docs/test-fixtures.md`: `./gradlew :shared:generateMaestroFixtures` after the migration lands.

## Migration Plan

A single additive migration: add `fuel_amount_milliliters`, `fuel_type`, `filled_up` (all nullable) to
`vehicle_event`, bump the schema version. Every existing row gets `NULL` for all three, which `toDomain()` never
reads for a non-`'REFUELING'` row — no backfill needed. No rollback complexity beyond what every prior migration in
this project already has (SQLDelight's forward-only migration mechanism); this is not a destructive or
data-reshaping change to any existing row.
