# Tasks

## 1. Fixture generation

- [x] 1.1 Add a JVM-target Gradle task that builds a named fixture's `.db` file by running
      `SqlDelightVehicleRepository` against SqlDelight's JDBC driver (the same setup
      `SqlDelightVehicleRepositoryTest` already uses) and calling its real methods for that scenario, writing the
      result to `maestro/assets/fixtures/<name>.db`. Verify by generating a first fixture
      (`vehicle-with-log-and-note`: one vehicle, its initial odometer, a couple of distance entries, one with a
      note) and inspecting the produced file with `sqlite3` to confirm the expected rows exist.

      Revised during implementation: a plain `JavaExec` task hit AGP variant-ambiguity errors resolving the
      compilation's classpath directly. Fixed by writing the generator as an ordinary `@Test`
      (`GenerateFixturesTest`, androidHostTest) that writes files as a side effect, and a `generateMaestroFixtures`
      `Test` task that reuses `testAndroidHostTest`'s own already-resolved `classpath`/`testClassesDirs`, filtered
      to just that one test; `testAndroidHostTest` itself excludes it by name so it never runs as part of the
      ordinary suite. A fixed clock (not wall-clock time) is used throughout, so the fixture's dates are stable
      across regenerations. Verified: `./gradlew :shared:generateMaestroFixtures` writes
      `maestro/assets/fixtures/vehicle-with-log-and-note.db`; inspected with Python's `sqlite3` module (no `sqlite3`
      CLI in this environment) — one vehicle, `INITIAL_ODOMETER`, two `DISTANCE` events, one with `note = "borrowed
      to Sam"`, exactly as scripted.
