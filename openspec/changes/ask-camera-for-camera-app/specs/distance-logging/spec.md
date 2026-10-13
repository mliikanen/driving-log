## ADDED Requirements

### Requirement: A refused camera is shown on the photo element
When the user chooses the camera from "Add photo" and camera access is refused, now or before, the system SHALL open
no camera, keep the attached photos as they were, and show the missing-permission message (`app-shell`) on the photo
element. Leaving the chooser, the camera or a photo app without providing a photo SHALL change nothing and SHALL NOT
show that message. The same holds on the event edit screen, which uses this photo element.

#### Scenario: Camera access refused when adding a photo
- **WHEN** the user taps "Add photo", chooses the camera, and camera access is refused (now or before)
- **THEN** no camera opens, the attached photos stay as they were, and the photo element shows the missing-permission message with its "Open settings" action

#### Scenario: Allowed from the message
- **WHEN** the photo element shows the missing-permission message, and the user allows camera access in the device settings and comes back
- **THEN** the message is gone, and choosing the camera from "Add photo" opens it

#### Scenario: Leaving the chooser without a photo
- **WHEN** the user taps "Add photo" and leaves the chooser, the camera or a photo app without providing a photo
- **THEN** the attached photos stay as they were, and no missing-permission message is shown
