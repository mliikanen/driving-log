# Spec Delta

## REMOVED Requirements

### Requirement: Each kind of test has one job
**Reason**: How the project is built, tested and released belongs in `docs/`, not `openspec/specs/` (`CLAUDE.md`); this capability is retired.
**Migration**: Covered by `docs/test-strategy.md`, "The kinds of test" and "Platform split". The rule is unchanged.

### Requirement: Maestro flows are happy paths
**Reason**: How the project is built, tested and released belongs in `docs/`, not `openspec/specs/` (`CLAUDE.md`); this capability is retired.
**Migration**: Covered by `docs/test-strategy.md`, "Maestro flows are happy paths". The rule is unchanged.

### Requirement: Flows are organized into manifests with one setup
**Reason**: How the project is built, tested and released belongs in `docs/`, not `openspec/specs/` (`CLAUDE.md`); this capability is retired.
**Migration**: Covered by `docs/test-strategy.md`, "Manifests and setup". The rule is unchanged.

### Requirement: A test run shows where each tap lands
**Reason**: How the project is built, tested and released belongs in `docs/`, not `openspec/specs/` (`CLAUDE.md`); this capability is retired.
**Migration**: Covered by `docs/test-strategy.md`, "Manifests and setup" (`run.sh` enables "Show taps"). The rule is unchanged.

### Requirement: A flow whose subject is rendering may start from a seeded fixture
**Reason**: How the project is built, tested and released belongs in `docs/`, not `openspec/specs/` (`CLAUDE.md`); this capability is retired.
**Migration**: Covered by `docs/test-fixtures.md`. The rule is unchanged.

### Requirement: A fixture is placed before the app starts, and its state is left afterward
**Reason**: How the project is built, tested and released belongs in `docs/`, not `openspec/specs/` (`CLAUDE.md`); this capability is retired.
**Migration**: Covered by `docs/test-fixtures.md`, "How a fixture is placed on the device". The rule is unchanged.

### Requirement: The final regression run does not include Maestro
**Reason**: How the project is built, tested and released belongs in `docs/`, not `openspec/specs/` (`CLAUDE.md`); this capability is retired.
**Migration**: Covered by `docs/test-strategy.md`, "What is run when", and `CLAUDE.md`. The rule is unchanged.

### Requirement: CI runs the final regression run on every pull request and push to main
**Reason**: How the project is built, tested and released belongs in `docs/`, not `openspec/specs/` (`CLAUDE.md`); this capability is retired.
**Migration**: Covered by `docs/change-workflow.md`, "Merging", and `CLAUDE.md`. The rule is unchanged.

### Requirement: A pull request cannot be merged until its checks pass
**Reason**: How the project is built, tested and released belongs in `docs/`, not `openspec/specs/` (`CLAUDE.md`); this capability is retired.
**Migration**: Covered by `docs/change-workflow.md`, "Merging" (the rulesets in `.github/rulesets/`). The rule is unchanged.
