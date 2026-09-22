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

### Requirement: The final regression run does not include Maestro
The final regression run of a change (the run before it is archived) SHALL be the shared unit and integration tests, the Android debug build and the validation of the specs, and
SHALL NOT include Maestro. While a change is being applied, the manifests of the functionality it touches SHALL be run. The whole Maestro suite SHALL be run only for a major
refactoring, or when the developer asks for it. The project's instructions (`CLAUDE.md`) and the rules for tasks in `openspec/config.yaml` SHALL say so, so that a change's tasks do not ask
for the whole suite.

#### Scenario: A change is made ready to archive
- **WHEN** the tasks of a change that touches the vehicle picture are done and its final regression run is made
- **THEN** the run is the gradle tests and build and the spec validation, and the only Maestro flows run are those of the picture manifest, run while the change was applied

#### Scenario: A major refactoring
- **WHEN** a change restructures navigation, storage or the theme across all screens
- **THEN** its tasks include running all manifests

#### Scenario: A new change is planned
- **WHEN** the tasks of a new change are written
- **THEN** they name the manifests to run for the functionality the change touches and do not include a task to run the whole suite unless the change is a major refactoring
