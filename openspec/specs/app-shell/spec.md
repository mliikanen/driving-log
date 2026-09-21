# app-shell Specification

## Purpose

Defines the baseline behavior of the Driving Log application shell: how it launches, how it looks, and what the
user sees before any feature exists. Later features add screens and behavior on top of this baseline.

## Requirements

### Requirement: Application launches to the Home screen
The system SHALL start on a Home screen when the application is opened, without requiring login, network
access or any other setup.

#### Scenario: Cold start on Android
- **WHEN** the user launches the app from the Android launcher
- **THEN** the Home screen is displayed

#### Scenario: Launch without network
- **WHEN** the user launches the app while the device has no network connection
- **THEN** the Home screen is displayed and no error is shown

### Requirement: Home screen lists vehicles
The system SHALL show the application name "Driving Log" in a top app bar on the Home screen, and SHALL show the
user's vehicles in the content area as specified in the `vehicles` capability. When the user has no vehicles, the
content area SHALL show an empty state that invites the user to add a vehicle. The Home screen SHALL offer an action
to add a vehicle.

#### Scenario: Home screen with vehicles
- **WHEN** the Home screen is displayed and the user has added vehicles
- **THEN** the top app bar shows "Driving Log"
- **AND** the content area lists the vehicles

#### Scenario: Empty Home screen content
- **WHEN** the Home screen is displayed and the user has not added any vehicle
- **THEN** the top app bar shows "Driving Log"
- **AND** the content area shows the empty state and the action to add a vehicle

### Requirement: Material Design 3 theming
The system SHALL render all screens using Material Design 3, and SHALL use the light or dark color scheme
according to the device's system setting.

#### Scenario: Device in light mode
- **WHEN** the app is opened while the device is set to light mode
- **THEN** the app is displayed with the light color scheme

#### Scenario: Device in dark mode
- **WHEN** the app is opened while the device is set to dark mode
- **THEN** the app is displayed with the dark color scheme

#### Scenario: System setting changes while running
- **WHEN** the device switches between light and dark mode while the app is open
- **THEN** the app switches to the matching color scheme without restarting

### Requirement: Home screen state survives configuration changes
The system SHALL keep the Home screen showing the same content across configuration changes such as device
rotation, without returning to a loading or initial state.

#### Scenario: Rotation on Android
- **WHEN** the user rotates the device while the Home screen is displayed
- **THEN** the Home screen is displayed again with the same content

### Requirement: iOS entry point
The shared code SHALL expose an entry point that an iOS host app can embed to display the same Home screen.
Behavior on iOS SHALL match Android unless a difference is recorded in a feature spec.

#### Scenario: iOS host embeds the entry point
- **WHEN** an iOS host app displays the shared entry point
- **THEN** the Home screen is displayed with the same content as on Android

**Platform note:** iOS is an opportunistic target. This requirement is verified on a Mac once the Xcode project exists,
not by Android builds or Linux CI.

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
