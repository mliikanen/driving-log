## MODIFIED Requirements

### Requirement: The application requests the minimum set of permissions
The system SHALL request only the minimum set of permissions it needs to fulfil the required functionality. Where the
platform offers a way to provide a function without a permission (a system picker, chooser, camera app or source sheet
in which the user chooses the provider), the system SHALL use it and SHALL NOT declare a permission for that function.

#### Scenario: No permission prompt at start
- **WHEN** the application is installed and opened for the first time
- **THEN** no permission prompt is shown

#### Scenario: Nothing is declared that no function needs
- **WHEN** the installed Android application's requested permissions are listed
- **THEN** the only system permission it requests is the camera, which the live scanner of `odometer-ocr-capture` needs (its own internal ones aside)

#### Scenario: A permission is asked for at the action
- **WHEN** a function needs a permission (taking a photo with the camera needs camera access on both platforms; on Android, the live scanner needs it too) and the user has not been asked for it
- **THEN** the system asks for it at the moment the user triggers that function, and not before

#### Scenario: A refusal does not stop the application
- **WHEN** the user refuses a permission the function needs
- **THEN** the system shows the missing-permission message for that function ("A missing permission is explained with a way to the settings"), and the rest of the application keeps working

**Platform note:** on Android the image functions use the system chooser, and choosing a photo or file app needs no permission. The camera
permission is declared for the live scanner (`odometer-ocr-capture`), whose in-app camera preview no chooser or intent can provide, and which
asks for it when "Scan a reading" is tapped. Because it is declared, Android lets the app open a camera app only while the app holds the
permission, so choosing a camera app in the chooser asks for it too (`vehicle-picture`). On iOS only the camera needs one.

## ADDED Requirements

### Requirement: A missing permission is explained with a way to the settings
When something the user asked for can't be done because the application lacks a permission, the system SHALL show one
shared message, the same everywhere: what can't be done, that the access it needs is turned off, and an "Open
settings" action that opens the application's own page in the device settings, where that permission is controlled.
The message SHALL go away once the user is back in the application with the permission allowed.

#### Scenario: The message says what is missing
- **WHEN** the user tries to take a photo with the camera and camera access is refused
- **THEN** the screen shows that a photo can't be taken because camera access is turned off, with an "Open settings" action

#### Scenario: Open settings
- **WHEN** the user taps "Open settings" in the message
- **THEN** the device settings open on the application's own page, where its permissions are controlled

#### Scenario: Back with the permission allowed
- **WHEN** the user allows the permission in the device settings and comes back to the application
- **THEN** the message is no longer shown, and what the user does next is not refused for that permission

#### Scenario: Back without the permission
- **WHEN** the user comes back from the device settings without allowing the permission
- **THEN** the message is still shown

**Platform note:** Android opens the application's details page (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`); iOS opens the
application's page in Settings (`UIApplication.openSettingsURLString`).

### Requirement: The missing-permission message is shown only for a missing permission
The system SHALL show the missing-permission message when, and only when, an action the user asked for is not done
because a permission is not allowed: refused when asked, refused before, or the prompt left without allowing it. Any
other outcome SHALL NOT show it: leaving a chooser, a camera or another app without a result changes nothing, and
other failures keep their own message.

#### Scenario: The prompt is dismissed without an answer
- **WHEN** the system asks for a permission and the user leaves the prompt without allowing it
- **THEN** it is handled as a refusal: the action is not done and the missing-permission message is shown

#### Scenario: Not choosing a photo is not a missing permission
- **WHEN** the user leaves the chooser, the camera or a photo app without providing a photo
- **THEN** nothing changes and no missing-permission message is shown

#### Scenario: Other errors keep their own message
- **WHEN** something fails for a reason other than a missing permission (for example, a photo that can't be opened)
- **THEN** the system shows that failure's own message, without an "Open settings" action
