# test-strategy Specification

## Purpose
Says what each kind of test in the project is for, how the end-to-end flows are organized and set up, and which tests are run when, so that the suite
stays fast enough to be run and every check has one owner.

## Requirements

### Requirement: Each kind of test has one job
The project SHALL document in `docs/test-strategy.md` what each kind of test is for, and SHALL put a check in the lowest kind that can check it:
pure unit tests SHALL check many input/output variations of small pieces of code (rules, formatting, parsing, the processors' logic); Compose integration
tests SHALL exercise MVI, a screen together with its processor; screenshot tests SHALL make the structure of the UI change only intentionally; Maestro flows
SHALL check the happy path of a feature end to end. The document SHALL state which of these kinds exist in the project today and which are still to be adopted.

#### Scenario: A validation rule is checked
- **WHEN** a rule has several input cases (an empty name, a whitespace name, a count that is not higher than the known odometer, a future date)
- **THEN** each case is a unit test, and no Maestro flow repeats it

#### Scenario: A screen's behavior is checked
- **WHEN** a screen's reaction to an intent is checked (a field's error appears, a choice is kept)
- **THEN** it is checked with the screen's processor (a Compose integration test once that kind exists, the processor's unit test until then), not by a flow

#### Scenario: The document states the platform split
- **WHEN** a developer reads where each kind of test lives
- **THEN** the document says that pure unit tests are shared and run on every platform, that Robolectric-based tests (Compose integration and screenshot tests) are Android-only, and where they go in the source tree, even if no such tests exist yet

#### Scenario: The document names what is not there yet
- **WHEN** a kind of test is described in the document but is not yet part of the build
- **THEN** the document says so and names the change that adds it

### Requirement: Maestro flows are happy paths
Every Maestro flow SHALL follow one journey of a feature from start to finish as the user does it, and SHALL NOT be written to check cases, error messages
or combinations of inputs. Several flows for one feature are allowed. A flow that repeats a journey another flow already follows SHALL be merged into it or removed, and an
assertion SHALL be removed from a flow only when the test that checks it in a lower kind exists.

#### Scenario: Two flows follow the same journey
- **WHEN** two flows both add a vehicle, restart the app and check it is still there
- **THEN** there is one flow for it, and the other is removed or its distinct steps are merged into the first

#### Scenario: A flow is asked to check a case
- **WHEN** a flow would check that saving with an empty name is refused
- **THEN** that check is a unit test of the processor and the flow only follows the successful path

#### Scenario: An assertion is removed from a flow
- **WHEN** an assertion is taken out of a flow because a lower test owns it
- **THEN** that test exists and passes before the assertion is removed

### Requirement: Flows are organized into manifests with one setup
The Maestro flows SHALL be grouped by feature area into manifests, each a Maestro configuration file that lists its flows and their order. Each manifest that
needs test data on the device (the photos the flows use) SHALL upload it once, in a setup flow that runs first, and its other flows SHALL NOT upload it again. A
fresh emulator with the debug app installed SHALL be all a manifest needs, and one command SHALL run a manifest, several manifests or all of them.

#### Scenario: A manifest with photos is run
- **WHEN** a manifest whose flows choose photos is run on a fresh emulator
- **THEN** its setup flow uploads the photos once, before its first flow, and every flow of the manifest finds them in the picker

#### Scenario: A flow of a manifest is run alone
- **WHEN** a developer wants one flow of a manifest
- **THEN** the documented command runs the manifest's setup and then that flow

#### Scenario: Old photo copies
- **WHEN** the emulator holds many copies of the test photos from earlier runs
- **THEN** the run command clears them before the setup flow uploads the photos again

### Requirement: A test run shows where each tap lands
The system SHALL enable the device's visual touch indicator ("Show taps") once at the start of a `run.sh`
invocation, before any manifest's flows run, regardless of how many areas or flows that invocation covers. The
system SHALL NOT disable it again after the run finishes.

#### Scenario: One invocation, several manifests
- **WHEN** `run.sh` is invoked with more than one manifest (or `--all`)
- **THEN** the touch indicator is enabled once, before the first manifest's flows run, not once per manifest or per flow

