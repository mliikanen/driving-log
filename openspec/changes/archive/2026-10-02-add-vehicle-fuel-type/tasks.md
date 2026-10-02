# Tasks

## 1. Vehicle fuel type: domain, storage, migration

- [x] 1.1 Add a `VehicleFuelType` enum (`shared/.../vehicle/domain/VehicleFuelType.kt`): `PETROL`, `DIESEL`, `LPG`,
      `CNG`, `HYDROGEN`, `OTHER`, with `code`/`label` and a `fromCode` treating an unknown code as `OTHER` —
      mirroring `VehicleType.kt` exactly. Verify with a unit test: `fromCode` round-trips every code, an unknown
      code reads as `OTHER`, a null code reads as `OTHER`.
- [x] 1.2 Add `fuel_type` to the `vehicle` table (`TEXT NOT NULL DEFAULT 'PETROL'`) via a new migration `11.sqm`
      (following `10.sqm`); add it to `Vehicle.sq`'s insert/select/update queries and a new
      `updateVehicleFuelType` query mirroring `updateVehicleType`; add `fuelType: VehicleFuelType` to the `Vehicle`
      domain model and its mapping in `SqlDelightVehicleRepository`. Verify with `SqlDelightVehicleRepositoryTest`
      (saving and reading a vehicle's fuel type, updating it) and `VehicleMigrationJvmTest` (a new
      `VERSION_10_SCHEMA`-style constant if one doesn't already cover version 10, and a 10→11 migration test
      asserting an existing row reads `PETROL`).
- [x] 1.3 Re-generate the checked-in Maestro fixtures (`./gradlew :shared:generateMaestroFixtures`) — migration 11
      makes every one stale (`docs/test-fixtures.md`). Verify `FixtureFreshnessTest` passes and
      `git status maestro/assets/fixtures/` shows the regenerated files.

## 2. Add and edit vehicle screens

- [x] 2.1 Add a fuel-type choice to the add vehicle screen (a plain selectable list, no icons needed — like
      `UnitChoice`'s shape, not `VehicleTypeChoice`'s tiles), preselected "Petrol", wired through
      `AddVehicleContract`/`AddVehicleProcessor` (a `FuelTypeSelected` intent, `state.fuelType`). Verify with
      `AddVehicleProcessorTest`: defaults to Petrol, selecting another type changes it, the saved vehicle carries
      the chosen fuel type.
- [x] 2.2 Add the same choice to the edit vehicle screen, showing the vehicle's current fuel type selected and
      applying a change on save. Verify with `EditVehicleProcessorTest`: the current fuel type is preselected,
      changing and saving updates it, and changing only the fuel type leaves name/plate/odometer/unit/picture/type/
      color/log untouched.

## 3. Filtering refueling's fuel type by the vehicle

- [x] 3.1 Add the `VehicleFuelType -> Set<FuelType>` mapping (design.md decision 2): `PETROL` →
      `{REGULAR_PETROL, PREMIUM_PETROL, E85, OTHER}`, `DIESEL` → `{DIESEL, PREMIUM_DIESEL, BIODIESEL, OTHER}`,
      `LPG` → `{LPG, OTHER}`, `CNG` → `{CNG, OTHER}`, `HYDROGEN` → `{HYDROGEN, OTHER}`, `OTHER` → every `FuelType`,
      unfiltered. Verify with a unit test covering all six groups.
- [x] 3.2 Wire `LogEventProcessor` to compute the filtered set from the currently-selected vehicle (reactive to the
      in-form vehicle selector changing, not just the vehicle the form opened with — design.md decision 3) and
      combine it with the remembered global fuel type: use the remembered choice when the current vehicle offers
      it, otherwise the filtered set's first entry, without writing back to the remembered preference. Update
      `FuelTypeSelector` to take and render only the allowed set (design.md decision 4). Verify with
      `LogEventProcessorTest`: the offered set for each `VehicleFuelType`; the remembered choice is used when
      offered; the fallback-to-first-offered case when it isn't; switching the in-form vehicle selector to a
      vehicle with a different fuel type re-evaluates both the offered set and the selected value.

## 4. Maestro

- [x] 4.1 Extend `resilience/rotation.yaml`'s add-form section — which already exercises the picture, name, type,
      color, unit and odometer choices and asserts they survive rotation — with the new fuel-type choice the same
      way, keeping the flow's own stated scope accurate. Run `maestro/run.sh resilience` and confirm it still
      passes.
- [x] 4.2 Run `maestro/run.sh vehicles` and `maestro/run.sh distance`: confirm the add/edit vehicle flows are
      unaffected (the new field is preselected and never blocks Save, like `type`/`color` already are) and the
      refueling fuel type selector still works for whatever vehicle those flows use. Update any flow step that
      taps or asserts a specific default fuel type if the now-filtered default differs from before.

## 5. Verification

- [x] 5.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`;
      confirm both pass before archiving.
