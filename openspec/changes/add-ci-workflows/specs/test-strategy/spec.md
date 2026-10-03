# Spec Delta

## ADDED Requirements

### Requirement: CI runs the final regression run on every pull request and push to main
The system SHALL run the final regression run (the shared unit and integration tests, the Android debug build, the
static analysis gate and the strict validation of the specs, without Maestro) automatically in CI for every pull
request targeting `main` and for every push to `main`, and SHALL report the result as status checks on the pull
request or commit: one for the tests, build and spec validation, and one for the static analysis gate. A failure
of any part SHALL fail its check. CI SHALL NOT build or test iOS targets.

#### Scenario: A pull request is opened or updated
- **WHEN** a pull request targeting `main` is opened, or a new commit is pushed to its branch
- **THEN** the regression run runs on that commit and its two status checks are shown on the pull request

#### Scenario: A test fails
- **WHEN** one shared test fails, the debug build fails, or `openspec validate --all --strict` reports an error
- **THEN** the tests-and-build check fails and names the failing step

#### Scenario: A linter reports a finding
- **WHEN** ktlint, detekt or Android lint reports a finding
- **THEN** the code-quality check fails and names the tool and the finding

#### Scenario: A commit is pushed directly to main
- **WHEN** a commit reaches `main` without a pull request
- **THEN** the regression run runs on it too, and its results are shown on the commit

### Requirement: A pull request cannot be merged until its checks pass
The repository SHALL refuse to merge a pull request into `main` until both CI status checks (tests-and-build and
code-quality) have passed on the pull request's latest commit. A pending or failed check SHALL block the merge,
whoever wrote the pull request, a person or an agent. The repository administrator SHALL keep the ability to push
directly to `main` without a pull request; such commits are checked after they land.

#### Scenario: A check fails
- **WHEN** a pull request's code-quality check or tests-and-build check has failed on its latest commit
- **THEN** the merge button is disabled and merging through the API is refused

#### Scenario: Checks still running
- **WHEN** a pull request's latest commit has a check that hasn't finished
- **THEN** the pull request can't be merged until it finishes and passes

#### Scenario: A fix is pushed
- **WHEN** a commit that fixes the failure is pushed to the pull request's branch and both checks pass on it
- **THEN** the pull request can be merged

#### Scenario: The administrator commits directly to main
- **WHEN** the repository administrator pushes a commit directly to `main`
- **THEN** the push is accepted, and both checks run on that commit afterwards