- [x] 1.2 Prepare static picture fixture assets (a vehicle's small/large picture) sized to `vehicle-picture`'s
      conventions (256px / 1024px), checked into `maestro/assets/fixtures/`, and extend the `.db` generator to
      write the matching `picture_id` into the fixture's vehicle row. Verify the generated `.db`'s `picture_id`
      matches the checked-in file's name.

      `fixture-vehicle-picture-{small,large}.png` (256px/1024px solid color, generated with Pillow), matching
      `FilePictureStore`'s `{pictureId}-{size}.{ext}` naming (PNG is one of `KNOWN_EXTENSIONS`). The `picture_id`
      is written via the real, generated `vehicleQueries.updateVehiclePicture` query — the same one the repository
      calls internally — not a hand-written UPDATE. Verified: regenerated `.db`'s `picture_id` reads
      `fixture-vehicle-picture`, matching the checked-in files exactly.

## 2. Device placement

- [x] 2.1 Write `maestro/seed-fixture.sh <fixture-name>`: `adb shell am force-stop`, `adb shell pm clear`, `adb
      push` the fixture's `.db` and picture files to a staging path, then `run-as ... cp` each into the app's
      database directory and picture-store root, in that order. Verify by running it against a live emulator with
      the debug app installed, then inspecting the app's private storage directly (`adb shell run-as ... ls
      databases/`, `... ls files/pictures/` or wherever the picture store root is) to confirm the files landed.

      Revised during implementation: `pm clear` can leave the app's `databases/`/`files/` directories not yet
      created (Android normally makes them on first launch), so the script `run-as ... mkdir -p databases
      files/pictures` before copying. Every picture fixture under `assets/fixtures/` is pushed alongside every
      named `.db` (today's one fixture shares one vehicle picture; harmless for a future fixture that doesn't need
      it). Verified on the live emulator: `run-as com.mikonoma.drivinglog ls databases/ files/pictures/` shows
      `driving-log.db` and both `fixture-vehicle-picture-{small,large}.png` in place.
- [x] 2.2 Verify end-to-end: after running `seed-fixture.sh vehicle-with-log-and-note`, launch the app manually
      (`adb shell am start ...`, no `clearState` equivalent needed since this is a manual check, not a Maestro
      flow) and confirm the vehicle and its log show up exactly as the fixture defines, without having gone through
      the add-vehicle/log-event UI at all.

      Found and fixed a real bug here: the Home screen's vehicle action stayed permanently disabled after seeding,
      even though the underlying data was intact (confirmed by pulling the on-device `.db` back and inspecting it).
      Root cause: `AndroidSqliteDriver` decides whether to run its own create/migrate callback by reading `PRAGMA
      user_version`; plain `Schema.create()` over the JDBC driver never sets it, so the app re-ran schema creation
      on top of the already-seeded data on first open (`SQLiteLog: table ... already exists`, repeatedly), which
      broke its reactive queries entirely. Fixed by having the generator stamp `PRAGMA user_version =
      DrivingLogDatabase.Schema.version` after `Schema.create()`. Verified on-device after the fix: the Home
      screen's vehicle and "Log event" actions are enabled immediately, "Vehicles" shows "Fixture Car" / "FIX-001"
      with its fixture picture (the solid Oil Slick Blue square) — all without touching the add-vehicle UI.

## 3. A first fixture-seeded flow

- [x] 3.1 Convert (or add, alongside the existing UI-driven ones) one Maestro flow whose subject is rendering — a
      good first candidate is a case from `add-event-details-view`'s own coverage, or the row-icon assertions in
      `log-distance.yaml` — to start from `vehicle-with-log-and-note` instead of building that state through the
      UI: a companion `<flow>.fixture` file naming the fixture, and the flow's own `launchApp` using `clearState:
      false`. Verify the flow passes and is meaningfully faster than its UI-driven equivalent (time the run).

      Added `maestro/distance/view-existing-log.yaml` + `.fixture` (current odometer, row icon, full log, event
      details — the same checks `log-distance.yaml`'s tail already exercises, now against the fixture's vehicle).
      Measured directly per the developer's request: a scratch UI-driven flow doing the identical checks (add
      vehicle, log two entries, then the same assertions) took **105.8s** (`1:45.78` wall-clock, `maestro test`);
      the fixture-seeded flow doing the same checks took **27.8s** — a 3.8x speedup, both passing cleanly.
- [x] 3.2 Verify the `clearState: false` requirement is actually enforced by inspection, not just documentation:
      deliberately set `clearState: true` in the converted flow, confirm the flow now fails (the fixture's data is
      gone), then revert. This is the one footgun design.md calls out; confirming it fails loudly (not silently
      passing with empty state) matters more than the happy path.

      Confirmed: with `clearState: true`, the flow fails immediately at `Tap on "Fixture Car"` ("Element not found");
      reverted to `clearState: false`.
- [x] 3.3 Verify no cleanup happens: after the flow passes, inspect the device's data (the app's UI or `adb shell
      run-as ... sqlite3`) and confirm it reflects the flow's own changes, not a wiped or reset state.

      Confirmed: right after the flow passed, the app is still on the full log screen showing the fixture's data
      (pulled the on-device `.db` back too — vehicle and all 3 events still there, untouched).

## 4. `run.sh` integration

- [x] 4.1 Teach `maestro/run.sh` to look for a `<flow>.fixture` file next to a flow it is about to run and, if
      present, invoke `seed-fixture.sh <name>` before calling `maestro test` for that flow (extending its existing
      per-flow config-building logic for `run.sh <area> <flow>`). Verify `maestro/run.sh distance
      <fixture-seeded-flow>` runs the seed step then the flow and passes.

      Verified: `maestro/run.sh distance view-existing-log` seeds, then runs — `[Passed] view-existing-log (25s)`.

## 5. Documentation

- [x] 5.1 Document the fixture-seeding pattern in `docs/test-strategy.md`, alongside the existing manifest/setup
      section: when to use a fixture vs. build state through the UI, how to add a new fixture, and the `clearState:
      false` requirement called out explicitly. Verify by re-reading it start to finish and confirming a new
      fixture-seeded flow could be written from the doc alone.

      Revised during implementation, per the developer's explicit request: a dedicated `docs/test-fixtures.md`
      (linked from `test-strategy.md`, which keeps only a pointer) instead of inlining everything there, and two
      further additions beyond the original plan:
      - `FixtureFreshnessTest` (new, `shared/src/androidHostTest`): opens every checked-in fixture as part of the
        ordinary test suite and asserts its stamped schema version still matches the current one, so a migration
        that outpaces the fixtures fails fast, here, rather than mysteriously later in a Maestro flow. Verified it
        actually catches staleness (deliberately corrupted a fixture's `PRAGMA user_version`, confirmed the test
        failed, restored via `generateMaestroFixtures`, confirmed it passes again) — not just documented as a risk.
      - A "reuse one fixture for every case it already covers" section and table, per the developer's explicit
        instruction to avoid fixture duplication.
      - `CLAUDE.md` gained a `docs/` entry in Layout, directing a reader to `docs/test-fixtures.md` specifically
        before adding a migration.

## 6. Regression

- [x] 6.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`;
      confirm both pass. Run `maestro/run.sh distance` (or whichever manifest the converted flow in 3.1 belongs to)
      to confirm the fixture-seeded flow keeps passing alongside the manifest's other, unconverted flows.

      Found and fixed a real integration bug here: `manifests/distance.yaml`'s `flows: "distance/*.yaml"` glob
      swept `view-existing-log.yaml` into the whole-manifest run too — which has no seeding step (only `run.sh`'s
      single-flow path does) — so it ran unseeded and failed exactly as expected (`Element not found: Fixture
      Car`), confirming the glob really was the problem. Fixed by listing the manifest's two flows explicitly
      instead of globbing. Verified twice: `maestro/run.sh distance` now runs only its intended 2 flows, both
      passing (`log-distance` 3m16s, `log-from-home` 2m29s); `maestro/run.sh distance view-existing-log` seeds and
      passes on its own (23s). `./gradlew :shared:allTests :androidApp:assembleDebug --rerun-tasks`: `BUILD
      SUCCESSFUL` (includes `FixtureFreshnessTest`). `openspec validate --all --strict`: 17 passed, 0 failed.
