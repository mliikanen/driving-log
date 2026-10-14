# app-shell Specification

## Purpose

Defines the baseline behavior of the Driving Log application shell: how it launches, how it looks, and what the
user sees before any feature exists. Later features add screens and behavior on top of this baseline.

## Requirements

### Requirement: Application launches to the Home screen
The system SHALL start on a sign-in screen (`firebase-auth`, "A sign-in screen gates the rest of the app") when the
user is not signed in, and on the Home screen, without requiring network access or any other setup, once they are.

#### Scenario: Cold start on Android
- **WHEN** the user launches the app from the Android launcher, already signed in
- **THEN** the Home screen is displayed

#### Scenario: Launch without network
- **WHEN** the user launches the app, already signed in, while the device has no network connection
- **THEN** the Home screen is displayed and no error is shown

### Requirement: Material Design 3 theming
The system SHALL render all screens using Material Design 3 with the Petroleum color theme (the palette in
`docs/color-palette.md`), in its light or dark color scheme according to the device's system setting. The theme SHALL
be the same on every screen and SHALL NOT take colors from the device's wallpaper. In both schemes the top app bar of
every screen SHALL be Petroleum Deep (`#0F2027`) with light text and icons.

#### Scenario: Device in light mode
- **WHEN** the app is opened while the device is set to light mode
- **THEN** the app is displayed with the light color scheme

#### Scenario: Device in dark mode
- **WHEN** the app is opened while the device is set to dark mode
- **THEN** the app is displayed with the dark color scheme

#### Scenario: System setting changes while running
- **WHEN** the device switches between light and dark mode while the app is open
- **THEN** the app switches to the matching color scheme without restarting

#### Scenario: The light scheme uses the palette
- **WHEN** the app is opened in light mode
- **THEN** the screen background is `#F4F7F6`, the top app bar is `#0F2027` with white title and icons, and a primary button is `#203A43` with white text

#### Scenario: The dark scheme uses the palette
- **WHEN** the app is opened in dark mode
- **THEN** the screen background is `#12181B`, the top app bar is `#0F2027` with light title and icons, and a divider line separates the app bar from the screen

#### Scenario: Text and controls are readable in both schemes
- **WHEN** the colors of either scheme are checked pair by pair
- **THEN** every text color has a contrast ratio of at least 4.5:1 on its background and every outline, icon and control at least 3:1 against its surroundings

#### Scenario: Errors stay distinct
- **WHEN** a field shows a validation error
- **THEN** its message and outline use an error color that is clearly different from the palette's blues and readable on the background, in both schemes

#### Scenario: No color from the wallpaper
- **WHEN** the device offers wallpaper-based dynamic colors
- **THEN** the app still shows the Petroleum theme

#### Scenario: The launch window matches
- **WHEN** the app is launched on Android in light mode or in dark mode
- **THEN** the window shows the scheme's background color before the first frame, with no white or black flash

#### Scenario: System bar icons follow the theme
- **WHEN** a screen is shown on Android in light mode
- **THEN** the status bar's icons are light over the dark header and the navigation bar's icons are dark over the light background

**Platform note:** the system bars are configured on Android. On iOS the status bar must also show light content over the dark header; this is verified once the Xcode project exists.

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

### Requirement: Screens respect the system bars and the keyboard
The system SHALL keep every screen's content out from under the status bar, the gesture or three-button navigation
bar, the display cutout and the keyboard, and SHALL let the user reach all of it while the keyboard is shown: content
that does not fit SHALL scroll, far enough that its last item ends clear of the navigation bar or the keyboard by the
inset and a little more, so nothing is left half hidden. The system SHALL NOT add the same inset twice, which would
leave a gap.

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

### Requirement: Distances and fuel have their own accent colors
The system SHALL draw logged distances in the Road Trip Emerald accent (`#06D6A0`) in the dark scheme and in a deeper
shade of it in the light scheme, where the bright emerald is too pale to read as text (a contrast of at least 4.5:1 on
the background and on a card). It SHALL reserve Fuel Gauge Gold (`#FFB703`) for fuel and refueling, as a fill with
dark text or as a mark, never as text on a light background. Nothing that is not a distance SHALL use the distance
accent.

#### Scenario: A distance entry in the log
- **WHEN** the log or the recent events show a distance entry
- **THEN** its distance ("+30 km") is drawn in the distance accent, and the initial odometer reading and the dates are not

#### Scenario: The accent is readable in light mode
- **WHEN** a distance entry is shown in light mode
- **THEN** the distance's text contrast against the background is at least 4.5:1, which the bright emerald alone would not give

#### Scenario: Gold is not used as light-mode text
- **WHEN** any screen is shown in light mode
- **THEN** no text is drawn in Fuel Gauge Gold on the light background

