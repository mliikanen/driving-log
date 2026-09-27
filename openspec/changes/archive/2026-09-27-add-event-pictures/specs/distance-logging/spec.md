# Spec Delta

## ADDED Requirements

### Requirement: Photos can be added to the log event form
The system SHALL show a photo element on the log event form, below the note element, for a "Distance" or "Odometer
reading" (new odometer count) entry — the "Initial odometer" event never offers one, the same restriction it already
has for a note. The element SHALL show a strip of thumbnails for the photos currently attached (0 to 5) and, while
fewer than 5 are attached, an "Add photo" action. Tapping "Add photo" SHALL show the system's own chooser of image
sources (`vehicle-picture`, "The photo comes from an app the user chooses through the system"), and the chosen photo
SHALL be added to the strip **without a crop step**: unlike a vehicle's picture, an event photo is looked at for its
content, and cropping it to a square risks cutting off what it was meant to capture. Once 5 are attached, "Add
photo" SHALL NOT be shown until one is removed.

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

### Requirement: An attached photo can be removed while composing the event
The system SHALL show a remove action on each thumbnail in the photo strip. Tapping it SHALL show a confirmation
dialog before removing that photo. Confirming SHALL remove it from the strip; dismissing or cancelling SHALL leave
it attached.

#### Scenario: Remove a photo
- **WHEN** the user taps a thumbnail's remove action and confirms
- **THEN** that photo is no longer in the strip

#### Scenario: Cancel keeps the photo
- **WHEN** the user taps a thumbnail's remove action and cancels or dismisses the dialog
- **THEN** that photo is still in the strip

### Requirement: Attached photos are saved with the event
Saving a valid log event form SHALL store the attached photos (0 to 5), if any, with the new event. Leaving the form
without saving SHALL discard the attached photos along with the rest of the unsaved entry, as it already does for
every other field on the form. Attached photos SHALL be kept across a rotation or a process restart while the form
is open.

#### Scenario: Photos are saved with the entry
- **WHEN** the user attaches photos, enters a valid distance and saves
- **THEN** the log contains the new entry together with those photos

#### Scenario: No photos means nothing extra is stored
- **WHEN** the user saves a valid entry without ever attaching a photo
- **THEN** the log contains the new entry with no photo

#### Scenario: Leaving the form drops attached photos too
- **WHEN** the user attaches a photo and leaves the form without saving
- **THEN** no entry is added and the photo is not stored anywhere

#### Scenario: Photos survive a rotation
- **WHEN** the user has attached photos and rotates the device before saving
- **THEN** the form still shows those photos attached
