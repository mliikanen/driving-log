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

### Requirement: The details screen shows a "Refueling" event's fuel details
The system SHALL show, on the details screen of a refueling's entry, its fuel amount (in the unit it was entered in),
its fuel type, whether it was a full fill-up and, when the entry also holds a "Distance" or "Odometer reading" event,
that event's figure as its mileage line, shown as that event's own figure is. The screen SHALL be the same whether
opened from the "Refueling" row or the mileage row. A refueling with no mileage SHALL show no mileage line.

#### Scenario: A refueling's details
- **WHEN** the user opens the details screen of a "Refueling" event of 42.3 L, Diesel, filled up, with a trip-distance mileage of 30 km
- **THEN** the screen shows "42.3 L," "Diesel," that it was a full fill-up, and "+30 km" as its mileage

#### Scenario: Opened from the mileage row
- **WHEN** the user taps the "Distance" row of a refueling logged with a trip-distance mileage of 30 km
- **THEN** the screen shows the refueling's fuel amount, fuel type and fill-up state, and "+30 km" as its mileage, exactly as when opened from its "Refueling" row

#### Scenario: No mileage line without one
- **WHEN** the user opens the details screen of a "Refueling" event saved with no mileage
- **THEN** the screen shows the fuel amount, fuel type and fill-up state, and no mileage line

### Requirement: A "Distance" or "Odometer reading" event's note can be edited
The system SHALL show an "Edit" action on the details screen of a "Distance," "Odometer reading" or "Refueling" event,
and SHALL NOT show it for an "Initial odometer" event, which never has a note or photo. Tapping "Edit" SHALL open an
edit screen with the event's note element and photo strip, seeded with its current note and photos. Its explicit
"Save" action SHALL store the note and the resulting photos together; leaving without saving SHALL leave the note and
photos exactly as they were.

#### Scenario: Edit is offered for a note-capable event
- **WHEN** the user opens the details screen of a "Distance," "Odometer reading" or "Refueling" event
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

#### Scenario: Editing a refueling never changes its fuel data
- **WHEN** the user taps "Edit" on a "Refueling" event, changes only its note, and taps "Save"
- **THEN** the details screen shows the changed note, and the event's fuel amount, fuel type, "filled up" state and mileage are exactly as they were before

#### Scenario: Editing from the mileage row changes the shared note
- **WHEN** a refueling was logged with a trip-distance mileage, and the user opens the details from its "Distance" row, taps "Edit," changes the note and taps "Save"
- **THEN** both the "Refueling" row and the "Distance" row show the note icon, and the details screen opened from either shows the changed note

### Requirement: The edit screen uses the log event form's note and photo elements
The event edit screen's note element SHALL open the same full-screen note editor the log event form uses
(`distance-logging`, "The full-screen note editor"), seeded with the event's current note. Its photo strip SHALL be the
same element as the log event form's (`distance-logging`, "Photos can be added to the log event form"), seeded with the
event's current photos, up to the same 5-photo cap, each removable with the same confirmation.

#### Scenario: The photo cap on the edit screen
- **WHEN** the user taps "Edit" on an event that has 5 photos
- **THEN** the photo strip offers no way to add another photo until one is removed

### Requirement: Editing changes the entry's shared note and photos
The note and photos the edit screen changes SHALL be those of the event's entry (`vehicle-log`, "Events of one entry
share one note and one set of photos"): saving SHALL change them for every event of the entry, whichever row the
details screen was opened from.

#### Scenario: A photo added from the refueling row is seen from the mileage row
- **WHEN** a refueling was logged with a trip-distance mileage, and the user opens the details from its "Refueling" row,
  taps "Edit," adds a photo and taps "Save"
- **THEN** its "Distance" row shows the photo icon, and the details screen opened from it shows the photo's thumbnail

### Requirement: A refueling's fuel data is never edited
For a "Refueling" event, the edit screen SHALL offer only the note and photos, as for a "Distance" or "Odometer
reading" event. Its fuel amount, fuel type, "filled up" state and mileage (or the absence of one) SHALL be fixed once
saved and SHALL NOT be offered on the edit screen.

#### Scenario: A refueling's edit screen
- **WHEN** the user taps "Edit" on a "Refueling" event
- **THEN** the edit screen shows the note element and the photo strip, and no fuel amount, fuel type, "filled up" or
  mileage field
