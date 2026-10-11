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
- **WHEN** `openspec validate --all` reports an error
- **THEN** the spec-validation check fails, without waiting for the build

#### Scenario: A linter reports a finding
- **WHEN** ktlint, detekt or Android lint reports a finding
- **THEN** the code-quality check fails and names the tool and the finding

#### Scenario: A pull request is merged
- **WHEN** a pull request is merged into `main`
- **THEN** the regression run runs on the resulting commit on `main` too, and its results are shown on that commit

