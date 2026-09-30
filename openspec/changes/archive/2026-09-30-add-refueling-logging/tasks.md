# Tasks

## 1. Storage and domain model

- [x] 1.1 Add `VehicleEvent.Refueling` (fuel amount, fuel type, "filled up" flag, optional mileage shape mirroring
      `DistanceEntry`/`OdometerAnchor`'s own, note, photoIds). Add the migration: `fuel_amount_milliliters`,
      `fuel_type`, `filled_up` nullable columns on `vehicle_event`, a `'REFUELING'` `type` value, and an
      `insertRefueling` query (see design.md's decision 2 — mileage reuses the existing
      `odometer_meters`/`distance_meters`/`logged_odometer_meters` columns, no new columns for that part). Update
      `toDomain()` to map the new type. Verify with a migration test (schema version bump, existing rows unaffected)
      and a unit test round-tripping a `Refueling` through the repository with each of the three mileage shapes (trip
      distance, new odometer, none).
- [x] 1.2 Add two `FuelUnit` (liters/gallons) and `FuelType` (the fixed ten-item list) domain enums, locale-agnostic
      coded like `OdometerUnit`/`VehicleType`. Add a small two-decimal fuel-amount entry model (design.md decision 5
      — not a reuse of `OdometerEntry`). Verify with unit tests covering entry/formatting for both units and a
      round-trip of every `FuelType` code.
- [x] 1.3 Add `VehicleRepository.addRefueling(vehicleId, amount, fuelType, filledUp, mileage: RefuelingMileage?,
      note, photos)`, implemented in `SqlDelightVehicleRepository`: saves the event, applies the same odometer
      validation and confirmation path as a "Distance"/"New odometer" entry when mileage is given
      (`distance-logging`'s existing requirements, reused unchanged), and does not touch the odometer when it isn't.
      Add `last_fuel_unit`/`last_fuel_type` reads and writes on `app_state`, mirroring `last_logged_vehicle_id`.
      Verify with unit tests: saving with each mileage shape, saving with none, a lower-new-odometer mileage
      triggering the existing confirmation path, and the two new `app_state` preferences persisting and being
      read back independently of any vehicle or event.

## 2. The log event form's Refueling kind

- [x] 2.1 Enable the `LogKind` selector (two entries, both tappable) and, when "Refueling" is chosen, show the fuel
      amount field (with its required-field "*", `refueling-logging`'s "The fuel amount must be entered and above
      zero"), the fuel type selector, the "filled up" checkbox (default checked), and the optional mileage section
      reusing the existing Way/`OdometerEntry` UI. Verify with unit tests: Save stays disabled with an empty fuel
      amount regardless of the mileage section's state, and enables once a valid amount is typed with the mileage
      section still empty.
- [x] 2.2 Wire the fuel unit and fuel type selectors to the new global preferences: preselect the last-used value
      on open, update it on save. Verify with unit tests covering the scenarios in `refueling-logging`'s two
      "remembers the last choice" requirements (defaults to last choice, not derived from the vehicle or its log,
      survives a restart).
- [x] 2.3 Wire save/discard for the refueling fields together with the existing note/photo machinery (already built
      by `add-event-notes`/`add-event-pictures`, reused as-is for this kind). Verify with unit tests: saving stores
      fuel fields, mileage (if any), note and photos as one event; leaving the form without saving discards all of
      it; refueling fields survive a rotation.

## 3. Row display

- [x] 3.1 Extend `EventRowContent`/`EventRow` with a "Refueling" case: its fuel amount (in the unit it was entered
      in) as the row's figure, no plus sign. The existing note/photo icon cluster requires no change (already
      generic to "any event"). Verify with `EventRowContentTest` covering a refueling with/without a note and
      with/without photos.

## 4. Details screen

- [x] 4.1 Extend `EventDetailsContract`/`EventDetailsProcessor`/`EventDetailsScreen` to show a "Refueling" event's
      fuel amount, fuel type, "filled up" state, and its mileage line when present (none shown when it has none).
      Verify with unit tests of the details processor's state for all three mileage shapes.
- [x] 4.2 Confirm the existing "Edit" action is offered for a "Refueling" event and that it exposes only the note
      and photo strip, exactly as it already does for "Distance"/"Odometer reading" — no new UI for the fuel
      fields or mileage. Verify with a unit test that editing a refueling's note through "Edit" leaves its fuel
      amount, fuel type, "filled up" state and mileage exactly as they were.

## 5. Verification

- [x] 5.1 Regenerate the Maestro fixtures (`./gradlew :shared:generateMaestroFixtures`) since the schema changed
      (`docs/test-fixtures.md`). Add a refueling happy-path case to the `distance` manifest (or a dedicated
      `refueling` area, whichever `maestro/run.sh`'s existing structure fits better): log a refueling with an
      amount, a fuel type, a note and a photo, both with and without mileage; verify the row and details screen
      show it correctly and that Edit only ever changes the note/photos. Run the manifest and confirm it passes.
- [x] 5.2 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; confirm
      both pass before archiving.
