# Spec Delta

## MODIFIED Requirements

### Requirement: Material Design 3 theming
The system SHALL render all screens using Material Design 3 with the Petroleum color theme (the palette in `docs/color-palette.md`), and SHALL use its light or
dark color scheme according to the device's system setting. The theme SHALL be the same on every screen, except that the add and edit vehicle screens are themed by the vehicle's color (as specified in the `vehicle-color` capability), and SHALL NOT take colors from the device's wallpaper.
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

#### Scenario: The vehicle screens use the vehicle's theme
- **WHEN** the user opens the add or edit vehicle screen
- **THEN** the screen is drawn with a color scheme derived from the vehicle's color, in the light or dark mode of the device, with the Petroleum Deep app bar, and every other screen keeps the Petroleum scheme
