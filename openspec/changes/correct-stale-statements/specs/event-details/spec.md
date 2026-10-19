## RENAMED Requirements

- FROM: `### Requirement: The details screen shows a "Distance" or "Odometer reading" event's photos`
- TO: `### Requirement: The details screen shows an event's photos`

- FROM: `### Requirement: A "Distance" or "Odometer reading" event's note can be edited`
- TO: `### Requirement: An event's note and photos can be edited`

## MODIFIED Requirements

### Requirement: The details screen shows an event's photos
The system SHALL show, on the details screen of a "Distance," "Odometer reading" or "Refueling" event whose entry
has one or more photos, a thumbnail for each, in the order they were attached. Tapping a thumbnail SHALL open a full-size viewer for
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

### Requirement: An event's note and photos can be edited
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
