# vehicle-color Specification

## Purpose
Gives every vehicle one color of its own, mandatory and never empty, chosen by the user from a set of presets or taken from the vehicle's photo, and
shows it in the vehicle's icons. It is the one color that everything vehicle-specific is later derived from; nothing derived from it is stored.

## Requirements

### Requirement: A vehicle always has a color
The system SHALL give every vehicle exactly one color; the data model SHALL NOT allow a vehicle without one. It SHALL
store the color as its six-digit hexadecimal code (`RRGGBB`, upper case, no alpha), independent of the language, the
device and the theme, and SHALL treat a stored value that is not a valid code as the default color, the main theme
color (Oil Slick Blue, `#203A43`). Vehicles from before colors existed SHALL get the default color on the update, not
one from their pictures.

#### Scenario: A new vehicle has the default color
- **WHEN** the user opens the add vehicle screen
- **THEN** the default color is the selected color

#### Scenario: The color is stored as a code
- **WHEN** a vehicle with the color "Teal" is saved
- **THEN** what is stored for its color is its six-digit code, whatever the device language and theme

#### Scenario: An existing vehicle after the update
- **WHEN** the app is updated and a vehicle that was saved before colors existed is shown, with or without a picture
- **THEN** it has the default color

#### Scenario: An invalid stored value
- **WHEN** a vehicle's stored color is not a valid code
- **THEN** the vehicle is shown with the default color and nothing fails

### Requirement: The color is chosen from a set of presets
The system SHALL show the color choice on the add and edit vehicle screens as a palette of twelve preset colors:
selectable swatches in a fixed order, the first being the default color, each with a name that is also its
accessibility label. Exactly one swatch or segment SHALL be selected at all times, the one of the vehicle's current
color. The choice SHALL have no "none" option, and choosing another swatch or segment SHALL replace the color.

#### Scenario: The presets are offered
- **WHEN** the user opens the color choice on the add vehicle screen
- **THEN** it offers twelve named preset colors in a fixed order, the first being the default color, the default color is the only one selected, and there is no row of segments

#### Scenario: Choose a preset
- **WHEN** the user taps the swatch "Red"
- **THEN** "Red" is the only selected swatch and the form's icons are drawn from it

#### Scenario: The saved color is selected on the edit screen
- **WHEN** the user opens the edit screen of a vehicle whose color is "Teal"
- **THEN** "Teal" is the selected swatch

#### Scenario: Cancel editing the color
- **WHEN** the user chooses another color on the edit screen and leaves without saving
- **THEN** the vehicle keeps its saved color

#### Scenario: The choice survives a rotation
- **WHEN** the user chooses a color on the add screen and rotates the device
- **THEN** the same color is still selected

### Requirement: The edit screen shows the old color
On the edit vehicle screen the color choice SHALL show, in the row beneath the palette, a segment named "Old color" filled with the vehicle's saved color and labelled with that
name in text on it, so that the user can always go back to what the vehicle had. It SHALL be selected when the current color is the saved color and is not a preset. Its color
SHALL NOT change while the form is open.

#### Scenario: A saved color that is a preset
- **WHEN** the user opens the edit screen of a vehicle whose color is "Teal"
- **THEN** the palette shows "Teal" selected and the row beneath it shows the segment "Old color" in teal, not selected

#### Scenario: A saved color that is not a preset
- **WHEN** the user opens the edit screen of a vehicle whose color is not one of the presets
- **THEN** the segment "Old color" shows that color and is the selected one, and no preset is selected

#### Scenario: Going back to the old color
- **WHEN** the user has chosen another color on the edit screen and taps "Old color"
- **THEN** the current color is the saved color again

### Requirement: A picture sets the color
When the user confirms the crop of a picture on the add or edit vehicle screen, the system SHALL extract one
representative color from the cropped photo and make it the form's current color, shown in the row beneath the palette
as a selected segment named "Photo color" (its name in text on it), so that the user sees what happened, can choose a
preset instead and can choose the photo color again. Opening the edit screen of a vehicle that has a picture SHALL NOT
extract a color.

