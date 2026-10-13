## MODIFIED Requirements

### Requirement: A photo is chosen to scan for a reading
The log event form SHALL offer a "Scan a reading" action that opens the live scanner, which SHALL offer a photo action
opening the system photo chooser used for a vehicle's picture (the camera app included, which asks for camera access
when chosen, as for a vehicle's picture). Choosing a photo SHALL lead to detection and the photo review; cancelling the chooser, or leaving
the review without accepting, SHALL return to the live scanner. On iOS, with no on-device recognizer yet, the action
SHALL NOT be shown.

#### Scenario: Open the scanner
- **WHEN** the user taps "Scan a reading" on the log event form
- **THEN** the live scanner opens

#### Scenario: Open the chooser
- **WHEN** the user taps the live scanner's photo action
- **THEN** the system's photo chooser (including the camera) opens

#### Scenario: No scan on iOS
- **WHEN** the user opens the log event form on iOS
- **THEN** no "Scan a reading" action is shown

#### Scenario: Cancel the chooser
- **WHEN** the user opens the chooser from the live scanner and cancels it without choosing a photo
- **THEN** the live scanner is shown again, with no missing-permission message

#### Scenario: Leave the photo review
- **WHEN** the user chose a photo and leaves its review without accepting a candidate
- **THEN** the live scanner is shown again


### Requirement: The camera permission is asked for when the scanner is first opened
The system SHALL ask for the camera permission when the user taps "Scan a reading" and it has not been granted, and
not before. If refused, the live scanner SHALL open without a preview, show the missing-permission message
(`app-shell`) in its place, and keep its photo and close actions working. Once camera access is allowed, the scanner
SHALL show the preview as soon as the user is back in it. On iOS the scan action is not offered.

#### Scenario: Asked on first use
- **WHEN** the user taps "Scan a reading" and has never been asked for the camera
- **THEN** the system asks for the camera permission

#### Scenario: Refused
- **WHEN** the user refuses the camera permission
- **THEN** the live scanner opens without a preview and shows the missing-permission message with its "Open settings" action, and its photo action still opens the system's photo chooser

#### Scenario: Granted
- **WHEN** the user grants the camera permission
- **THEN** the live scanner shows the camera's preview

#### Scenario: Allowed from the settings
- **WHEN** the live scanner shows the missing-permission message, the user taps "Open settings", allows camera access and comes back
- **THEN** the message is gone and the live scanner shows the camera's preview

## ADDED Requirements

### Requirement: A refused camera is shown on the scan screen it was chosen from
When the user chooses the camera from the live scanner's photo action, or from "choose another photo" in the photo
review, and camera access is refused, now or before, the system SHALL open no camera, keep that screen and the scan
as they were, and show the missing-permission message (`app-shell`) on that screen. Leaving the chooser without a
photo SHALL NOT show that message.

#### Scenario: Refused from the live scanner's photo action
- **WHEN** the user chooses the camera from the live scanner's photo action and camera access is refused
- **THEN** no camera opens, the live scanner stays open, and it shows the missing-permission message with its "Open settings" action

#### Scenario: Refused from the photo review
- **WHEN** the photo review says no reading was found, the user chooses another photo with the camera, and camera access is refused
- **THEN** no camera opens, the review stays as it was, and it shows the missing-permission message with its "Open settings" action

#### Scenario: Leaving the review's chooser without a photo
- **WHEN** the user opens the chooser from the photo review and leaves it without a photo
- **THEN** the review stays as it was, with no missing-permission message
