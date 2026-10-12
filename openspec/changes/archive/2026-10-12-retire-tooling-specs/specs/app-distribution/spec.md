# Spec Delta

## REMOVED Requirements

### Requirement: A distributed build is signed with the release keystore
**Reason**: How the project is built, tested and released belongs in `docs/`, not `openspec/specs/` (`CLAUDE.md`); this capability is retired.
**Migration**: Covered by `docs/app-distribution.md`, "The release keystore", and `docs/distribution.md`, "One-time setup". The rule is unchanged.

### Requirement: Version name and version code are both derived automatically, never edited by hand
**Reason**: How the project is built, tested and released belongs in `docs/`, not `openspec/specs/` (`CLAUDE.md`); this capability is retired.
**Migration**: Covered by `docs/app-distribution.md`, "Versioning: fully automated, no hand-edited value anywhere". The rule is unchanged.

### Requirement: Release notes MUST be generated from the OpenSpec archive history
**Reason**: How the project is built, tested and released belongs in `docs/`, not `openspec/specs/` (`CLAUDE.md`); this capability is retired.
**Migration**: Covered by `docs/app-distribution.md`, "Release notes: generated from the OpenSpec archive, not typed from scratch", and `docs/distribution.md`, "Cutting a release". The rule is unchanged.

### Requirement: A signed build uploads to the configured tester group
**Reason**: How the project is built, tested and released belongs in `docs/`, not `openspec/specs/` (`CLAUDE.md`); this capability is retired.
**Migration**: Covered by `docs/distribution.md`, "Cutting a release" and "Manual fallback: from your machine". The rule is unchanged.

### Requirement: One-time Firebase project setup is documented, with only the developer's own login left manual
**Reason**: How the project is built, tested and released belongs in `docs/`, not `openspec/specs/` (`CLAUDE.md`); this capability is retired.
**Migration**: Covered by `docs/distribution.md`, "One-time setup" and "One-time CI setup". The rule is unchanged.

### Requirement: Every push to main publishes from CI when there is something to release
**Reason**: How the project is built, tested and released belongs in `docs/`, not `openspec/specs/` (`CLAUDE.md`); this capability is retired.
**Migration**: Covered by `docs/app-distribution.md`, "Publishing from CI", and `docs/distribution.md`, "Cutting a release". The rule is unchanged.
