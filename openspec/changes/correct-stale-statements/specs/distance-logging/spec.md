## ADDED Requirements

### Requirement: The kind of event is chosen from Distance and Refueling
The system SHALL show a "Kind" selector at the top of the log event form: a dropdown in the vehicle selector's style
listing the kinds the form can log, "Distance" and "Refueling" (`refueling-logging`), with "Distance" preselected. Choosing a kind SHALL show that kind's
fields below it.

#### Scenario: Distance is preselected
- **WHEN** the user opens the log event form
- **THEN** "Distance" is selected in the Kind selector and the Distance fields are shown

#### Scenario: Choosing Refueling switches the form
- **WHEN** the user opens the Kind selector and chooses "Refueling"
- **THEN** the form shows the refueling fields (`refueling-logging`, "Refueling can be logged from the log event form")
  in place of the Distance fields

#### Scenario: Sharing the row with the vehicle selector
- **WHEN** the form is opened from the Home screen
- **THEN** the Kind selector and the vehicle selector are shown side by side, each half the row's width and the same height, the Kind selector on the left

#### Scenario: Alone from the details screen
- **WHEN** the form is opened from a vehicle's details screen
- **THEN** the Kind selector is shown alone, the full width of the row, and no vehicle selector is shown

## MODIFIED Requirements

### Requirement: Log an event from the vehicle details screen
The system SHALL offer a "Log event" action on the vehicle details screen that opens a form for one event: a distance entry
(or an odometer anchor) or a refueling (`refueling-logging`).
Saving a valid entry SHALL return to the details screen, where the entry appears in the recent events and the current
odometer includes it. Leaving the form without saving SHALL add nothing. The form's dismiss action, top-left in its
top app bar, SHALL be a close "X" (a Material full-screen dialog's dismiss icon), not a back arrow.

#### Scenario: Open the form
- **WHEN** the user taps "Log event" on a vehicle's details screen
- **THEN** the log event form for that vehicle is displayed

#### Scenario: Save an entry
- **WHEN** the user enters a valid entry and saves
- **THEN** the vehicle's details screen is displayed and shows the new entry among the recent events

#### Scenario: Leave without saving
- **WHEN** the user leaves the form without saving
- **THEN** no entry is added and the vehicle's current odometer is unchanged

#### Scenario: The dismiss action is a close icon
- **WHEN** the user opens the log event form, from a vehicle's details screen or from the Home screen
- **THEN** the top-left action of the form's top app bar is a close "X", not a back arrow

### Requirement: Logging only adds to the log
The system SHALL record each saved distance entry as a new event and SHALL NOT change or remove any existing event
or any stored total, **except the one explicit edit action `event-details`'s "An event's note and photos can be
edited" requirement adds** (changing an already-saved entry's note and photos from its details screen, for any kind of
event but the "Initial odometer"). Saving a new entry SHALL either add it or add nothing.

#### Scenario: Earlier events are untouched
- **WHEN** the user logs a distance entry
- **THEN** every event that was in the log before is still there with the same values

#### Scenario: Editing a note is the one exception
- **WHEN** the user edits a previously logged event's note or photos through its details screen
- **THEN** the log's other events, and the edited event's other fields, are unchanged

### Requirement: A pending note is saved with the event
Saving a valid log event form SHALL store the pending note, if any, with the new event (whichever the form saves: a
distance entry, an odometer anchor or a refueling). Leaving the form without saving SHALL discard the pending note along with the
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

### Requirement: Photos can be added to the log event form
The system SHALL show a photo element on the log event form, below the note element, for a "Distance", "Odometer
reading" (new odometer count) or "Refueling" entry; the "Initial odometer" event never offers one, as it never offers a note. It
SHALL show thumbnails of the photos attached (0 to 5) and, while fewer than 5 are attached, an "Add photo" action that
shows the system's chooser of image sources (`vehicle-picture`). With 5 attached, "Add photo" SHALL NOT be shown until
one is removed.

#### Scenario: No photos yet
- **WHEN** the user opens the log event form and has not attached any photo
- **THEN** the photo element shows only "Add photo"

#### Scenario: Add a photo
- **WHEN** the user taps "Add photo" and chooses a photo from the system chooser
- **THEN** the photo is added to the strip, with no crop screen shown

#### Scenario: The limit is reached
- **WHEN** the user has attached 5 photos to the entry being logged
- **THEN** the strip shows the 5 thumbnails and no "Add photo" action

#### Scenario: Removing one allows another
- **WHEN** the user has attached 5 photos and removes one
- **THEN** the strip shows "Add photo" again

#### Scenario: No photo element for the initial odometer
- **WHEN** a vehicle is added
- **THEN** its "Initial odometer" event offers no photo element

## REMOVED Requirements

### Requirement: The kind of event is chosen
**Reason**: Its rules and two scenarios for a selector listing only one kind describe a state that cannot happen while "Distance" and "Refueling" are both offered, and OpenSpec cannot drop a scenario through MODIFIED.
**Migration**: Replaced by "The kind of event is chosen from Distance and Refueling", which keeps every other rule and scenario.
