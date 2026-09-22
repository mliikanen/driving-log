# Tasks

## 1. Fixture generation

- [ ] 1.1 Add a JVM-target Gradle task that builds a named fixture's `.db` file by running
      `SqlDelightVehicleRepository` against SqlDelight's JDBC driver (the same setup
      `SqlDelightVehicleRepositoryTest` already uses) and calling its real methods for that scenario, writing the
      result to `maestro/assets/fixtures/<name>.db`. Verify by generating a first fixture
      (`vehicle-with-log-and-note`: one vehicle, its initial odometer, a couple of distance entries, one with a
      note) and inspecting the produced file with `sqlite3` to confirm the expected rows exist.
- [ ] 1.2 Prepare static picture fixture assets (a vehicle's small/large picture) sized to `vehicle-picture`'s
      conventions (256px / 1024px), checked into `maestro/assets/fixtures/`, and extend the `.db` generator to
      write the matching `picture_id` into the fixture's vehicle row. Verify the generated `.db`'s `picture_id`
      matches the checked-in file's name.

## 2. Device placement

- [ ] 2.1 Write `maestro/seed-fixture.sh <fixture-name>`: `adb shell am force-stop`, `adb shell pm clear`, `adb
      push` the fixture's `.db` and picture files to a staging path, then `run-as ... cp` each into the app's
      database directory and picture-store root, in that order. Verify by running it against a live emulator with
      the debug app installed, then inspecting the app's private storage directly (`adb shell run-as ... ls
      databases/`, `... ls files/pictures/` or wherever the picture store root is) to confirm the files landed.
- [ ] 2.2 Verify end-to-end: after running `seed-fixture.sh vehicle-with-log-and-note`, launch the app manually
      (`adb shell am start ...`, no `clearState` equivalent needed since this is a manual check, not a Maestro
      flow) and confirm the vehicle and its log show up exactly as the fixture defines, without having gone through
      the add-vehicle/log-event UI at all.

## 3. A first fixture-seeded flow

- [ ] 3.1 Convert (or add, alongside the existing UI-driven ones) one Maestro flow whose subject is rendering — a
      good first candidate is a case from `add-event-details-view`'s own coverage, or the row-icon assertions in
      `log-distance.yaml` — to start from `vehicle-with-log-and-note` instead of building that state through the
      UI: a companion `<flow>.fixture` file naming the fixture, and the flow's own `launchApp` using `clearState:
      false`. Verify the flow passes and is meaningfully faster than its UI-driven equivalent (time the run).
- [ ] 3.2 Verify the `clearState: false` requirement is actually enforced by inspection, not just documentation:
      deliberately set `clearState: true` in the converted flow, confirm the flow now fails (the fixture's data is
      gone), then revert. This is the one footgun design.md calls out; confirming it fails loudly (not silently
      passing with empty state) matters more than the happy path.
- [ ] 3.3 Verify no cleanup happens: after the flow passes, inspect the device's data (the app's UI or `adb shell
      run-as ... sqlite3`) and confirm it reflects the flow's own changes, not a wiped or reset state.

## 4. `run.sh` integration

- [ ] 4.1 Teach `maestro/run.sh` to look for a `<flow>.fixture` file next to a flow it is about to run and, if
      present, invoke `seed-fixture.sh <name>` before calling `maestro test` for that flow (extending its existing
      per-flow config-building logic for `run.sh <area> <flow>`). Verify `maestro/run.sh distance
      <fixture-seeded-flow>` runs the seed step then the flow and passes.

## 5. Documentation

- [ ] 5.1 Document the fixture-seeding pattern in `docs/test-strategy.md`, alongside the existing manifest/setup
      section: when to use a fixture vs. build state through the UI, how to add a new fixture, and the `clearState:
      false` requirement called out explicitly. Verify by re-reading it start to finish and confirming a new
      fixture-seeded flow could be written from the doc alone.

## 6. Regression

- [ ] 6.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`;
      confirm both pass. Run `maestro/run.sh distance` (or whichever manifest the converted flow in 3.1 belongs to)
      to confirm the fixture-seeded flow keeps passing alongside the manifest's other, unconverted flows.
