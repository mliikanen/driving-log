# Spec Delta

## MODIFIED Requirements

### Requirement: The application requests the minimum set of permissions
The system SHALL request only the minimum set of permissions it needs to fulfil the required functionality. Where the platform offers a way
to provide a function without a permission (a system picker, chooser, camera app or source sheet in which the user chooses the provider),
the system SHALL use it and SHALL NOT declare a permission for that function. A permission that is needed SHALL be declared for that
function only, SHALL be requested at the moment the user triggers the action that needs it and never at start-up or on entering a
screen, and a refusal SHALL be explained to the user and SHALL NOT stop the application or block any function that does not need it.

#### Scenario: No permission prompt at start
- **WHEN** the application is installed and opened for the first time
- **THEN** no permission prompt is shown

#### Scenario: Nothing is declared that no function needs
- **WHEN** the installed Android application's requested permissions are listed
- **THEN** the only system permission it requests is the camera, which the live scanner of `odometer-ocr-capture` needs (its own internal ones aside)

#### Scenario: A permission is asked for at the action
- **WHEN** a function needs a permission (on iOS, taking a photo needs camera access; on Android, the live scanner needs it) and the user has not been asked for it
- **THEN** the system asks for it at the moment the user triggers that function, and not before

#### Scenario: A refusal does not stop the application
- **WHEN** the user refuses a permission the function needs
- **THEN** the system explains that the function needs it and that it can be allowed in the device settings, and the rest of the application keeps working

**Platform note:** on Android the image functions use the system chooser and the camera app through intents and need no permission; the
one exception is the live scanner (`odometer-ocr-capture`), whose in-app camera preview no chooser or intent can provide, and which asks for
the camera permission when "Scan a reading" is tapped. On iOS only the camera needs one.
