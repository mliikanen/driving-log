# Spec Delta

## ADDED Requirements

### Requirement: The details screen shows a "Refueling" event's fuel details
The system SHALL show, on the details screen of a "Refueling" event, its fuel amount (in the unit it was entered
in), its fuel type, whether it was a full fill-up, and, when it carries one, its mileage reading — shown the same
way a "Distance" or "Odometer reading" event's own reading or distance is shown (`event-details`, "A logged event's
details can be viewed"). A "Refueling" event saved with no mileage SHALL show no mileage line.

#### Scenario: A refueling's details
- **WHEN** the user opens the details screen of a "Refueling" event of 42.3 L, Diesel, filled up, with a trip-distance mileage of 30 km
- **THEN** the screen shows "42.3 L," "Diesel," that it was a full fill-up, and "+30 km" as its mileage

#### Scenario: No mileage line without one
- **WHEN** the user opens the details screen of a "Refueling" event saved with no mileage
- **THEN** the screen shows the fuel amount, fuel type and fill-up state, and no mileage line

## MODIFIED Requirements

### Requirement: A "Distance" or "Odometer reading" event's note can be edited
The system SHALL show an "Edit" action on the details screen of a "Distance," "Odometer reading" or "Refueling"
event, and SHALL NOT show it for an "Initial odometer" event, which can never carry a note or a photo. Tapping
"Edit" SHALL open an edit screen showing the event's note element (tap it to open the same full-screen note editor
`distance-logging`'s "The full-screen note editor" requirement describes, seeded with the event's current note) and
its photo strip (the same element `distance-logging`'s "Photos can be added to the log event form" requirement
describes, seeded with the event's current photos, up to the same 5-photo cap, each removable with the same
confirmation). The edit screen SHALL offer an explicit "Save" action that stores the note text and the resulting set
of photos together, and leaving the edit screen without saving SHALL leave the event's note and photos exactly as
they were. For a "Refueling" event, Edit's scope is unchanged by this: its fuel amount, fuel type, "filled up" state
and mileage (or the absence of one) are fixed once saved and are never offered on the edit screen — only its note
and photos can be changed, the same as for a "Distance" or "Odometer reading" event.

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
