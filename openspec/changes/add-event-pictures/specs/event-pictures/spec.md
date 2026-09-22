# Spec Delta

## Purpose

Lets a "Distance" or "Odometer reading" event carry up to 5 photos, kept at their original aspect ratio (not
cropped to a square, since they are looked at for their content), stored in the application's private storage and
referenced by id, the same way a vehicle's picture is. How a photo is added, removed or shown on the log event form
is specified by `distance-logging`; this capability covers the storage facts themselves.

## ADDED Requirements

### Requirement: A photo is scaled down when it is larger than this capability's size caps
The system SHALL store two versions of each photo, both keeping its original aspect ratio and neither cropped: a
small version of at most 256 pixels on its longer side, and a large version of at most 2048 pixels on its longer
side. A version SHALL be scaled down only when the photo's longer side exceeds its cap, and SHALL NOT be enlarged: a
photo already smaller than a cap is stored at its own size. Both versions SHALL be stored in a size-efficient image
format, matching `vehicle-picture`'s own format choice (lossy WebP; PNG on iOS until a WebP encoder is added), and
the stored photo SHALL respect the orientation the original was marked with.

#### Scenario: A large photo is downscaled
- **WHEN** the user attaches a 4000 x 3000 photo
- **THEN** the stored large version is 2048 x 1536 pixels and the stored small version is 256 x 192 pixels

#### Scenario: A small photo is not enlarged
- **WHEN** the user attaches a 600 x 400 photo
- **THEN** the stored large version is 600 x 400 pixels (its own size) and the small version is 256 x 171 pixels

#### Scenario: Both versions show the same photo
- **WHEN** the user attaches a photo
- **THEN** the small and the large version show the same photo, only at different sizes

#### Scenario: Orientation is respected
- **WHEN** the user attaches a photo that is stored rotated and marked with an orientation
- **THEN** the stored versions are the right way up

### Requirement: Photos are stored in the application's local file system
The system SHALL store photo files in the application's private storage, SHALL NOT store them in the device's photo
library or another shared location, and SHALL keep only an id with each photo, never a file path — the same
convention `vehicle-picture`'s "Pictures are stored in the application's local file system" requirement already
establishes. Every photo's id SHALL be unique and, once written, SHALL NOT be reused for different content, whether
the id belongs to an event's photo or a vehicle's picture. The system SHALL keep photos after the app is closed and
reopened, and SHALL show and change them without a network connection.

#### Scenario: Survives a restart
- **WHEN** the user saves an event with photos, closes the app completely and reopens it
- **THEN** the event's photos are still shown, in its details and wherever its row shows the photo icon

#### Scenario: Not in the photo library
- **WHEN** the user saves an event with photos
- **THEN** no new photo appears in the device's photo library

#### Scenario: Offline
- **WHEN** the device has no network connection and the user attaches a photo while logging an event
- **THEN** every step succeeds and no network error is shown

#### Scenario: No id collides, ever
- **WHEN** an event's photo and a vehicle's picture are both stored
- **THEN** their ids and files never collide, whatever order they were created in
