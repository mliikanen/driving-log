# Spec Delta

## Purpose

Gives each vehicle a palette of four colors that belongs to it: extracted from the vehicle's picture, or derived from a main color
the user chooses when the vehicle has no picture. The palette is stored with the vehicle so that screens can later take their colors
from it. This capability stores and shows the palette; it does not apply it to any screen.

## ADDED Requirements

### Requirement: A vehicle has a palette of four colors, or none
The system SHALL store with a vehicle a palette of exactly four opaque colors, in a fixed order with the most representative color first,
or no palette. A vehicle SHALL have a palette if and only if it has a picture (see the `vehicle-picture` capability) or a main color. The four colors of a
palette SHALL be visibly different from each other. The stored form SHALL NOT depend on the device locale, the theme or the platform. The palette
SHALL be kept after the app is closed and reopened and SHALL be read and changed without a network connection.

#### Scenario: A vehicle with a picture has a palette
- **WHEN** a vehicle has a picture
- **THEN** it has a palette of four colors

#### Scenario: A vehicle with neither has none
- **WHEN** a vehicle has no picture and no main color
- **THEN** it has no palette

#### Scenario: The palette survives a restart
- **WHEN** the user saves a vehicle with a palette, closes the app completely and opens it again
- **THEN** the vehicle's edit screen shows the same four colors in the same order

#### Scenario: Offline use
- **WHEN** the device has no network connection and the user adds a picture or chooses a color for a vehicle
- **THEN** the palette is computed and saved and no network error is shown

### Requirement: The palette of a picture is extracted from the cropped photo
When the user confirms the crop of a vehicle's picture, the system SHALL extract a palette of four colors from the cropped photo, before the
vehicle is saved, and SHALL show it on the form. The extraction SHALL be deterministic: the same cropped photo SHALL always give the same
four colors in the same order. It SHALL consider the central part of the photo more than its edges, SHALL ignore transparent
pixels, and SHALL NOT discard white, black or grey: a white vehicle SHALL get a white color. When the photo has fewer than four distinct colors, the
system SHALL fill the palette up with lightness variations of its most representative color, so that there are always four different colors. A photo
without any opaque pixel SHALL give no palette. The palette of a picture SHALL come from the crop the user confirmed, not from the original photo.

**Platform note:** the extraction is the same on Android and iOS; only the reading of the pixels is platform code.

#### Scenario: A solid color photo
- **WHEN** the user confirms the crop of a photo that is entirely the color #C62828
- **THEN** the first color of the palette is #C62828 and the other three are different lightness variations of it

#### Scenario: A photo with two main colors
- **WHEN** the user confirms the crop of a photo whose left half is red and whose right half is blue
- **THEN** the palette contains a red color and a blue color

#### Scenario: A white vehicle
- **WHEN** the user confirms the crop of a photo that is mostly white with a little dark grey
- **THEN** the palette contains a white color and a dark grey color

#### Scenario: The middle counts more than the edges
- **WHEN** the user confirms the crop of a photo with a red square in the middle and a blue border of the same total area
- **THEN** the first color of the palette is red

#### Scenario: The same photo, the same palette
- **WHEN** the same crop of the same photo is confirmed twice
- **THEN** both palettes have the same four colors in the same order

#### Scenario: Only the confirmed crop counts
- **WHEN** the user crops a photo of a red vehicle in front of a blue wall so that only the vehicle is inside the frame
- **THEN** the palette has no blue color that comes from the wall

#### Scenario: A transparent photo
- **WHEN** the user confirms the crop of an image that is completely transparent
- **THEN** the picture has no palette

### Requirement: A main color can be chosen for a vehicle without a picture
The system SHALL let the user choose a main color for a vehicle on the add and edit screens when the form has no picture, using the color picker, and SHALL let the
user clear the color again. The system SHALL NOT offer the color choice while the form has a picture: the palette then comes from the picture, and the screen SHALL say so.
The main color is optional. A chosen color that has not been saved SHALL be kept when the screen is rotated or recreated, and SHALL be discarded when the user leaves the
screen without saving.

