# Tasks

## 1. Storage

- [ ] 1.1 A `.sqm` migration adding `hidden_at INTEGER` (nullable) to `vehicle`; `selectVehicles` filters to
      `hidden_at IS NULL`; new queries for hidden vehicles, "any vehicle", every picture id, hide and restore. Read
      `docs/test-fixtures.md` first. Verify: a migration test (a version-10 database migrated keeps its vehicles, all shown;
      its columns match a fresh database's).
- [ ] 1.2 `VehicleRepository`: `observeVehicles()` is visible vehicles; `observeHiddenVehicles()`, `observeHasAnyVehicle()`,
      `allPictureIds()`, `hideVehicle(id)`, `restoreVehicle(id)`; the fake repository follows. Verify: repository unit tests
      (hidden leaves the visible list and joins the hidden one; restore reverses it; hiding changes no event, picture or
      capture; hidden survives reopening the database).
- [ ] 1.3 `sweepPictures` reads `allPictureIds()`. Verify: a unit test that a hidden vehicle's picture survives the sweep.
- [ ] 1.4 Run `./gradlew :shared:generateMaestroFixtures` and check in the regenerated fixtures. Verify:
      `FixtureFreshnessTest` passes.

## 2. Screens

- [ ] 2.1 The details screen: an overflow menu with "Hide vehicle", the confirmation dialog, and back to the list on
      confirm (state and effect in `VehicleDetailsProcessor`). Verify: processor unit tests (open the dialog, cancel
      changes nothing, confirm hides and emits the effect; the dialog's state survives saved state).
- [ ] 2.2 The vehicle list's "Hidden vehicles (n)" entry after the last vehicle, only while any is hidden. Verify:
      processor unit tests (entry shown with the count, not shown with none hidden; a hidden vehicle is not listed).
- [ ] 2.3 The hidden vehicles screen (`HiddenVehiclesNavKey`, processor, screen): the rows, "Restore", back when the last
      is restored; registered with the other keys. Verify: processor unit tests (lists the hidden, restore removes it,
      the last restore emits back); `VehicleNavKeysTest` includes the new key.
- [ ] 2.4 The Home screen: `hasAnyVehicle` for the first tile, `hasVisibleVehicle` for "Log event". Verify:
      `LandingProcessorTest` cases (every vehicle hidden: the first tile opens the list, "Log event" unavailable).
- [ ] 2.5 The log event form's selector with a hidden vehicle and a hidden remembered vehicle. Verify:
      `LogEventProcessorTest` cases (the hidden one is not offered; a hidden remembered one starts on the first visible).

## 3. Verification

- [ ] 3.1 A `vehicles` manifest flow: add two vehicles, hide one from its details screen (cancel once, then confirm),
      see it gone from the list and the selector, restore it from "Hidden vehicles", see it back with its log. Verify:
      `maestro/run.sh vehicles` passes.
- [ ] 3.2 Final regression run: `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all
      --strict`, without Maestro. Verify: both pass.
