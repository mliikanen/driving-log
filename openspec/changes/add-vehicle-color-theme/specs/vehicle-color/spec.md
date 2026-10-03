# Spec Delta

## ADDED Requirements

### Requirement: The add and edit screens are themed by the vehicle's color
The system SHALL draw the add vehicle screen and the edit vehicle screen with a Material 3 color scheme derived from the form's current color, in the light or dark mode of the
device, in place of the Petroleum scheme: the background and surfaces, containers, text fields, buttons, radio buttons, selected tiles and icons SHALL take their colors
from it. The app bar SHALL stay Petroleum Deep with light text and icons, the error colors SHALL stay the app's error colors, and the system bars SHALL follow the device's mode as elsewhere. Every text color SHALL
reach a contrast of at least 4.5:1 against the color it is drawn on, and every outline, icon and control at least 3:1 against its surroundings, in both
modes, whatever the vehicle's color is (any preset, white, black and greys included). Leaving the screen SHALL restore the app's theme for the screen that is shown next.

#### Scenario: The default color
- **WHEN** the user opens the add vehicle screen and has not chosen another color
- **THEN** the screen is themed in the shades of the default color and its texts and controls meet the contrast rules

#### Scenario: Choosing a color themes the screen
- **WHEN** the user taps the swatch "Red" on the add vehicle screen
- **THEN** the screen's background, containers, buttons, selected tiles and icons are red-based shades, while the app bar is still Petroleum Deep

#### Scenario: The edit screen starts in the vehicle's color
- **WHEN** the user opens the edit screen of a vehicle whose color is "Teal"
- **THEN** the screen is themed in the shades of teal from the first frame, without animating from another color

#### Scenario: Every color is legible
- **WHEN** the screen is drawn for any preset, for white, for black and for a sweep of other colors, in light and in dark
- **THEN** every text pair reaches 4.5:1 and every outline, icon and control reaches 3:1

#### Scenario: Errors stay recognizable
- **WHEN** the vehicle's color is "Red" and a field shows an error
- **THEN** the error text and outline are drawn in the app's error colors and are readable on the themed surface

#### Scenario: Leaving restores the app theme
- **WHEN** the user saves the vehicle, or leaves the screen without saving, and the details screen or the vehicle list is shown
- **THEN** that screen is drawn with the Petroleum scheme

#### Scenario: The device mode is respected
- **WHEN** the device is in dark mode and the user chooses a color
- **THEN** the screen is drawn with the dark scheme derived from that color

### Requirement: The screen's colors change in one animation
When the color of the form changes on the add or edit screen (a swatch is chosen, or a picture sets the color), the system SHALL move every color of the screen (background,
surfaces, containers, fields, buttons, selected tiles and icons) in the single animation of the color animation requirement, so that all of them are derived at every moment
from the same intermediate color. The app bar, which does not depend on the color, SHALL NOT change. The animation SHALL NOT make any text unreadable at any moment of
it: the contrast rules hold for every intermediate color.

#### Scenario: The whole screen changes together
- **WHEN** the user taps another swatch on the add screen
- **THEN** the background, containers, buttons, tiles and icons change from the old color to the new one in one animation, none of them earlier or later than the others

#### Scenario: Legible during the animation
- **WHEN** the color animation is running
- **THEN** every text pair on the screen meets the contrast rules at every frame

#### Scenario: The app bar does not move
- **WHEN** the color changes
- **THEN** the app bar keeps its Petroleum Deep color throughout
