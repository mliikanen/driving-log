# Spec Delta

## MODIFIED Requirements

### Requirement: Logging only adds to the log
The system SHALL record each saved distance entry as a new event and SHALL NOT change or remove any existing event
or any stored total, **except the one explicit edit action `event-details`'s "A 'Distance' or 'Odometer reading'
event's note can be edited" requirement adds** (changing or clearing an already-saved event's note from its details
screen). Saving a new entry SHALL either add it or add nothing.

#### Scenario: Earlier events are untouched
- **WHEN** the user logs a distance entry
- **THEN** every event that was in the log before is still there with the same values

#### Scenario: Editing a note is the one exception
- **WHEN** the user edits a previously logged event's note through its details screen
- **THEN** the log's other events, and the edited event's other fields, are unchanged