#### Scenario: `adb` unavailable
- **WHEN** `run.sh` runs without `adb` available
- **THEN** enabling the touch indicator is skipped the same way the existing test-photo cleanup already is, with a message, and the run continues

### Requirement: A flow whose subject is rendering may start from a seeded fixture
The project SHALL let a Maestro flow whose subject is how already-stored data is shown (not the UI path that
produces that data) start from a fixture already placed on the device's private storage, instead of building that
state by driving the UI. A fixture's database rows SHALL be generated by running the app's real repository code
(off-device, on the JVM), never hand-written SQL, so fixture data is exactly as valid as data a real app flow would
produce. A fixture's picture files SHALL be valid, correctly-sized image files; they need not have passed through
the app's own image-encoding pipeline. A flow that itself tests the UI path that builds a piece of state (adding a
vehicle, logging an event) SHALL continue to build that state through the UI, not from a fixture.

#### Scenario: A rendering-focused flow uses a fixture
- **WHEN** a flow's subject is how the recent-events list or the event details screen renders a vehicle's existing log
- **THEN** the flow starts from a fixture already holding that vehicle and its log, and does not add them through the UI first

#### Scenario: A flow testing the add path still uses the UI
- **WHEN** a flow's subject is adding a vehicle or logging an event through the form
- **THEN** the flow builds that state through the UI as it does today, fixtures are not used for it

#### Scenario: Fixture data is never hand-written SQL
- **WHEN** a fixture's database file is produced
- **THEN** it is generated by calling the real repository's methods, not by writing `INSERT` statements by hand

### Requirement: A fixture is placed before the app starts, and its state is left afterward
The system SHALL, for a fixture-seeded flow, stop the app if it is running, clear its state, place the fixture's
database and picture files into the app's private storage, and only then launch the app — in that order, so the app
never has its database file replaced while holding it open and never starts before the fixture is in place. The
flow's own launch step SHALL NOT clear state again, since that would remove what was just placed. The system SHALL
NOT clean up a fixture-seeded flow's resulting state after the flow finishes; whatever the flow's own steps changed
SHALL remain on the device for inspection.

#### Scenario: The app is stopped before its files are replaced
- **WHEN** a fixture-seeded flow starts and the app is already running from an earlier step
- **THEN** the app is stopped before the fixture's files are placed

#### Scenario: The flow's own launch does not clear state
- **WHEN** a fixture-seeded flow launches the app after its fixture is placed
- **THEN** the launch does not clear state, and the app starts showing the fixture's data

#### Scenario: Nothing is cleaned up afterward
- **WHEN** a fixture-seeded flow finishes, whether it passed or failed
- **THEN** the device's data reflects exactly what the flow's own steps left, available for inspection until a later run replaces it

### Requirement: The final regression run does not include Maestro
The final regression run of a change (the run before it is archived) SHALL be the shared unit and integration tests, the Android debug build, the static analysis gate (the Kotlin formatting check, detekt and Android lint, see the code-quality capability) and the validation of the specs, and
SHALL NOT include Maestro. While a change is being applied, the manifests of the functionality it touches SHALL be run. The whole Maestro suite SHALL be run only for a major
refactoring, or when the developer asks for it. The project's instructions (`CLAUDE.md`) and the rules for tasks in `openspec/config.yaml` SHALL say so, so that a change's tasks do not ask
for the whole suite.

#### Scenario: A change is made ready to archive
- **WHEN** the tasks of a change that touches the vehicle picture are done and its final regression run is made
- **THEN** the run is the gradle tests and build, the static analysis gate and the spec validation, and the only Maestro flows run are those of the picture manifest, run while the change was applied

#### Scenario: A major refactoring
- **WHEN** a change restructures navigation, storage or the theme across all screens
- **THEN** its tasks include running all manifests

#### Scenario: A new change is planned
- **WHEN** the tasks of a new change are written
- **THEN** they name the manifests to run for the functionality the change touches and do not include a task to run the whole suite unless the change is a major refactoring

#### Scenario: Static analysis fails
- **WHEN** the tests and build pass but the static analysis gate reports a finding
- **THEN** the final regression run fails, and the change is not ready to archive
