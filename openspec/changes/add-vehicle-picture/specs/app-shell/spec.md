# Spec Delta

## ADDED Requirements

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
- **THEN** it requests no permission of the system (its own internal ones aside)

#### Scenario: A permission is asked for at the action
- **WHEN** a function needs a permission (on iOS, taking a photo needs camera access) and the user has not been asked for it
- **THEN** the system asks for it at the moment the user triggers that function, and not before

#### Scenario: A refusal does not stop the application
- **WHEN** the user refuses a permission the function needs
- **THEN** the system explains that the function needs it and that it can be allowed in the device settings, and the rest of the application keeps working

**Platform note:** on Android the image functions use the system chooser and the camera app through intents and need no permission. On iOS only the camera needs one.

### Requirement: Screens respect the system bars and the keyboard
The system SHALL keep the content of every screen out from under the status bar, the gesture bar or the three-button navigation bar, the display
cutout and the keyboard, and SHALL let the user reach all of a screen's content while the keyboard is shown: content that does not fit SHALL scroll,
and it SHALL scroll far enough that its last item ends clear of the navigation bar or the keyboard by the inset and a little more, so nothing is
left half hidden at the end. The system SHALL NOT add the same inset twice, which would leave a gap.

#### Scenario: The end of a screen clears a three-button navigation bar
- **WHEN** the device uses three-button navigation and the user scrolls the vehicle details screen to its end
- **THEN** the last action ("View full log") is fully visible with space between it and the navigation bar

#### Scenario: Forms while the keyboard is shown
- **WHEN** the keyboard is shown on the add vehicle screen and the user scrolls the form to its end
- **THEN** the last field is fully visible above the keyboard, and no field is stuck under it

#### Scenario: The field being edited stays visible
- **WHEN** the user taps a text field that is below the keyboard's top edge
- **THEN** the screen scrolls so that the field is visible above the keyboard

#### Scenario: Landscape
- **WHEN** the device is in landscape orientation
- **THEN** every screen's content can be scrolled to its end, clear of the system bars and the keyboard