#### Scenario: Confirming a crop sets the color
- **WHEN** the user confirms the crop of a photo that is a solid red
- **THEN** the current color is that red, the row beneath the palette shows the segment "Photo color" in it, selected, and the form's icons are drawn from it

#### Scenario: A white photo gives white
- **WHEN** the user confirms the crop of a photo that is white with a little dark grey
- **THEN** the photo color is white, not a colorful one

#### Scenario: A vivid color wins over a larger area of grey
- **WHEN** the user confirms the crop of a photo of a red car on grey asphalt, where the asphalt covers more of the middle than the car
- **THEN** the photo color is red

#### Scenario: Choose a preset after the picture
- **WHEN** the user has confirmed a crop and then taps the swatch "Blue"
- **THEN** "Blue" is selected, the segment "Photo color" is still there and unchanged, and tapping it selects the photo color again

#### Scenario: Another crop replaces the photo color
- **WHEN** the user confirms the crop of another photo
- **THEN** the "Photo color" segment and the current color are the new photo's color

#### Scenario: Cancelling a crop keeps the color
- **WHEN** the user chooses a photo, cancels its crop and the form has the color "Green"
- **THEN** the color is still "Green" and no "Photo color" segment is shown

#### Scenario: Removing the picture keeps the color
- **WHEN** the user removes the picture of a form whose current color came from it
- **THEN** the current color stays, is shown as a selected segment named "Current color" in the row beneath the palette, and the "Photo color" segment is gone

#### Scenario: A photo without opaque pixels
- **WHEN** the user confirms the crop of an image that is fully transparent
- **THEN** the current color is unchanged

#### Scenario: Opening the edit screen changes nothing
- **WHEN** the user opens the edit screen of a vehicle that has a picture and the color "Teal"
- **THEN** the color is still "Teal", no "Photo color" segment is shown until a new crop is confirmed, and the row shows "Old color" alone at the full width

#### Scenario: Old color and photo color share the row
- **WHEN** the user is on the edit screen and confirms the crop of a photo
- **THEN** the row beneath the palette shows two segments of equal width, "Old color" and "Photo color"

### Requirement: The vehicle's icons are drawn from its color
The system SHALL draw the vehicle icons (the type's icon for a vehicle without a picture, in the vehicle list, on the
details screen and in the form's picture preview, and the icons on the forms' type tiles) in a tint derived from the
vehicle's color, on a container derived from the same color. The tint SHALL reach a contrast of at least 3:1 against
its container in both schemes, whatever the color (white, black and greys included). A picture SHALL still be shown
instead of the icon.

#### Scenario: The list icon follows the color
- **WHEN** the vehicle list contains a vehicle of the type "Van" and the color "Red" without a picture
- **THEN** its van icon is drawn in a tint of red on a container of red, not in the neutral color

#### Scenario: The form preview follows the choice
- **WHEN** the user taps another color swatch on the add screen
- **THEN** the picture preview and the type tiles are drawn from that color

#### Scenario: Every color stays legible
- **WHEN** the icon is drawn for any of the presets, for white, for black, and for a sweep of other colors, in light and in dark
- **THEN** its contrast against its container is at least 3:1

#### Scenario: A picture wins
- **WHEN** a vehicle of the color "Red" has a picture
- **THEN** the vehicle list and the details screen show the picture and not the icon

### Requirement: Color changes are animated as one
Whenever a vehicle's color changes while it is on screen (choosing a swatch, a picture setting the color, a saved
change reaching a screen that shows the vehicle), the system SHALL animate it as one animation: a single animated
color drives every color derived from it, so that all of them change together and none jumps or runs out of step with
another. The animation SHALL take about 300 ms and SHALL move through the HCT color space (hue by the shortest way
around).

