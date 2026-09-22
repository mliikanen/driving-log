# Spec Delta

## MODIFIED Requirements

### Requirement: The vehicle is chosen with a selector when logging starts from the Home screen
When the log event form is opened from the Home screen, the system SHALL show a vehicle selector at the top of the form: a dropdown that shows the chosen vehicle (its picture or icon and its name) and lists every vehicle the user has, in the order of the vehicle list. Choosing a vehicle SHALL make the form
about that vehicle: the previous known odometer, the checks of a new odometer count and the log the entry will join are that vehicle's, and the unit starts as that vehicle's (its unit family and its remembered choice of tenths). The digits already typed, the way (trip distance or new odometer), the date, the time, the time zone and any pending note SHALL be kept when the vehicle is changed, and the typed digits are converted to the new unit as when the unit is changed by hand. The chosen vehicle SHALL survive a rotation of the device and the restart of the process. When the form is opened from a vehicle's details screen the selector SHALL NOT be shown and the vehicle SHALL be that vehicle.

#### Scenario: The selector lists the vehicles
- **WHEN** the user has vehicles named "van", "Bike" and "Family car" and opens the form from the Home screen and opens the selector
- **THEN** it lists "Bike", "Family car" and "van", in that order

#### Scenario: Choose another vehicle
- **WHEN** the user has typed a trip distance for "Family car" and chooses "Bike" in the selector
- **THEN** the form is about "Bike": its previous known odometer is shown, the typed digits are still there, and saving adds the entry to "Bike"

#### Scenario: The unit follows the chosen vehicle
- **WHEN** the user chooses a vehicle whose odometer is in miles after one whose odometer is in kilometers
- **THEN** the unit starts as miles (with that vehicle's remembered choice of tenths) and the typed digits are kept

#### Scenario: From the details screen there is no selector
- **WHEN** the user taps "Log event" on a vehicle's details screen
- **THEN** the form has no vehicle selector and is about that vehicle

#### Scenario: The choice survives a rotation
- **WHEN** the user has chosen a vehicle in the selector and rotates the device
- **THEN** the form shows the same vehicle and what was typed

#### Scenario: A pending note survives a vehicle change
- **WHEN** the user has typed a note, then chooses another vehicle in the selector
- **THEN** the note is still pending for the entry, now about the newly chosen vehicle

## ADDED Requirements

### Requirement: A note can be added to the log event form
The system SHALL show a note element on the log event form, below the trip distance or new odometer field and above
the Save action. Before a note is pending for the entry, the element SHALL prompt "Add a note...". Once a note is
pending, the element SHALL show its text instead, at most two rendered lines (whether the break is a wrap or a line
feed the user typed), end-ellipsized. Tapping the element SHALL open a full-screen note editor. The note element
SHALL NOT show the "Discard" action or otherwise let the user edit the text in place; every edit goes through the
full-screen editor.

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

### Requirement: The full-screen note editor
The system SHALL open a full-screen editor with a single multi-line text field, seeded with whatever note is
currently pending for the entry (empty when none is pending). Back navigation — the editor's own back action, the
system back gesture or button — SHALL attach the field's current text to the entry as its pending note and return to
the log event form; text that is empty or only whitespace SHALL attach as no note (clearing any note that was
pending before the editor opened). The editor SHALL also offer an explicit "Discard" action that returns to the log
event form without attaching anything, leaving the note that was pending before the editor opened exactly as it was.

#### Scenario: Editor starts with the pending note
- **WHEN** the user opens the editor while a note is already pending for the entry
- **THEN** the text field starts with that note's text, ready to continue editing

#### Scenario: Back attaches what was typed
- **WHEN** the user types a note in the editor and navigates back (the toolbar action, the system gesture or the system back button)
- **THEN** the log event form is shown again with that text pending for the entry

#### Scenario: Attaching blank text clears the note
- **WHEN** the user clears the text in the editor (or leaves it as only whitespace) and navigates back
- **THEN** the log event form is shown again with no note pending for the entry

#### Scenario: Discard drops the session's edits
- **WHEN** the user changes the text in the editor and taps "Discard"
- **THEN** the log event form is shown again with the note that was pending before the editor opened, unchanged

### Requirement: A pending note can be removed
While a note is pending for the entry, the system SHALL show a trash-can action on the note element, alongside the
tap-to-edit action. Tapping it SHALL show a confirmation dialog before removing the note. Confirming SHALL clear the
pending note, after which the note element reverts to prompting "Add a note...". Dismissing or cancelling the dialog
SHALL leave the pending note unchanged. The trash-can action SHALL NOT be shown while no note is pending.

#### Scenario: The action appears once a note is pending
- **WHEN** a note is pending for the entry
- **THEN** the note element shows a trash-can action

#### Scenario: No action without a note
- **WHEN** no note is pending for the entry
- **THEN** the note element shows no trash-can action

#### Scenario: Confirming removes the note
- **WHEN** the user taps the trash-can action and confirms the dialog
- **THEN** the pending note is cleared and the note element prompts "Add a note..." again

#### Scenario: Cancelling keeps the note
- **WHEN** the user taps the trash-can action and cancels or dismisses the dialog
- **THEN** the pending note is unchanged and still shown on the note element

### Requirement: A pending note is saved with the event
Saving a valid log event form SHALL store the pending note, if any, with the new event (whichever the form saves: a
distance entry or an odometer anchor). Leaving the form without saving SHALL discard the pending note along with the
rest of the unsaved entry, as it already does for every other field on the form.

#### Scenario: The note is saved with the entry
- **WHEN** the user types a note, enters a valid distance and saves
- **THEN** the log contains the new entry together with that note

#### Scenario: No note means nothing extra is stored
- **WHEN** the user saves a valid entry without ever adding a note
- **THEN** the log contains the new entry with no note

#### Scenario: Leaving the form drops the note too
- **WHEN** the user types a note and leaves the form without saving
- **THEN** no entry is added and the note is not stored anywhere
