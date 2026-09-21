# Spec Delta

## MODIFIED Requirements

### Requirement: The photo must be cropped to a square
After a photo is chosen the system SHALL open a crop screen showing the photo under a fixed square frame, and SHALL NOT use the
photo before the user has confirmed a crop. The user SHALL be able to move the photo under the frame and to zoom it, and the
system SHALL keep the frame inside the photo at all times: the smallest zoom makes the shorter side of the photo fill the frame,
and the photo cannot be moved so that an edge of the frame leaves it. The crop SHALL start at the smallest zoom with the photo centered. The
user SHALL confirm with "Use photo" or cancel; cancelling SHALL discard the chosen photo and leave the form as it was. There SHALL be no
way to keep a photo without cropping it. The user SHALL also be able to move and zoom without a gesture, with labelled buttons for zooming in and out and for moving the photo a step
in each direction, and to return to the start with a labelled "Reset" button; a hardware keyboard SHALL do the same with the arrow keys, plus and minus. Each button acts once per tap. The user SHALL also be able to turn the photo a quarter turn clockwise with a labelled "Rotate photo" button, which keeps the frame over the same part of the photo, and the picture SHALL be made from the photo as turned. The part of the photo outside the frame
SHALL be shown dimmed, so that the user sees what is left out. The zoom and the position SHALL survive a rotation of the device and the restart of the app's process while the crop screen is open.

#### Scenario: The crop starts centered
- **WHEN** the crop screen opens with a landscape photo
- **THEN** the frame covers the largest centered square of the photo

#### Scenario: The frame stays inside the photo
- **WHEN** the user drags the photo so that far more than its width would pass the frame
- **THEN** the photo stops at the frame's edge and no part of the frame is empty

#### Scenario: Zoom in and out
- **WHEN** the user pinches to zoom in and then pinches to zoom out past the start
- **THEN** the zoom never goes below the smallest zoom and the frame stays inside the photo

#### Scenario: Confirm
- **WHEN** the user moves and zooms the photo and taps "Use photo"
- **THEN** the form shows the part of the photo inside the frame as its picture

#### Scenario: Cancel the crop
- **WHEN** the user taps cancel on the crop screen
- **THEN** the form's picture is what it was before and nothing is stored

#### Scenario: The crop cannot be skipped
- **WHEN** the crop screen is displayed
- **THEN** besides the controls that move, zoom and turn the photo it offers only "Use photo" and cancel, and no way to use the photo without a crop

#### Scenario: Zoom with the buttons
- **WHEN** the user taps the zoom-in button and then the zoom-out button
- **THEN** the frame first covers a smaller part of the photo and then the part it covered before, and the buttons stop having an effect at the zoom limits

#### Scenario: Move with the buttons
- **WHEN** the user has zoomed in and taps the button that moves the photo to the left
- **THEN** the photo moves under the frame by a step and stops at the photo's edge

#### Scenario: Reset
- **WHEN** the user has moved and zoomed the photo and taps "Reset"
- **THEN** the frame covers the largest centered square again

#### Scenario: The controls are labelled
- **WHEN** a screen reader reads the crop screen
- **THEN** every control has a name ("Zoom in", "Zoom out", "Move left", "Move right", "Move up", "Move down", "Rotate photo", "Reset", "Use photo", "Cancel")

#### Scenario: Rotate the photo
- **WHEN** the user taps "Rotate photo" once
- **THEN** the photo is shown turned a quarter turn clockwise with the frame over the same part of it, and the frame still lies inside the photo

#### Scenario: Four turns are no turn
- **WHEN** the user taps "Rotate photo" four times
- **THEN** the photo and the frame are as they were

#### Scenario: The picture is made from the turned photo
- **WHEN** the user turns a sideways photo upright, and taps "Use photo"
- **THEN** the form's picture shows the photo upright, and the vehicle's color is taken from that picture

#### Scenario: The rest of the photo is visible, dimmed
- **WHEN** the crop screen shows a landscape photo at the smallest zoom
- **THEN** the parts of the photo left and right of the frame are drawn dimmed, not black

#### Scenario: The crop survives a rotation
- **WHEN** the user has zoomed in and moved the photo, and then rotates the device with the crop screen open
- **THEN** the crop screen shows the same part of the photo under the frame, turned as it was

## ADDED Requirements

### Requirement: The versions are scaled with high quality
The system SHALL make the small and the large version by scaling the cropped part down once, at the time the crop is confirmed, with a method that does not
produce aliasing (jagged edges or moiré), and SHALL NOT enlarge it. The picture SHALL be drawn from the stored version that is nearest above the size it is shown at, without another scaling step at
full resolution.

#### Scenario: A fine pattern is not aliased
- **WHEN** the user crops a photo with a fine regular pattern (a grille, a brick wall) to a large square and confirms
- **THEN** the small version shows the pattern smoothed, without stripes or jagged edges that are not in the photo

#### Scenario: Drawn from the stored version
- **WHEN** the vehicle list shows a picture
- **THEN** it is drawn from the small version and the details screen from the large one, each at most scaled by a modest factor on the screen