#### Scenario: Choose a color when adding a vehicle
- **WHEN** the user adds a vehicle without a picture, chooses the color #1E88E5 and saves
- **THEN** the vehicle has the main color #1E88E5 and a palette whose first color is #1E88E5

#### Scenario: The choice is not offered with a picture
- **WHEN** the form has a picture
- **THEN** the screen shows the four colors of the picture, says that they come from the picture, and offers no color picker

#### Scenario: Clear the color
- **WHEN** the user clears the main color of a vehicle without a picture and saves
- **THEN** the vehicle has no main color and no palette

#### Scenario: Rotate with an unsaved color
- **WHEN** the user has chosen a color and rotates the device
- **THEN** the form still shows that color and its palette

#### Scenario: Leave without saving
- **WHEN** the user chooses a color on the edit screen and leaves without saving
- **THEN** the vehicle keeps its previous main color and palette

### Requirement: The color picker is simple
The system SHALL offer a color picker made of a row of preset colors, common vehicle colors that the user selects with one tap, and of sliders for hue,
saturation and brightness that choose any other color, with a preview of the chosen color and its value written as #RRGGBB. Moving a slider or tapping a preset SHALL change the
chosen color and the palette preview at once. The picker SHALL be usable with the system keyboard closed and without any permission.

#### Scenario: Choose a preset
- **WHEN** the user taps a preset color
- **THEN** it becomes the chosen color, its value is shown as #RRGGBB and the palette preview changes

#### Scenario: Choose with the sliders
- **WHEN** the user moves the hue slider
- **THEN** the chosen color and the palette preview follow the slider

#### Scenario: The sliders follow a preset
- **WHEN** the user taps a preset color and then opens the sliders
- **THEN** the sliders show the hue, saturation and brightness of that preset

#### Scenario: The value is readable
- **WHEN** a color is chosen
- **THEN** the picker shows its value written as #RRGGBB

### Requirement: The palette of a main color is derived from it
The system SHALL derive the four-color palette of a chosen main color deterministically: the first color SHALL be the chosen color exactly, and the other three
SHALL be similar colors of the same family (analogous hues and lighter or darker variations) that are visibly different from each other and from the chosen
color. It SHALL give four different colors for every chosen color, including white, black and greys.

#### Scenario: The first color is the chosen one
- **WHEN** the user chooses the color #1E88E5
- **THEN** the first color of the derived palette is #1E88E5

#### Scenario: Similar colors
- **WHEN** the user chooses the color #1E88E5, a blue
- **THEN** the other three colors are blues or nearby hues, lighter or darker, none equal to another

#### Scenario: White has a palette too
- **WHEN** the user chooses the color #FFFFFF
- **THEN** the palette has four different colors and the first is #FFFFFF

#### Scenario: Black has a palette too
- **WHEN** the user chooses the color #000000
- **THEN** the palette has four different colors and the first is #000000

#### Scenario: Deterministic
- **WHEN** the same color is chosen twice
- **THEN** both derived palettes have the same four colors in the same order

### Requirement: The palette follows the picture and the color
The system SHALL choose a vehicle's palette by this rule: when the vehicle has a picture, the palette is the one extracted from the picture; otherwise, when it has a
main color, the palette is derived from the main color; otherwise it has none. The main color SHALL be kept when a picture is added, so that removing the picture makes the palette
come from the color again. Saving a vehicle without changing its picture or its main color SHALL keep its stored palette exactly as it is and SHALL NOT compute it again.

#### Scenario: A picture takes over
- **WHEN** the user adds a picture to a vehicle that has the main color #1E88E5 and saves
- **THEN** the vehicle's palette is the one extracted from the picture and its main color is still #1E88E5

