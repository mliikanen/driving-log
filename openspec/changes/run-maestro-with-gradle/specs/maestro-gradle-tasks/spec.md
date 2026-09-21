# Spec Delta

## Purpose
Says how the Maestro manifests and device-state groups are run from Gradle: one task each and aggregates, what they depend on, how a missing CLI or device is reported, where their reports go,
and that they are never part of the regression run, so that running the UI flows of a change is one command that builds and installs the app first.

## ADDED Requirements

### Requirement: Each manifest and device-state group has a Gradle task
The build SHALL provide, in the Gradle task group `maestro`, one task for each manifest in `maestro/manifests/` (named `maestro` followed by the manifest's name with a capital initial, for example `maestroVehicles`),
one for each device-state group, that is each `maestro/<area>/` directory with a `run.sh` (`maestroPicture`, `maestroTheme`, `maestroClock`), and the aggregate tasks `maestroAll` (every manifest, in order) and
`maestroDeviceState` (every device-state group). A manifest task SHALL run the Maestro command line tool with the manifest as its configuration file (`--config`) and the `maestro/` directory as the workspace,
so that the flows run in the order the manifest gives, the setup flow first. A new manifest file or device-state group SHALL get its task without the build being edited.

#### Scenario: A manifest is run
- **WHEN** a developer runs `./gradlew maestroVehicles` with a device connected
- **THEN** the flows of `maestro/manifests/vehicles.yaml` run in the manifest's order with the debug app installed, and the build succeeds when all pass and fails when one fails

#### Scenario: A manifest is added
- **WHEN** a file `maestro/manifests/fuel.yaml` is added
- **THEN** a task `maestroFuel` exists in the group `maestro` without any change to a build file

#### Scenario: A device-state group is run
- **WHEN** a developer runs `./gradlew maestroTheme`
- **THEN** the group's `run.sh` runs with the adb of the Android SDK the build uses, and the build fails when the script fails

#### Scenario: Everything is run
- **WHEN** a developer runs `./gradlew maestroAll`
- **THEN** every manifest task runs one after another, in a fixed order, and none runs at the same time as another

### Requirement: One flow of a manifest can be run
A manifest task SHALL accept an option `--flow=<name>` that runs the manifest's setup flow (when it has one) and then only that flow, in the manifest's area, and SHALL fail with the names of the
area's flows when there is no such flow.

#### Scenario: One flow
- **WHEN** a developer runs `./gradlew maestroVehicles --flow=edit`
- **THEN** the area's setup (if any) and then only `maestro/vehicles/edit.yaml` run

#### Scenario: A flow that does not exist
- **WHEN** a developer runs `./gradlew maestroVehicles --flow=nosuch`
- **THEN** the task fails before starting Maestro and lists the flows of `maestro/vehicles/`

### Requirement: The tasks are built for a device and say what is missing
A manifest or device-state task SHALL run only after the debug app has been installed (`:androidApp:installDebug`, which a developer can skip with Gradle's own task exclusion) and after the test photos earlier runs left on
the device have been removed, and SHALL be preceded by a check that the Maestro command line tool is installed and that exactly one device is connected (or the one named by `ANDROID_SERIAL`). A failed check SHALL fail the build
with a message that says what is missing and how to provide it; a task SHALL NOT be skipped or pass when no test ran. A task SHALL never be reported up to date, and tasks SHALL NOT use the same device at the same time.

#### Scenario: No device
- **WHEN** a developer runs `./gradlew maestroVehicles` and no emulator or device is connected
- **THEN** the build fails before installing anything, with a message that no device is connected and that an emulator has to be started or a device attached

#### Scenario: Maestro is not installed
- **WHEN** the `maestro` command is not found
- **THEN** the build fails with a message that says Maestro is required and where to install it from, and nothing is installed on the device

#### Scenario: The same task twice
- **WHEN** `./gradlew maestroDistance` is run twice in a row with nothing changed
- **THEN** the flows run both times

#### Scenario: Stale app
- **WHEN** the app's code has changed since the last install and a manifest task is run
- **THEN** the debug app is rebuilt and installed before the flows run

### Requirement: The tasks are never part of the regression run
No task of the group `maestro` SHALL be a dependency of `check`, `build`, `test`, `allTests`, `assemble` or any other lifecycle task, so that `./gradlew :shared:allTests :androidApp:assembleDebug` and `./gradlew check` never run Maestro, in keeping with the
test strategy. The tasks SHALL exist only in the project that builds the app.

#### Scenario: The regression run
- **WHEN** `./gradlew check --dry-run` or `./gradlew :shared:allTests :androidApp:assembleDebug --dry-run` lists the tasks that would run
- **THEN** no task of the group `maestro` and no Maestro process is among them

### Requirement: Reports are written to the build directory
Each manifest task SHALL write Maestro's JUnit report and its test output (screenshots and logs) to `build/reports/maestro/<area>/` of the app project, and the task's log SHALL say where they are, also
when the run fails. Device-state groups keep their own output.

#### Scenario: A run fails
- **WHEN** a flow of a manifest fails
- **THEN** the build fails and `build/reports/maestro/<area>/` holds the JUnit report of the run and Maestro's output for the failing flow, and the message names that directory

### Requirement: Compatibility with the build
The tasks SHALL work with the build's Gradle version, with the configuration cache and the build cache on (both are on in `gradle.properties`) and the Kotlin DSL and version catalog the build uses, and SHALL need no Gradle plugin
from outside the project. The Maestro command line tool SHALL remain an external installation whose version is not managed by the build; the flows and manifests SHALL be unchanged by this capability.

#### Scenario: Configuration cache
- **WHEN** `./gradlew maestroVehicles` is run twice with the configuration cache on
- **THEN** the second run reuses the cached configuration, and both runs execute the flows
