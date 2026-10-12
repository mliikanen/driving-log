# Tasks

Every task leaves `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `openspec validate --all`
green. Before touching tests, read `docs/test-strategy.md`; before the migration, read `docs/test-fixtures.md`.

## 1. Domain: atomic events, entry and order

- [ ] 1.1 Add `entryId` to `VehicleEvent` and a `Kind` with `rank` (INITIAL_ODOMETER 0, ODOMETER_ANCHOR 1, DISTANCE 2, REFUELING 3); remove `Refueling.mileage` and `Refueling.odometer`'s anchor branch; keep `RefuelingMileage` as the `addRefueling` input only (design Decisions 1, 5, 6). Verify: `:shared:compileKotlinMetadata` once the callers below compile; KDoc on `note`/`photoIds` says they are the entry's.
- [ ] 1.2 Add the `LogEntry(id, events, note, photoIds)` read model and `logOrder` comparators (oldest first: instant, entry order, kind rank) used by `FakeVehicleRepository`. Verify: a `LogOrderTest` in `commonTest` covers two entries at one instant (entry order wins over kind) and a refueling entry with a mileage event (kind order within the entry).
- [ ] 1.3 Drop the refueling branch from `knownOdometerAt` and update its contract to the new order. Verify: `KnownOdometerTest` gains cases for a refueling entry with a DISTANCE event (counts), an ODOMETER_ANCHOR event (sets), no mileage event (ignored), and the same-minute "trip, then confirmed lower anchor" case (anchor wins); the old refueling-mileage cases are rewritten as entry cases.

## 2. Schema, migration and repository

- [ ] 2.1 Update `VehicleEvent.sq`/`EventPicture.sq` and add `LogEntry.sq` per design Decision 2: `log_entry` table, `vehicle_event.entry_id` (no `note`), `event_picture.entry_id`, `insertLogEntry`, `updateEntryNote`, `touchEntry`, `selectEntryEvents`, and the log queries joining `log_entry` with `ORDER BY occurred_at DESC, log_entry.rowid DESC, <kind rank> DESC`. Verify: `aFreshDatabaseIsVersion…` test renamed to version 13 and asserts the new tables and columns.
- [ ] 2.2 Write `migrations/12.sqm` per the design's Migration Plan (entries with the event's id, table rebuilds, refueling split into `<id>-mileage` DISTANCE/ODOMETER_ANCHOR rows, indexes). Verify: `VehicleMigrationJvmTest` gains the version-12 schema and tests that a refueling with a trip distance, one with an odometer reading and one with none migrate as `vehicle-log`'s "Existing logs are carried over" scenarios say (events, entry, same instant and zone, unchanged current odometer, note and photos on the entry, capture still under the refueling id), and `aMigratedDatabaseHasTheSameTablesAsAFreshOne` migrates 1 → 13.
- [ ] 2.3 Change `SqlDelightVehicleRepository`: every `add*` writes one `log_entry` plus its event(s) in one transaction; `addRefueling` writes the mileage event and the refueling event, photos under the entry and the capture under the event its target names (add a `captureTarget` parameter); `updateEventNote`/`addEventPhoto`/`removeEventPhoto` resolve the event's entry; add `observeEntry`; map rows with the entry's note and photos. Mirror it in `FakeVehicleRepository`. Verify: `SqlDelightVehicleRepositoryTest` covers a refueling with each mileage shape (two events, one entry, shared note and photos, odometer), a failed refueling save storing nothing, a note edit from the mileage event seen on both, the capture landing on the right event per target, and same-minute ordering (`vehicle-log`'s "Events at the same time are ordered by entry, then by kind" scenarios).
- [ ] 2.4 Regenerate the Maestro fixtures (`./gradlew :shared:generateMaestroFixtures`) and check them in. Verify: `FixtureFreshnessTest` passes and `GenerateFixtures.kt` compiles against the new repository.

## 3. Screens

- [ ] 3.1 `LogEventProcessor`: pass the accepted scan's target to `addRefueling`. Verify: `LogEventProcessorTest` asserts the target for a fuel-amount scan and a mileage scan on a refueling.
- [ ] 3.2 `eventRowContent`: a refueling row no longer looks at mileage; note/photo icons come from the entry. Verify: `EventRowContentTest` covers both rows of a refueling entry with a note (both show the note icon; the Distance row shows "+30 km" in the distance accent, the Refueling row "42.3 L").
- [ ] 3.3 `EventDetailsProcessor`/`EventDetailsScreen`/edit screen: read `observeEntry`, show fuel data plus the mileage line from the entry's mileage event, and edit the entry's note and photos. Verify: `EventDetailsProcessorTest` covers opening from the Refueling event and from its Distance event (same state), no mileage line without a mileage event, and editing from the Distance event (the `event-details` delta's scenarios).
- [ ] 3.4 Vehicle details and full log: rows come in the new order and each row opens its entry's details. Verify: `VehicleDetailsProcessorTest`/`VehicleLogProcessorTest` assert a refueling with mileage yields the Refueling row above its Distance row, and two rows count toward the recent list's 5.

## 4. Maestro

- [ ] 4.1 In `maestro/distance/log-distance.yaml`'s second refueling (the one with a trip distance), assert the "Refueling" and "Distance" rows with the same time and tap the Distance row to see the refueling's details. Verify: the flow is valid YAML and the manifest `distance` lists it (running it is the reviewer's check; the manifest to run for this change is `distance`).

## 5. Final regression run

- [ ] 5.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `openspec validate --all`, without Maestro. Verify: both succeed.

## Workflow follow-up

- Reviewer's check: `maestro/run.sh distance` on the `fake` flavor (`docs/change-workflow.md`).
- After review: `/opsx:archive` as the last commit, then merge. Close or rewrite `generalize-vehicle-event-model`.
