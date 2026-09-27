# event-details Specification

## Purpose
Shows everything the system currently stores about one logged event, read-only, re-observed live from the log, once
tapped from a vehicle's recent events or full log.

## Requirements

### Requirement: A logged event's details can be viewed
The system SHALL open a read-only details screen for an event when its row is tapped, for every event kind. The
screen SHALL show the event's kind, its full date, time and time zone (as the row itself already does), its figure
(the reading or distance the row shows), and its note in full, untruncated, when it has one. An "Initial odometer"
event SHALL show no note section, since it can never carry one. Going back SHALL return to whichever list opened it.

#### Scenario: Open a distance event's details
- **WHEN** the user taps a "Distance" event's row in the recent events or the full log
- **THEN** the details screen shows that event's date, time, time zone, distance and note (if any), in full

#### Scenario: Open the initial odometer event's details
- **WHEN** the user taps the "Initial odometer" event's row
- **THEN** the details screen shows its date, time, time zone and reading, and no note section

#### Scenario: Back returns to the list
- **WHEN** the user navigates back from the details screen
- **THEN** the list that opened it is shown again, as it was

### Requirement: The details screen reflects the event live
The system SHALL show the details screen's content from the event as currently stored, not from data captured at the
moment the row was tapped, so that a change made elsewhere to the same event (once such a change is possible) is
reflected without the user having to navigate away and back.

#### Scenario: The screen is not stale
- **WHEN** the details screen for an event is open
- **THEN** its content is read from the event's current stored state, not from a snapshot taken when the screen opened

### Requirement: The details screen shows a "Distance" or "Odometer reading" event's photos
The system SHALL show, on the details screen of a "Distance" or "Odometer reading" event that has one or more
photos, a thumbnail for each, in the order they were attached. Tapping a thumbnail SHALL open a full-size viewer for
that photo. An event with no photo SHALL show no photo section.

#### Scenario: Thumbnails are shown
- **WHEN** the user opens the details screen of an event with 3 photos
- **THEN** the screen shows 3 thumbnails, in the order they were attached

#### Scenario: View a photo full-size
- **WHEN** the user taps one of the thumbnails
- **THEN** a full-size viewer opens showing that photo

#### Scenario: No photo section without photos
- **WHEN** the user opens the details screen of an event with no photo
- **THEN** the screen shows no photo section

### Requirement: A "Distance" or "Odometer reading" event's note can be edited
The system SHALL show an "Edit" action on the details screen of a "Distance" or "Odometer reading" event, and SHALL
NOT show it for an "Initial odometer" event, which can never carry a note or a photo. Tapping "Edit" SHALL open an
edit screen showing the event's note element (tap it to open the same full-screen note editor
`distance-logging`'s "The full-screen note editor" requirement describes, seeded with the event's current note) and
its photo strip (the same element `distance-logging`'s "Photos can be added to the log event form" requirement
describes, seeded with the event's current photos, up to the same 5-photo cap, each removable with the same
confirmation). The edit screen SHALL offer an explicit "Save" action that stores the note text and the resulting set
of photos together, and leaving the edit screen without saving SHALL leave the event's note and photos exactly as
they were.

#### Scenario: Edit is offered for a note-capable event
- **WHEN** the user opens the details screen of a "Distance" or "Odometer reading" event
- **THEN** the screen shows an "Edit" action

#### Scenario: Edit is not offered for the initial odometer event
- **WHEN** the user opens the details screen of the "Initial odometer" event
- **THEN** the screen shows no "Edit" action

#### Scenario: Add a note to an event that had none
- **WHEN** the user taps "Edit" on an event with no note, opens the note element, types a note, returns to the edit screen and taps "Save"
- **THEN** the details screen shows that note, and the event's row now shows the note icon

#### Scenario: Change an existing note
- **WHEN** the user taps "Edit" on an event that already has a note, changes the text through the note element, returns to the edit screen and taps "Save"
- **THEN** the details screen shows the changed note

#### Scenario: Clear a note by editing it blank
- **WHEN** the user taps "Edit" on an event with a note, clears the text through the note element, returns to the edit screen and taps "Save"
- **THEN** the details screen shows no note, and the event's row no longer shows the note icon

#### Scenario: Discard drops the edit
- **WHEN** the user taps "Edit", changes the note or the photos, and leaves the edit screen without tapping "Save"
- **THEN** the details screen shows the event's note and photos exactly as they were before "Edit" was tapped

#### Scenario: Add a photo to a saved event
- **WHEN** the user taps "Edit" on an event with no photo, adds one through the photo strip and taps "Save"
- **THEN** the details screen shows that photo's thumbnail, and the event's row now shows the photo icon

#### Scenario: Remove a photo from a saved event
- **WHEN** the user taps "Edit" on an event with a photo, removes it (confirming the removal) and taps "Save"
- **THEN** the details screen shows one fewer thumbnail, and the row's photo icon disappears if none remain
