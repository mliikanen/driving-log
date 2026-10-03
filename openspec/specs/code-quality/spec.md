# code-quality Specification

## Purpose
Defines the static analysis the project enforces on its source code (formatting, code smells and Android lint), how
findings in existing code are handled, and how a finding may be suppressed, so every change is checked the same way
whoever wrote it.

## Requirements

### Requirement: One command runs every static analysis gate
The project SHALL provide one Gradle command that runs the Kotlin formatting check, detekt and Android lint, and
fails when any of them reports a finding. The command SHALL cover every Kotlin source
set of the shared module (common, Android, iOS and test code) and of the Android app, and every Gradle Kotlin script.
It SHALL NOT need a macOS host: iOS source sets are checked by the Kotlin formatting check and detekt, which only read
the sources.

#### Scenario: Clean code
- **WHEN** the static analysis command runs on code with no findings
- **THEN** it succeeds

#### Scenario: A formatting violation
- **WHEN** a Kotlin file or Gradle Kotlin script is not formatted to the project's style
- **THEN** the command fails and names the file, line and rule

#### Scenario: A new detekt finding
- **WHEN** code added by a change triggers a detekt rule
- **THEN** the command fails and names the file, line and rule

#### Scenario: A new Android lint warning
- **WHEN** a change introduces an Android lint warning or error in the app's debug variants of either flavor
- **THEN** the command fails, since warnings are treated as errors, and the lint report names the issue

#### Scenario: Run on Linux
- **WHEN** the command runs on a Linux machine without Xcode
- **THEN** it checks the iOS source sets' Kotlin with the formatting check and detekt, and does not fail for lack of iOS tooling

### Requirement: Formatting is fixed by a tool, not by hand
The project SHALL provide a command that rewrites Kotlin files and Gradle Kotlin scripts to the project's formatting
style. The style SHALL be defined in one checked-in configuration file that the IDE reads too, so the IDE's own
formatter and the check agree.

#### Scenario: A developer fixes formatting
- **WHEN** the formatting check fails and the developer runs the format command
- **THEN** every finding that can be fixed automatically is fixed in place, and the check then reports only the findings that need a manual fix

### Requirement: A suppression states its reason
A finding SHALL be suppressed only in code, at the narrowest scope that covers it (an expression, a declaration or
a file, never a whole module through configuration), and the suppression SHALL carry a comment explaining why the
rule does not apply there. Disabling or loosening a rule for the whole project SHALL be done only in the checked-in
tool configuration, with a comment giving the reason.

#### Scenario: A rule does not fit one place
- **WHEN** a detekt rule flags code that is correct as written
- **THEN** the code carries a suppression for that rule at the narrowest scope, with a comment saying why

#### Scenario: A rule does not fit the project
- **WHEN** a rule conflicts with a project convention everywhere (for example, function naming for Compose functions)
- **THEN** the rule is configured or disabled in the tool's configuration file, with a comment naming the convention

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
