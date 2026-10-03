# Spec Delta

## ADDED Requirements

### Requirement: No finding is baselined
The project SHALL NOT keep a baseline for any static analysis tool: no detekt baseline, no Android lint baseline, and
no ktlint baseline. Every finding SHALL be fixed in code, suppressed in code at the narrowest scope with a reason
(see the suppression requirement), or handled by a project-wide rule configuration in the tool's checked-in
configuration with a comment naming the convention it follows. A change that introduces a finding SHALL resolve it in
one of those ways before it is archived.

#### Scenario: A new finding
- **WHEN** a change's code triggers a detekt rule or an Android lint check
- **THEN** the static analysis command fails, and the change fixes the code or suppresses that finding with a reason; there is no baseline to add it to

#### Scenario: A baseline file appears
- **WHEN** a change adds a detekt or Android lint baseline file, or a `baseline` setting to the build
- **THEN** the change is not acceptable as is; the findings it would hide are fixed or individually suppressed instead

#### Scenario: A toolchain update brings new checks
- **WHEN** a detekt, ktlint or Android Gradle Plugin update reports findings in unchanged code
- **THEN** the update's own change fixes them, suppresses each with a reason, or reconfigures the rule with a comment; it does not add a baseline

## REMOVED Requirements

### Requirement: Existing findings are baselined and the baselines only shrink
**Reason**: Every finding that was baselined when the gate was introduced has been fixed, suppressed with a reason,
or handled by a documented rule configuration, so no baseline is left to shrink.
**Migration**: None for code. A finding is now fixed or suppressed (see "No finding is baselined");
`scripts/check-baselines.sh` and the baseline files are removed.
