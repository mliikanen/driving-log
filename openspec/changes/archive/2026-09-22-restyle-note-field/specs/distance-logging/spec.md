# Spec Delta

## MODIFIED Requirements

### Requirement: A note can be added to the log event form
The system SHALL show a note element on the log event form, below the trip distance or new odometer field and above
the Save action, styled as a Material 3 outlined text field — matching the look of the form's other tap-to-choose
fields (the Kind and Vehicle selectors) — with a "Note" label. Before a note is pending for the entry, the element
SHALL show the field styled as empty, with "Add a note..." shown where the field's content goes. Once a note is
pending, the element SHALL show its text there instead, at most two rendered lines (whether the break is a wrap or a
line feed the user typed), end-ellipsized. Tapping the element SHALL open a full-screen note editor. The note element
SHALL NOT become an editable text field on tap: no text cursor, no software keyboard, and no in-place editing SHALL
appear, and it SHALL NOT show the "Discard" action either; every edit goes through the full-screen editor.

#### Scenario: No note yet
- **WHEN** the user opens the log event form and has typed nothing in the note editor
- **THEN** the note element shows "Add a note..."

#### Scenario: A short note
- **WHEN** a one-line note is pending for the entry
- **THEN** the note element shows that line in place of the prompt

#### Scenario: A long note is truncated to two lines
- **WHEN** a pending note is longer than two rendered lines, whether from wrapping or from line breaks the user typed
- **THEN** the note element shows only its first two rendered lines, the second ending with an ellipsis

#### Scenario: Opening the editor
- **WHEN** the user taps the note element
- **THEN** the full-screen note editor opens

#### Scenario: The element looks like the form's other fields
- **WHEN** the user opens the log event form
- **THEN** the note element is shown as an outlined field with a "Note" label, in the same visual style as the Kind and Vehicle selectors

#### Scenario: Tapping does not turn it into an editable field
- **WHEN** the user taps the note element
- **THEN** no text cursor or software keyboard appears on the log event form, and the full-screen note editor opens instead
