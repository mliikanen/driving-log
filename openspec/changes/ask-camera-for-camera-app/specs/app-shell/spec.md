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
- **THEN** the system explains that the function needs it and that it can be allowed in the device settings, and the rest of the application keeps working

**Platform note:** on Android the image functions use the system chooser, and choosing a photo or file app needs no permission. The camera
permission is declared for the live scanner (`odometer-ocr-capture`), whose in-app camera preview no chooser or intent can provide, and which
asks for it when "Scan a reading" is tapped. Because it is declared, Android lets the app open a camera app only while the app holds the
permission, so choosing a camera app in the chooser asks for it too (`vehicle-picture`). On iOS only the camera needs one.
