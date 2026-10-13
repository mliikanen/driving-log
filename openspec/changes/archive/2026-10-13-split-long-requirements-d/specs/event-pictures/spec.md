## MODIFIED Requirements

### Requirement: A photo is scaled down when it is larger than this capability's size caps
The system SHALL store two versions of each photo, both keeping its aspect ratio and neither cropped: a small one of
at most 256 pixels on its longer side and a large one of at most 2048. A version SHALL be scaled down only when the
photo's longer side exceeds its cap, and SHALL NOT be enlarged. Both SHALL be stored in `vehicle-picture`'s
size-efficient format (lossy WebP; PNG on iOS until a WebP encoder is added), the right way up for the orientation the
original was marked with.

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
The system SHALL store photo files in the application's private storage, never in the photo library or another shared
location, and SHALL keep only an id with each photo, never a file path, as `vehicle-picture` does. Every photo id, of
an event's photo or a vehicle's picture, SHALL be unique and SHALL NOT be reused for different content once written.
Photos SHALL survive closing and reopening the app, and SHALL be shown and changed without a network connection.

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
