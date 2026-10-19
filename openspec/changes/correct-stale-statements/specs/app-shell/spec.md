## MODIFIED Requirements

### Requirement: Home screen actions that are not available are disabled
An action that is not available ("Trip" and "Placeholder" until the changes that build them, and "Log event" while the
user has no vehicle, `distance-logging`) SHALL be a
disabled Material 3 component: shown in Material's disabled colors and not in an available action's, not reacting to a
tap, and exposed to a screen reader as disabled, with its name still readable.

#### Scenario: A screen reader reads a disabled action
- **WHEN** a screen reader reaches the "Trip" action
- **THEN** it reads its name, "Trip", and that it is disabled
