# Spec Delta

## ADDED Requirements

### Requirement: A "Distance" or "Odometer reading" event's note can be edited
The system SHALL show an "Edit" action on the details screen (`event-details`, "A logged event's details can be
viewed") of a "Distance" or "Odometer reading" event, and SHALL NOT show it for an "Initial odometer" event, which
can never carry a note. Tapping "Edit" SHALL open the same full-screen note editor `distance-logging`'s "The
full-screen note editor" requirement describes, seeded with the event's current note (empty when it has none).
Navigating back from the editor SHALL save the typed text as the event's note immediately, attaching blank or
whitespace-only text as no note (clearing any note the event had); the editor's "Discard" action SHALL return to the
details screen without changing the event's note.

#### Scenario: Edit is offered for a note-capable event
- **WHEN** the user opens the details screen of a "Distance" or "Odometer reading" event
- **THEN** the screen shows an "Edit" action

#### Scenario: Edit is not offered for the initial odometer event
- **WHEN** the user opens the details screen of the "Initial odometer" event
- **THEN** the screen shows no "Edit" action

#### Scenario: Add a note to an event that had none
- **WHEN** the user taps "Edit" on an event with no note, types a note and navigates back
- **THEN** the details screen shows that note, and the event's row in the log now shows the note icon

#### Scenario: Change an existing note
- **WHEN** the user taps "Edit" on an event that already has a note, changes the text and navigates back
- **THEN** the details screen shows the changed note

#### Scenario: Clear a note by editing it blank
- **WHEN** the user taps "Edit" on an event with a note, clears the text and navigates back
- **THEN** the details screen shows no note, and the event's row no longer shows the note icon

#### Scenario: Discard drops the edit
- **WHEN** the user taps "Edit", changes the text and taps "Discard"
- **THEN** the details screen shows the event's note exactly as it was before "Edit" was tapped
