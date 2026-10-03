# Spec Delta

## ADDED Requirements

### Requirement: The build has no compiler or Gradle warnings
Building and testing the project on Linux (the shared module's compilations, the Android app's and their tests) SHALL
produce no Kotlin compiler warning and no Gradle or Gradle plugin deprecation warning. A new warning of either kind
SHALL fail the build, so the final regression run and the CI checks fail on it with no separate step. A compiler
warning SHALL be resolved by fixing the code, or, when the warned-about code is deliberate, by a suppression at the
narrowest scope with a comment giving the reason, under the same rule as a linter finding (see "A suppression states
its reason"). Warnings printed by third-party tools about their own code (not about the project's code or build
configuration) SHALL be listed with their cause in the project's code quality documentation and SHALL NOT fail the
build.

#### Scenario: A new compiler warning
- **WHEN** a change adds code that the Kotlin compiler warns about, for example a call to a deprecated API
- **THEN** the build fails at that compilation, naming the file, line and warning

#### Scenario: A deliberate use of a deprecated API
- **WHEN** code has to keep using a deprecated API, because its replacement can't do what the code needs
- **THEN** the use carries a suppression for that warning at the narrowest scope, with a comment saying why, and the build passes

#### Scenario: A Gradle deprecation
- **WHEN** the build script or a plugin uses a Gradle feature that Gradle reports as deprecated
- **THEN** the build fails, naming the deprecation

#### Scenario: A build script uses a deprecated API
- **WHEN** a Gradle build script uses a deprecated Gradle or plugin API, for example a deprecated DSL block
- **THEN** compiling the script fails the build, naming the script, line and deprecation

#### Scenario: A toolchain update brings new warnings
- **WHEN** a Kotlin, Compose, AGP or other dependency update makes existing code warn
- **THEN** the update's own change resolves the warnings before it can pass the gate

#### Scenario: A tool warns about itself
- **WHEN** a third-party tool run by the build prints a warning about its own internals (for example a JVM warning about the tool's use of `sun.misc.Unsafe`)
- **THEN** the build passes, and the warning is listed, with its cause, in the code quality documentation
