# Maestro test fixtures

A Maestro flow whose subject is **how something renders** (a list, a details screen, a row's icon) can start from a
database already in the state it needs, instead of spending most of its steps re-creating that state through the
UI (add a vehicle, log several events). This is `speed-up-tests-with-db-fixtures`'s mechanism. Measured directly:
the same set of rendering checks took **105.8s** built through the UI and **27.8s** started from a fixture — a
~3.8x speedup, for identical checks.

A flow that itself tests the UI path that *builds* a piece of state (adding a vehicle, logging an event) keeps
building it through the UI — that is what it is testing. Fixtures are for flows that only look at what is already
there.

## ⚠️ Fixtures go stale when the database schema changes — read this

Every fixture `.db` file under `maestro/assets/fixtures/` is generated against one specific schema version
(`PRAGMA user_version`, stamped to match `DrivingLogDatabase.Schema.version`). **Adding or changing a `.sqm`
migration makes every checked-in fixture stale**, and the app will not read a fixture whose stamped version doesn't
match its own schema correctly.

You do not have to remember to check this by hand: **`FixtureFreshnessTest`** (`shared/src/androidHostTest/.../
vehicle/fixtures/FixtureFreshnessTest.kt`) runs as part of the ordinary `:shared:allTests`/`testAndroidHostTest`
suite and opens every checked-in fixture, asserting its stamped version still matches the current schema. A
migration that outpaces the fixtures fails this test immediately, with a message naming the fix — not, mysteriously
and much later, a Maestro flow on a device.

**Whenever `FixtureFreshnessTest` fails (or whenever you add a `.sqm` migration), the fix is always the same:**

```sh
./gradlew :shared:generateMaestroFixtures
```

This rewrites every fixture `.db` from the *current* schema and repository code, and stamps the new version. Check
in the result (`git status maestro/assets/fixtures/`) along with the migration that made it necessary.

## Reuse one fixture for every case it already covers

Before adding a new named fixture, check whether an existing one (see the table below) already holds the state a
new flow needs. Add a **case** to an existing flow, or a **new flow that seeds the same fixture**, rather than
generating a near-duplicate fixture with slightly different data — the fewer fixtures there are, the less there is
to keep in sync with the schema, and the more of `GenerateFixtures.kt`'s scenario-building code is shared instead of
copy-pasted per fixture.

| Fixture | Scenario | Used by |
|---|---|---|
| `vehicle-with-log-and-note` | One vehicle ("Fixture Car"), its picture, an initial odometer, two distance entries, one with a note | `maestro/distance/view-existing-log.yaml` |

## Adding a fixture-seeded flow

1. **Reuse an existing fixture if it already fits** (see above). Only add a new one to `GenerateFixtures.kt` (and
   regenerate) if no existing scenario covers what the flow needs to check.
2. Write the flow's `.yaml` as usual, but its `launchApp` step **must use `clearState: false`** (or omit
   `clearState`) — the fixture is placed *before* the app launches, and `clearState: true` would wipe it. This is
   the one thing to get right; there is no way to enforce it mechanically today, only to remember it.
3. Add a companion `<flow-name>.fixture` file next to the flow's `.yaml`, containing exactly one line: the fixture's
   name (matching `maestro/assets/fixtures/<name>.db`, no extension). `run.sh` looks for this file and seeds that
   fixture (`maestro/seed-fixture.sh <name>`) before running the flow.
4. Run it: `maestro/run.sh <area> <flow-name>` seeds and runs it like any other single flow.

## Adding a new fixture scenario

Only when no existing fixture fits (see "Reuse" above):

1. Add a new scenario function to `GenerateFixtures.kt` (`shared/src/androidHostTest/.../vehicle/fixtures/`),
   following the existing one: construct `SqlDelightVehicleRepository` against a real, file-backed SQLite database
   (via SqlDelight's JDBC driver) and call its real methods (`addVehicle`, `addDistanceEntry`, ...) — never
   hand-written `INSERT` SQL, so the fixture's data is exactly as valid as data a real app flow would produce. Use a
   fixed clock throughout (not wall-clock time), so the fixture's dates are stable across regenerations and a flow
   can assert on them literally.
2. If the scenario needs a picture, add static, hand-prepared small/large image files to `maestro/assets/fixtures/`
   (there is no JVM implementation of `ImageCodec`, so a fixture's pictures cannot be produced by the real encode
   pipeline — they only need to be valid, correctly-sized files), and wire the id via the real, generated
   `vehicleQueries.updateVehiclePicture` query.
3. Run `./gradlew :shared:generateMaestroFixtures` and check in the new `.db` (and any new picture files).
4. Add a row to the table above.

## How a fixture is placed on the device

`maestro/seed-fixture.sh <fixture-name>`, in order: `adb shell am force-stop` (the app must not hold the database
open while its file is replaced), `adb shell pm clear` (a known-empty starting point, the same guarantee
`clearState: true` gives a UI-driven flow), then `adb push` to a staging path and `run-as ... cp` into the app's
private storage (a plain `adb push` cannot write there directly) — the database file and every picture fixture
file. Nothing is cleaned up after a fixture-seeded flow finishes; its resulting state (whatever the flow's own
steps changed) is left on the device for inspection, the same as any other flow today.
