# Spec Delta

## ADDED Requirements

### Requirement: CI runs the final regression run on every pull request and push to main
The system SHALL run the final regression run (the shared unit and integration tests, the Android debug build, the
static analysis gate and the strict validation of the specs, without Maestro) automatically in CI for every pull
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
- **WHEN** `openspec validate --all --strict` reports an error
- **THEN** the spec-validation check fails, without waiting for the build

#### Scenario: A linter reports a finding
- **WHEN** ktlint, detekt or Android lint reports a finding
- **THEN** the code-quality check fails and names the tool and the finding

#### Scenario: A pull request is merged
- **WHEN** a pull request is merged into `main`
- **THEN** the regression run runs on the resulting commit on `main` too, and its results are shown on that commit

### Requirement: A pull request cannot be merged until its checks pass
The repository SHALL refuse to merge a pull request into `main` until all three CI status checks (tests-and-build,
code-quality and spec-validation) have passed on the pull request's latest commit. A pending or failed check SHALL block the merge,
whoever wrote the pull request, a person or an agent. `main` SHALL accept changes only through pull requests, from
everyone, the repository administrator included: a direct push to `main` SHALL be refused.

#### Scenario: A check fails
- **WHEN** any of a pull request's three checks has failed on its latest commit
- **THEN** the merge button is disabled and merging through the API is refused

#### Scenario: Checks still running
- **WHEN** a pull request's latest commit has a check that hasn't finished
- **THEN** the pull request can't be merged until it finishes and passes

#### Scenario: A fix is pushed
- **WHEN** a commit that fixes the failure is pushed to the pull request's branch and all three checks pass on it
- **THEN** the pull request can be merged

#### Scenario: Someone pushes directly to main
- **WHEN** anyone, the repository administrator included, pushes a commit directly to `main`
- **THEN** the push is refused, and the change has to go through a pull request
