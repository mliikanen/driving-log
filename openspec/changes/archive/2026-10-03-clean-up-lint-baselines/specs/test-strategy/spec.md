# Spec Delta

## MODIFIED Requirements

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
