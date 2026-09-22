# Spec Delta

## ADDED Requirements

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