#### Scenario: Choosing a swatch animates
- **WHEN** the user taps another swatch on the add screen
- **THEN** the icons on the screen change from the old color to the new one in one animation, not in one jump

#### Scenario: Everything moves together
- **WHEN** the color changes and the screen shows several things derived from it (the preview, the tiles)
- **THEN** at every moment of the animation they are all derived from the same intermediate color

#### Scenario: A second change interrupts the first
- **WHEN** the user taps a swatch while the animation to the previous one is still running
- **THEN** the animation continues from the color shown at that moment, without a jump

#### Scenario: The first display is not animated
- **WHEN** a screen is opened
- **THEN** it shows the vehicle's color at once, without animating from another color

#### Scenario: Animations are turned off
- **WHEN** the system's animation setting is off and the user taps another swatch
- **THEN** the colors change at once

### Requirement: The color is stored with the vehicle and works offline
The system SHALL keep a vehicle's color on the device with the vehicle, so that it is still there after the app is closed and reopened, and SHALL offer choosing
it and taking it from a picture without a network connection.

**Platform note:** the behavior is the same on iOS; it is verified on Android now and on iOS once the Xcode project exists.

#### Scenario: The color survives a restart
- **WHEN** the user adds a vehicle with the color "Purple", closes the app completely and opens it again
- **THEN** the vehicle still has the color "Purple"

#### Scenario: Offline use
- **WHEN** the device has no network connection and the user adds a vehicle, crops a picture and chooses a color
- **THEN** every step succeeds and no network error is shown

### Requirement: A row of segments holds colors that are not presets
Beneath the palette, the color choice MAY show one full-width row of segments for colors that are not presets (the
photo color, the vehicle's old or current color); when it has more than one segment, they SHALL share the row's width
equally. A current color that is not a preset SHALL be selected as its segment.

#### Scenario: A saved color that is not a preset
- **WHEN** the user opens the edit screen of a vehicle whose saved color is not one of the presets
- **THEN** the row shows the "Old color" segment, selected, and no swatch is selected

### Requirement: The edit screen starts with the saved color and applies it on save
The edit screen SHALL start with the vehicle's saved color selected. The color SHALL be applied when the form is saved;
leaving the form without saving SHALL keep the saved color.

#### Scenario: Saving applies the chosen color
- **WHEN** the user chooses "Green" on the edit screen and saves
- **THEN** the vehicle's color is "Green"

### Requirement: The photo color is extracted predictably
The extraction of a photo color SHALL be deterministic (the same photo always gives the same color), SHALL weigh the
central part of the photo more than its border, SHALL favor vivid colors over grey ones (a red car on grey asphalt gives
red), and SHALL still give white, black or grey for a photo that has nothing vivid in it (a white car gives white). A
photo without any opaque pixel SHALL leave the color as it was.

#### Scenario: The same photo twice
- **WHEN** the user confirms the same crop of the same photo twice
- **THEN** both give the same photo color

### Requirement: The photo color segment changes only with a new photo
The row beneath the palette SHALL change in real time only when a photo is added: the "Photo color" segment takes the
color of the newest confirmed crop, and nothing else about the row changes while the user chooses presets. Cancelling a
crop SHALL NOT change the color, and neither SHALL removing the picture: the current color stays, and the "Photo color"
segment goes with the picture.

#### Scenario: Choosing presets leaves the photo color alone
- **WHEN** the user has confirmed a crop on the add screen, and then taps "Red" and then "Blue"
- **THEN** the "Photo color" segment is still shown, unchanged, and not selected

### Requirement: A color animation can be interrupted or skipped
A color animation interrupted by another change SHALL continue from the color shown at that moment. No animation SHALL
run when a screen is first shown, nor when the system's animation setting turns animations off: the change is then
immediate.

#### Scenario: A saved color reaching the details screen
- **WHEN** the user saves another color on the edit screen and the details screen is shown again
- **THEN** the details screen's icon moves to the new color in one animation, starting from the color it showed
