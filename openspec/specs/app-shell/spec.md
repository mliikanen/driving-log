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

### Requirement: Material Design 3 theming
The system SHALL render all screens using Material Design 3 with the Petroleum color theme (the palette in `docs/color-palette.md`), and SHALL use its light or
dark color scheme according to the device's system setting. The theme SHALL be the same on every screen and SHALL NOT take colors from the device's wallpaper.
In the light scheme the app background is Cool Platinum (`#F4F7F6`) with Asphalt (`#12181B`) text, primary buttons and active elements are Oil Slick Blue
(`#203A43`) with white text, interactive accents such as switches and secondary actions are Refinery Teal (`#2C5364`), and dividers are Exhaust Fog (`#E0E6ED`).
In the dark scheme the app background is Asphalt Dark (`#12181B`) and the primary and secondary colors are lighter tints of the palette's blue-greens, so that
buttons, icons and switches stand out from it. In both schemes the top app bar of every screen is Petroleum Deep (`#0F2027`) with light text and icons.
Every text color SHALL have a contrast ratio of at least 4.5:1 against the color it is drawn on, and every outline, icon and control at least 3:1 against
its surroundings, in both schemes. On Android the launch window and the system bars SHALL match the theme: the window background before the first frame is the
scheme's background, the status bar's icons are light over the header, and the navigation bar's icons are light or dark to suit the scheme.

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

### Requirement: Distances and fuel have their own accent colors
The system SHALL draw logged distances in the Road Trip Emerald accent (`#06D6A0`) in the dark scheme and in a deeper shade of it in the light scheme, where the
bright emerald is too pale to read as text (a text contrast of at least 4.5:1 on the background and on a card). The system SHALL reserve Fuel Gauge Gold
(`#FFB703`) for fuel and refueling, and SHALL use it only as a fill with dark text or as a mark, never as text on a light background. Anything that is not a
distance (an odometer reading, a name, a date) SHALL NOT use the distance accent.

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
The system SHALL show on the Home screen the application name "Driving Log" in a top app bar, and below it a grid of four actions in two rows of two, each an icon with no visible text. The icons are a car (vehicles), a pencil on a note ("Log event"), a route ("Trip") and a question mark ("Placeholder"), and every action SHALL have a name for a screen reader: "Vehicles", "Log event", "Trip" and "Placeholder". The grid SHALL stay two by two in
portrait and in landscape, and every action SHALL be at least 48 dp square with its whole area tappable. The first action SHALL always show the car icon; it SHALL open the vehicle list, or, when the user has no vehicle, the add vehicle screen, and only its name for a screen reader changes with that ("Add vehicle" instead of "Vehicles"). An action that is not available
yet ("Log event", "Trip" and "Placeholder" until the changes that build them) SHALL be disabled Material 3 components: shown in Material's disabled colors and not in the colors of an available action, not reacting to a tap, and exposed to a screen reader as disabled (with their names still readable). The vehicle list SHALL NOT be shown on the Home screen.

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
