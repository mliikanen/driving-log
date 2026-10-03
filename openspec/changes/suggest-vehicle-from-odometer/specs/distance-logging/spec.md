# Spec Delta

## ADDED Requirements

### Requirement: A scanned reading can suggest which vehicle it belongs to
When the log event form's vehicle selector is shown (the form was opened from the Home screen, not from a specific
vehicle's details screen — `distance-logging`'s "The vehicle is chosen with a selector when logging starts from the
Home screen") and a scanned candidate (`odometer-ocr-capture`) is classified as an odometer reading, the system
SHALL compare it against every vehicle's own current known odometer and, when exactly one vehicle is a plausible
match, offer that vehicle as a suggestion the user can accept or ignore. The system SHALL NOT change which vehicle
the entry is logged against without the user accepting the suggestion. Exactly what "a plausible match" means, and
what happens when more than one vehicle plausibly matches, is not decided by this requirement (see design.md's open
questions) and is left to whoever implements this.

#### Scenario: Exactly one plausible match
- **WHEN** a scanned candidate classified as an odometer reading plausibly matches exactly one vehicle's own current known odometer
- **THEN** that vehicle is offered as a suggestion in the vehicle selector

#### Scenario: Ignoring the suggestion
- **WHEN** the user does not accept an offered suggestion
- **THEN** the vehicle selector keeps whichever vehicle was already chosen, unchanged

#### Scenario: No suggestion without a clear match
- **WHEN** no vehicle plausibly matches, or more than one does
- **THEN** no suggestion is offered
