# Spec Delta (stub)

## MODIFIED Requirements

### Requirement: Photos are stored in the application's local file system
The system SHALL store photo files in the application's private storage, SHALL NOT store them in the device's photo
library or another shared location, and SHALL keep only an id with each photo, never a file path — the same
convention `vehicle-picture`'s "Pictures are stored in the application's local file system" requirement already
establishes. Every photo's id SHALL be unique and, once written, SHALL NOT be reused for different content, whether
the id belongs to an event's photo or a vehicle's picture. The system SHALL keep photos after the app is closed and
reopened, and SHALL show and change them without a network connection. A photo file that no saved event refers to
(left by a removal, an interrupted save or an abandoned edit session) SHALL be deleted when the app starts. A stub:
whether cleanup also happens immediately when the app itself already knows a file is abandoned, rather than only at
the next startup, is settled together with the open questions in the design.

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

#### Scenario: Unused files are cleaned up
- **WHEN** the app starts and the event picture storage holds files that no saved event refers to
- **THEN** those files are deleted and every saved event's photos are kept
