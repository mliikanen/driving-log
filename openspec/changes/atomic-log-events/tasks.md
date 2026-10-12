# Tasks

Every task leaves `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `openspec validate --all`
green. Before touching tests, read `docs/test-strategy.md`; before the migration, read `docs/test-fixtures.md`.

## 1. Domain: atomic events, entry and order

- [x] 1.1 Merge `VehicleEvent.InitialOdometer` and `OdometerAnchor` into one `OdometerReading` case; add `entryId` and the entry's `source` (`EntrySource.VEHICLE_ADDED`/`LOG_FORM`) to `VehicleEvent` and a `Kind` with `rank` (ODOMETER_READING 0, DISTANCE 1, REFUELING 2); remove `Refueling.mileage` and `Refueling.odometer`'s anchor branch; keep `RefuelingMileage` as the `addRefueling` input only (design Decisions 1, 5, 6). Verify: `:shared:compileKotlinMetadata` once the callers below compile; KDoc on `note`/`photoIds` says they are the entry's.
- [x] 1.2 Add the `LogEntry(id, events, note, photoIds)` read model and `logOrder` comparators (oldest first: instant, entry order, kind rank) used by `FakeVehicleRepository`. Verify: a `LogOrderTest` in `commonTest` covers two entries at one instant (entry order wins over kind) and a refueling entry with a mileage event (kind order within the entry).
- [x] 1.3 Drop the refueling branch from `knownOdometerAt` and update its contract to the new order. Verify: `KnownOdometerTest`'s initial-odometer and anchor cases use `OdometerReading` with each source and still pass, and it gains cases for a refueling entry with a DISTANCE event (counts), an ODOMETER_READING event (sets), no mileage event (ignored), and the same-minute "trip, then confirmed lower anchor" case (anchor wins); the old refueling-mileage cases are rewritten as entry cases.

## 2. Schema, migration and repository

- [x] 2.1 Update `VehicleEvent.sq`/`EventPicture.sq` and add `LogEntry.sq` per design Decision 2: `log_entry` table with `source`, `vehicle_event.entry_id` (no `note`, type `ODOMETER_READING` replacing both odometer types), `event_picture.entry_id`, `insertLogEntry`, `updateEntryNote`, `touchEntry`, `selectEntryEvents`, and the log queries joining `log_entry` with `ORDER BY occurred_at DESC, log_entry.rowid DESC, <kind rank> DESC`. Verify: `aFreshDatabaseIsVersion…` test renamed to version 13 and asserts the new tables and columns.
- [x] 2.2 Write `migrations/12.sqm` per the design's Migration Plan (entries with the event's id, table rebuilds, INITIAL_ODOMETER/ODOMETER_ANCHOR rewritten to ODOMETER_READING with the entry's source, refueling split into `<id>-mileage` DISTANCE/ODOMETER_READING rows, indexes). Verify: `VehicleMigrationJvmTest` gains the version-12 schema and tests that a refueling with a trip distance, one with an odometer reading and one with none migrate as `vehicle-log`'s "Existing logs are carried over" scenarios say (events, entry, same instant and zone, an initial odometer's entry has source VEHICLE_ADDED and an anchor's LOG_FORM, unchanged current odometer, note and photos on the entry, capture still under the refueling id), and `aMigratedDatabaseHasTheSameTablesAsAFreshOne` migrates 1 → 13.
- [x] 2.3 Change `SqlDelightVehicleRepository`: every `add*` writes one `log_entry` (`addVehicle` with source VEHICLE_ADDED, the others LOG_FORM) plus its event(s) in one transaction; `addRefueling` writes the mileage event and the refueling event, photos under the entry and the capture under the event whose number it filled (told by the accepted detection's kind, design Decision 4); `updateEventNote`/`addEventPhoto`/`removeEventPhoto` resolve the event's entry; add `observeEntry`; map rows with the entry's note and photos. Mirror it in `FakeVehicleRepository`. Verify: `SqlDelightVehicleRepositoryTest` covers a refueling with each mileage shape (two events, one entry, shared note and photos, odometer), a failed refueling save storing nothing, a note edit from the mileage event seen on both, the capture landing on the right event per target, and same-minute ordering (`vehicle-log`'s "Events at the same time are ordered by entry, then by kind" scenarios).
- [x] 2.4 Regenerate the Maestro fixtures (`./gradlew :shared:generateMaestroFixtures`) and check them in. Verify: `FixtureFreshnessTest` passes and `GenerateFixtures.kt` compiles against the new repository.

## 3. Screens

- [x] 3.1 `LogEventProcessor`: the accepted scan it passes to `addRefueling` tells which field it filled by its accepted detection's kind (no code change needed, design Decision 4). Verify: `LogEventProcessorTest` asserts the kind for a fuel-amount scan and a mileage scan on a refueling.
- [x] 3.2 `eventRowContent`: an odometer reading is labelled "Initial odometer" when its entry's source is VEHICLE_ADDED and "Odometer reading" otherwise; a refueling row no longer looks at mileage; note/photo icons come from the entry. Verify: `EventRowContentTest` covers both odometer labels, and both rows of a refueling entry with a note (both show the note icon; the Distance row shows "+30 km" in the distance accent, the Refueling row "42.3 L").
- [x] 3.3 `EventDetailsProcessor`/`EventDetailsScreen`/edit screen: read `observeEntry`, show fuel data plus the mileage line from the entry's mileage event, and edit the entry's note and photos; offer "Edit" unless the entry's source is VEHICLE_ADDED. Verify: `EventDetailsProcessorTest` covers no Edit for the initial odometer and Edit for a later odometer reading, opening from the Refueling event and from its Distance event (same state), no mileage line without a mileage event, and editing from the Distance event (the `event-details` delta's scenarios).
- [x] 3.4 Vehicle details and full log: rows come in the new order and each row opens its entry's details. Verify: `VehicleDetailsProcessorTest`/`VehicleLogProcessorTest` assert a refueling with mileage yields the Refueling row above its Distance row, and two rows count toward the recent list's 5.

## 4. Maestro

- [x] 4.1 In `maestro/distance/log-distance.yaml`'s second refueling (the one with a trip distance), assert the "Refueling" and "Distance" rows with the same time and tap the Distance row to see the refueling's details. Verify: the flow is valid YAML and the manifest `distance` lists it (running it is the reviewer's check; the manifest to run for this change is `distance`).

## 5. Final regression run

- [x] 5.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `openspec validate --all`, without Maestro. Verify: both succeed.

## Workflow follow-up

- Reviewer's check: `maestro/run.sh distance` on the `fake` flavor (`docs/change-workflow.md`).
- After review: `/opsx:archive` as the last commit, then merge.
