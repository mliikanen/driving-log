# Tasks

## 1. Storage of the last vehicle logged for

- [x] 1.1 Add migration `6.sqm` (the `app_state` table), `AppState.sq` (select and upsert by key) and bump the schema to 7; verify by the migration test in `VehicleMigrationJvmTest`: a version-6 database with vehicles and events migrates, the vehicles and the log are unchanged and the table is empty
- [x] 1.2 Add `VehicleRepository.observeLastLoggedVehicleId()` and make `addDistanceEntry` and `addOdometerAnchor` upsert `last_logged_vehicle_id` in their transactions (`SqlDelightVehicleRepository` and `FakeVehicleRepository`); verify by `SqlDelightVehicleRepositoryTest`: nothing remembered on a new database, a saved entry from either way remembers its vehicle, **a backdated entry still makes its vehicle the remembered one** (the point of not deriving it), the last save wins, a failed insert leaves the memory as it was, and the flow emits when it changes

## 2. The log form with a vehicle chosen

- [x] 2.1 Extract one vehicle order (case-insensitive by name) shared by the vehicle list and the selector; verify the list's existing ordering tests still pass and add a test for the shared comparator
- [ ] 2.2 Give `LogDistanceState` `selectedVehicleId`, `vehicles` and `unitFor`, and the intent `VehicleSelected`; make `LogDistanceProcessor` choose the vehicle (restored, else the remembered, else the first by name), observe by `flatMapLatest` on it, initialise the unit again when the vehicle changes (keeping the digits, the way, the moment and the zone) and save for the chosen vehicle; verify by `LogDistanceProcessorTest`: each of the three ways the first vehicle is chosen, a remembered vehicle that is gone, switching keeps digits and converts them to the other vehicle's unit, the known odometer and the check of a new odometer count follow the switch, saving adds to the chosen vehicle, the chosen id survives `restoreState`, and the details route (a fixed id) has no selection and behaves as before
- [ ] 2.3 Add the selector to `LogDistanceScreen` (`ExposedDropdownMenuBox`, the vehicle's picture or icon, name and plate, tag `log_vehicle_selector`), shown only when the form was opened without a vehicle; verify it compiles for Android and iOS and by hand on the emulator (light and dark, portrait and landscape) that it opens, lists the vehicles in order and changes the vehicle

## 3. The Home tile

- [ ] 3.1 Enable the tile named "Log event" in `landingTiles` when there is a vehicle (disabled otherwise, exactly like the other placeholder tiles: no extra hint or semantics added), add the effect `ShowLogEvent` and navigate to `LogDistanceNavKey(graph, "")`; verify by the landing tests (enabled with a vehicle and not without, the effect) and on the emulator that saving returns to the Home screen and back navigation from the form does too

## 4. Flows and final verification

- [ ] 4.1 Add `maestro/distance/log-from-home.yaml` (two vehicles: the Home screen's Log event action, the selector, choose the other vehicle and save, the Home screen; open again and the chosen vehicle is preselected; log from the other vehicle's details, which has no selector, and the Home route now preselects it) and list it in `manifests/distance.yaml`; verify by running the `distance` manifest
- [ ] 4.2 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`, and verify all pass (the `distance` manifest was run in 4.1; Maestro is not part of the final regression run)
