# Spec Delta

## ADDED Requirements

### Requirement: The spec validation is strict for changes
The system SHALL fail the spec validation on any error or warning in an active change, including a new requirement
over 500 characters, and on any error in the main specs. Warnings in the main specs SHALL be shown without failing it
until none are left. The final regression run and CI SHALL run the same spec validation.

#### Scenario: A change adds a long requirement
- **WHEN** a change adds a requirement whose text is over 500 characters
- **THEN** the spec validation fails and names that requirement

#### Scenario: A change has another warning
- **WHEN** a change's `.openspec.yaml` has a key OpenSpec does not know
- **THEN** the spec validation fails

#### Scenario: An existing long requirement in a main spec
- **WHEN** no change is active and a main spec has a requirement over 500 characters
- **THEN** the spec validation shows the warning and passes

#### Scenario: Locally and in CI
- **WHEN** a developer makes the final regression run on a change's branch
- **THEN** its spec validation is the same check CI's spec-validation job runs

### Requirement: A long requirement can only get shorter
The system SHALL fail the spec validation when a change modifies a requirement so that its text is over 500
characters and longer than the same requirement's text in the main spec. A modified requirement that is shorter than
before, or no longer, SHALL pass.

#### Scenario: A long requirement grows
- **WHEN** a change modifies a requirement of 600 characters so that it has 650
- **THEN** the spec validation fails and names the requirement, its new length and its length in the main spec

#### Scenario: A short requirement grows past the limit
- **WHEN** a change modifies a requirement of 450 characters so that it has 520
- **THEN** the spec validation fails

#### Scenario: A long requirement shrinks
- **WHEN** a change modifies a requirement of 900 characters so that it has 700
- **THEN** that requirement does not fail the spec validation

#### Scenario: Only a scenario is added
- **WHEN** a change modifies a requirement of 900 characters by adding a scenario and leaving its text as it is
- **THEN** that requirement does not fail the spec validation

## MODIFIED Requirements

### Requirement: CI runs the final regression run on every pull request and push to main
The system SHALL run the final regression run (the shared unit and integration tests, the Android debug build, the
static analysis gate and the validation of the specs, without Maestro) automatically in CI for every pull
request targeting `main` and for every push to `main`, and SHALL report the result as status checks on the pull
request or commit: one for the tests and build, one for the static analysis gate, and one for the spec validation. A
failure of any part SHALL fail its check. CI SHALL NOT build or test iOS targets.

#### Scenario: A pull request is opened or updated
- **WHEN** a pull request targeting `main` is opened, or a new commit is pushed to its branch
- **THEN** the regression run runs on that commit and its three status checks are shown on the pull request

#### Scenario: A test fails
- **WHEN** one shared test fails or the debug build fails
- **THEN** the tests-and-build check fails and names the failing step

#### Scenario: A spec is invalid
- **WHEN** the spec validation fails (`test-strategy`, "The spec validation is strict for changes", "A long requirement can only get shorter")
- **THEN** the spec-validation check fails, without waiting for the build

#### Scenario: A linter reports a finding
- **WHEN** ktlint, detekt or Android lint reports a finding
- **THEN** the code-quality check fails and names the tool and the finding

#### Scenario: A pull request is merged
- **WHEN** a pull request is merged into `main`
- **THEN** the regression run runs on the resulting commit on `main` too, and its results are shown on that commit