#### Scenario: Removing the picture
- **WHEN** the user removes the picture of a vehicle that has the main color #1E88E5 and saves
- **THEN** the vehicle's palette is derived from #1E88E5

#### Scenario: Removing the picture without a color
- **WHEN** the user removes the picture of a vehicle that has no main color and saves
- **THEN** the vehicle has no palette

#### Scenario: An unrelated edit keeps the palette
- **WHEN** the user changes only the name of a vehicle and saves
- **THEN** the vehicle's palette has the same four colors in the same order

#### Scenario: Changing the picture
- **WHEN** the user changes the picture of a vehicle and saves
- **THEN** the vehicle's palette is the one extracted from the new picture

### Requirement: The forms show the palette
The system SHALL show the vehicle's four palette colors as swatches on the add and edit screens, in the palette's order, whenever the form has a palette, and SHALL update them as soon
as the crop of a picture is confirmed or a color is chosen. Each swatch SHALL be described to accessibility services with its value as "Color #RRGGBB". When the form has no picture and no
color, no swatches are shown.

#### Scenario: Swatches after cropping
- **WHEN** the user confirms the crop of a picture on the add screen
- **THEN** the screen shows four swatches with the colors extracted from the crop

#### Scenario: Swatches while choosing a color
- **WHEN** the user moves a slider of the color picker
- **THEN** the four swatches show the palette derived from the color at that moment

#### Scenario: Swatches of a saved vehicle
- **WHEN** the user opens the edit screen of a vehicle that has a palette
- **THEN** the screen shows its four stored colors

#### Scenario: No swatches without a source
- **WHEN** the user opens the add screen
- **THEN** no swatches are shown until a picture or a color is chosen

### Requirement: The palette is saved with the vehicle or not at all
The system SHALL save a new vehicle's palette and main color together with the vehicle, its initial odometer event and its picture, or save none of them, and SHALL apply a
changed palette and main color together with the other changes of an edit, or none of them. A failed save SHALL leave the vehicle's saved palette and main color as they were. Changing the
palette or the main color SHALL NOT change the vehicle's odometer, unit or log.

#### Scenario: Add a vehicle with a picture and a palette
- **WHEN** the user adds a vehicle, adds and crops a picture and saves
- **THEN** the vehicle is saved with its picture and the palette that was shown on the form

#### Scenario: A failed save
- **WHEN** saving a vehicle with a new picture or color fails
- **THEN** no vehicle is added or changed and the vehicle's earlier palette and main color are still in use

#### Scenario: The log is untouched
- **WHEN** the user changes only the main color of a vehicle and saves
- **THEN** the vehicle's odometer, unit and log are unchanged

### Requirement: Existing pictures get a palette
When the app starts, the system SHALL give a palette to every vehicle that has a picture but no palette (a picture saved before palettes existed), extracting it from the vehicle's stored picture in the
same way as from a confirmed crop. The system SHALL NOT change the palette of a vehicle that already has one. A picture that cannot be read SHALL be skipped without stopping the others or the app, and be tried again the next time the app starts.

#### Scenario: A picture saved before palettes existed
- **WHEN** the app starts and a vehicle has a picture but no palette
- **THEN** the vehicle gets a palette extracted from its picture

#### Scenario: Existing palettes are kept
- **WHEN** the app starts and a vehicle already has a palette
- **THEN** its palette is unchanged

#### Scenario: An unreadable picture
- **WHEN** the app starts and one vehicle's picture files cannot be read
- **THEN** that vehicle keeps having no palette, the other vehicles get theirs and the app starts normally

### Requirement: The palette does not change how the app looks
The system SHALL NOT use a vehicle's palette to color any screen, text, header or background in this change: the app's colors and theme SHALL be the same with or without palettes. Only
the swatches on the add and edit screens show it.

#### Scenario: Screens look the same
- **WHEN** a vehicle has a palette
- **THEN** the vehicle list and the details screen use the app's normal colors