### Requirement: Home screen offers the main actions
The system SHALL show on the Home screen the application name "Driving Log" in a top app bar and, below it, a grid of
four actions in two rows of two, each an icon with no visible text: a car (vehicles), a pencil on a note ("Log
event"), a route ("Trip") and a question mark ("Placeholder"), named for a screen reader "Vehicles", "Log event",
"Trip" and "Placeholder". The grid SHALL stay two by two in portrait and in landscape. The vehicle list SHALL NOT be
shown on the Home screen.

#### Scenario: Home screen with vehicles
- **WHEN** the Home screen is displayed and the user has added a vehicle
- **THEN** the top app bar shows "Driving Log" and the grid shows the car, note, route and question mark icons, two in each row, with no text on the tiles

#### Scenario: Home screen without vehicles
- **WHEN** the Home screen is displayed and the user has not added any vehicle
- **THEN** the first action still shows the car icon, its name for a screen reader is "Add vehicle", and tapping it displays the add vehicle screen

#### Scenario: Open the vehicles
- **WHEN** the user has a vehicle and taps the first action
- **THEN** the vehicle list is displayed, and going back from it returns to the Home screen

#### Scenario: An action that is not available
- **WHEN** the user taps the trip action
- **THEN** nothing happens, and the action is shown in the disabled colors, unlike the available actions

#### Scenario: Available and disabled actions look different
- **WHEN** the Home screen is displayed with a vehicle, in the light and in the dark scheme
- **THEN** the car action is drawn in the theme's action colors, the other three in the theme's disabled colors, and the two are told apart by color as well as by the icon

#### Scenario: The grid in landscape
- **WHEN** the user rotates the device to landscape while the Home screen is displayed
- **THEN** the four actions are still shown two by two and each can be tapped

#### Scenario: The first action waits for the vehicles
- **WHEN** the application starts and the vehicles are not yet loaded
- **THEN** the first action does not react to a tap until it is known whether the user has vehicles, so it cannot open the wrong screen

### Requirement: The light and dark schemes use the palette
In the light scheme the app background SHALL be Cool Platinum (`#F4F7F6`) with Asphalt (`#12181B`) text, primary
buttons and active elements Oil Slick Blue (`#203A43`) with white text, interactive accents such as switches and
secondary actions Refinery Teal (`#2C5364`), and dividers Exhaust Fog (`#E0E6ED`). In the dark scheme the background
SHALL be Asphalt Dark (`#12181B`), with lighter tints of the palette's blue-greens as primary and secondary, so that buttons, icons and switches stand out.

#### Scenario: The light scheme's accents and dividers
- **WHEN** the app is opened in light mode
- **THEN** its secondary color is Refinery Teal (`#2C5364`) and its divider color is Exhaust Fog (`#E0E6ED`)

### Requirement: Text and controls meet a contrast minimum
Every text color SHALL have a contrast ratio of at least 4.5:1 against the color it is drawn on, and every outline,
icon and control at least 3:1 against its surroundings, in both color schemes.

#### Scenario: A primary button in light mode
- **WHEN** a primary button is shown in light mode
- **THEN** its white text has a contrast of at least 4.5:1 against its Oil Slick Blue fill

### Requirement: The launch window and the system bars match the theme
On Android the launch window and the system bars SHALL match the theme: the window background before the first frame
SHALL be the scheme's background, the status bar's icons SHALL be light over the header, and the navigation bar's icons
SHALL be light or dark to suit the scheme.

#### Scenario: System bar icons in dark mode
- **WHEN** a screen is shown on Android in dark mode
- **THEN** the status bar's icons are light over the header and the navigation bar's icons are light over the dark
  background

### Requirement: A permission is asked for only at the action that needs it
A permission that is needed SHALL be declared for that function only and SHALL be requested at the moment the user
triggers the action that needs it, never at start-up or on entering a screen. A refusal SHALL be explained to the user
and SHALL NOT stop the application or block any function that does not need it.

#### Scenario: Opening a form that can scan asks nothing
- **WHEN** the user opens the log event form, which offers "Scan a reading", without tapping it
- **THEN** no permission prompt is shown

### Requirement: Home screen actions are large and tappable
Every Home screen action SHALL be at least 48 dp square, with its whole area tappable.

#### Scenario: Tapping the edge of a tile
- **WHEN** the user taps the first action near the edge of its tile, away from the icon
- **THEN** the action reacts as it does to a tap on its icon

### Requirement: The first Home screen action follows whether there are vehicles
The first Home screen action SHALL always show the car icon. It SHALL open the vehicle list or, when the user has no
vehicle, the add vehicle screen, and only its name for a screen reader changes with that ("Add vehicle" instead of
"Vehicles").

#### Scenario: After the first vehicle is added
- **WHEN** the user with no vehicle adds one and returns to the Home screen
- **THEN** the first action's name for a screen reader is "Vehicles", and tapping it opens the vehicle list

### Requirement: Home screen actions that are not available are disabled
An action that is not available yet ("Log event", "Trip" and "Placeholder" until the changes that build them) SHALL be a
disabled Material 3 component: shown in Material's disabled colors and not in an available action's, not reacting to a
tap, and exposed to a screen reader as disabled, with its name still readable.

#### Scenario: A screen reader reads a disabled action
- **WHEN** a screen reader reaches the "Trip" action
- **THEN** it reads its name, "Trip", and that it is disabled

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
