# vehicle-picture Specification

## Purpose
Lets users give a vehicle a picture: they choose or take a photo with the system's own image chooser, crop it to a square, and the app stores a
small and a large version of it in its own local file system. The small one shows the vehicle in lists and pickers, the large one
in full screen views such as the vehicle details.

## Requirements

### Requirement: A vehicle can have one optional picture
The system SHALL let a vehicle have at most one, optional, picture. A vehicle without one SHALL be shown with its
type's icon as a placeholder wherever its picture would be (`vehicle-type`); the add vehicle screen shows the icon of
the type chosen there (Car at first). The add and edit vehicle screens SHALL show a preview of the picture, or the
placeholder, labelled "Add picture" without a picture and "Change picture" with one, and SHALL offer "Remove picture"
only when there is a picture.

#### Scenario: A vehicle without a picture
- **WHEN** the user opens the add vehicle screen
- **THEN** the screen shows the placeholder as the preview (the icon of the type currently chosen, which is the car icon at first), labelled "Add picture", and offers no "Remove picture"

#### Scenario: The placeholder is a generic car icon
- **WHEN** the user opens the add vehicle screen and has not changed the preselected type
- **THEN** the picture preview shows the car icon in the place of the picture

#### Scenario: The placeholder follows the vehicle's type
- **WHEN** the vehicle list contains a vehicle of the type "Van" without a picture
- **THEN** its item shows the van icon in the place of the picture

#### Scenario: A vehicle with a picture
- **WHEN** the user opens the edit screen of a vehicle that has a picture
- **THEN** the screen shows that picture as the preview, labelled "Change picture", and offers "Remove picture"

#### Scenario: Tapping the picture starts choosing
- **WHEN** the user taps the preview on the add or edit screen
- **THEN** the system chooser of where the photo comes from is shown, and there is no other button that does so

#### Scenario: The preview shows a camera mark
- **WHEN** the user opens the add vehicle screen, and again the edit screen of a vehicle that has a picture
- **THEN** the corner of the preview shows a camera mark in both cases, and no pen mark

#### Scenario: The mark is not a separate control
- **WHEN** the user taps the camera mark
- **THEN** the system chooser is shown, as when tapping anywhere else on the preview, and the screen reader announces the preview as "Add picture" or "Change picture" and not the mark separately

### Requirement: The photo comes from an app the user chooses through the system
The system SHALL let the user choose where the photo comes from with the system's own mechanism for providing an
image, opened by "Add picture" or "Change picture": on Android the intent chooser of the apps that can supply an image
(the camera app included); on iOS the source sheet with Take Photo, Photo Library and Choose File. It SHALL NOT draw
its own picker or camera screen, SHALL accept any still image the chosen app returns, and SHALL NOT change or remove a
photo the user chose.

#### Scenario: The system asks which app provides the image
- **WHEN** the user taps "Add picture"
- **THEN** the system's chooser (on iOS the source sheet) is shown, listing the apps or sources that can provide an image, and the app draws no picker of its own

#### Scenario: The camera apps are listed as themselves on Android
- **WHEN** on Android the user taps "Add picture", whether or not camera access has been allowed
- **THEN** the chooser lists the installed camera apps, up to two, each under its own name and icon, next to every app that can provide an image

#### Scenario: Choose a photo
- **WHEN** the user chooses a photo in the app they picked
- **THEN** the crop screen opens with that photo

#### Scenario: Take a new photo
- **WHEN** the user chooses the camera app in the chooser and takes a photo
- **THEN** the crop screen opens with the new photo

#### Scenario: No permission is asked before it is needed
- **WHEN** the user opens the add vehicle screen, and again when the user chooses a photo from the library
- **THEN** no permission prompt is shown

#### Scenario: Camera access is asked for when taking a photo
- **WHEN** the user chooses the camera (Take Photo on iOS, a camera app in the chooser on Android) and the app has not been allowed camera access
- **THEN** the system asks for camera access at that moment, and the camera opens when it is allowed

#### Scenario: Camera access refused
- **WHEN** the user refuses camera access when asked, or has refused it before and chooses the camera again
- **THEN** the system opens no camera, keeps the vehicle's picture as it was, and shows below the picture the missing-permission message (`app-shell`): a photo can't be taken because camera access is turned off, with "Open settings"

#### Scenario: Leave the chooser
- **WHEN** the user opens the chooser and leaves it, or leaves the camera without taking a photo
- **THEN** the form is unchanged, and no missing-permission message is shown

#### Scenario: A file that is not an image
- **WHEN** the chosen app returns a file that cannot be decoded as an image
- **THEN** the system shows that the picture could not be opened, does not open the crop screen and keeps the vehicle's picture as it was

#### Scenario: The photo's orientation is respected
- **WHEN** the user chooses or takes a photo that is stored rotated and marked with an orientation
- **THEN** the crop screen shows it the right way up

**Platform note:** Android: `Intent.createChooser` over `ACTION_GET_CONTENT` for images, with up to two installed camera apps' own
`ACTION_IMAGE_CAPTURE` as initial intents, so each is listed under its own name and icon. The system chooser shows at most two initial
intents (`MAX_INITIAL_INTENTS` in IntentResolver); the image apps are its own targets and are not capped, and a device is far more likely
to have more than two image apps than more than two camera apps. The chooser's refinement step
(`EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER`), which runs after the user picks an app and before its launch, asks for the camera permission
when a camera app was picked and the permission isn't held. Because the app declares the camera permission (for the live scanner), Android
refuses `ACTION_IMAGE_CAPTURE` from the app while the permission isn't held. iOS: `PHPicker` for the library, `UIImagePickerController` for the camera and the document picker for files,
offered in a source sheet.

### Requirement: The photo must be cropped to a square
After a photo is chosen, the system SHALL open a crop screen showing the photo under a fixed square frame, and SHALL
NOT use the photo before the user confirms a crop. The user SHALL be able to move and zoom the photo, and the frame
SHALL stay inside it at all times: the smallest zoom makes the photo's shorter side fill the frame. The crop SHALL
start at the smallest zoom, centered, with the part outside the frame shown dimmed. There SHALL be no way to keep a
photo without cropping it.

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
- **WHEN** the user taps the back arrow on the crop screen, or uses the system's back
- **THEN** the form's picture is what it was before and nothing is stored

#### Scenario: The crop cannot be skipped
- **WHEN** the crop screen is displayed
- **THEN** besides the controls that move, zoom and turn the photo it offers only "Use photo" and back navigation (which cancels), and no way to use the photo without a crop

#### Scenario: Zoom with the buttons
- **WHEN** the user taps the zoom-in button and then the zoom-out button
- **THEN** the frame first covers a smaller part of the photo and then the part it covered before, and the buttons stop having an effect at the zoom limits

#### Scenario: Move with the keyboard
- **WHEN** the user has zoomed in and presses the left arrow key on a hardware keyboard
- **THEN** the photo moves under the frame by a step and stops at the photo's edge

#### Scenario: The crop screen is edge to edge
- **WHEN** the crop screen is open, in portrait or in landscape
- **THEN** its black background and the photo reach every edge of the screen behind the system bars, and its buttons are not covered by the status bar, the navigation bar or a display cutout

#### Scenario: Reset
- **WHEN** the user has moved and zoomed the photo and taps "Reset"
- **THEN** the frame covers the largest centered square again

#### Scenario: The controls are labelled
- **WHEN** a screen reader reads the crop screen
- **THEN** every control has a name ("Back", "Zoom in", "Zoom out", "Rotate photo", "Reset", "Use photo")

#### Scenario: Rotate the photo
- **WHEN** the user taps "Rotate photo" once
- **THEN** the photo is shown turned a quarter turn clockwise with the frame over the same part of it, and the frame still lies inside the photo

#### Scenario: Four turns are no turn
- **WHEN** the user taps "Rotate photo" four times
- **THEN** the photo and the frame are as they were

#### Scenario: The picture is made from the turned photo
- **WHEN** the user turns a sideways photo upright, and taps "Use photo"
- **THEN** the form's picture shows the photo upright, and the vehicle's color is taken from that picture

#### Scenario: The buttons are laid out by orientation
- **WHEN** the crop screen is shown in portrait, and then in landscape
- **THEN** in portrait "Zoom out" is to the left of "Zoom in" in a row under the photo, and in landscape "Zoom in" is above "Zoom out" in a column beside it

#### Scenario: The rest of the photo is visible, dimmed
- **WHEN** the crop screen shows a landscape photo at the smallest zoom
- **THEN** the parts of the photo left and right of the frame are drawn dimmed, not black

#### Scenario: The crop survives a rotation
- **WHEN** the user has zoomed in and moved the photo, and then rotates the device with the crop screen open
- **THEN** the crop screen shows the same part of the photo under the frame, turned as it was

### Requirement: A small and a large version are stored
The system SHALL store two versions of the cropped picture, both square and showing the same crop: a small one of at
most 256 x 256 pixels and a large one of at most 1024 x 1024 pixels. Each is scaled down to its target size and SHALL
NOT be enlarged: when the crop's side is shorter than the target, the version has the crop's size. Both SHALL be
stored as lossy WebP at quality 80 with transparency kept, so that the large version of a typical photo is well under
200 kilobytes.

#### Scenario: Sizes of a large photo
- **WHEN** the user crops a 4000 x 3000 photo to a 3000 x 3000 square and confirms
- **THEN** the stored small version is 256 x 256 pixels and the stored large version is 1024 x 1024 pixels

#### Scenario: A small photo is not enlarged
- **WHEN** the user crops a 600 x 400 photo to a 400 x 400 square and confirms
- **THEN** the stored large version is 400 x 400 pixels and the small version is 256 x 256 pixels

#### Scenario: A tiny crop is not enlarged at all
- **WHEN** the user crops a photo to a 200 x 200 square and confirms
- **THEN** both stored versions are 200 x 200 pixels

#### Scenario: Both versions show the same crop
- **WHEN** the user confirms a crop
- **THEN** the small and the large version show the same part of the photo

#### Scenario: Efficient format
- **WHEN** a picture is stored on Android
- **THEN** both files are WebP images and the large one is smaller than the same picture would be as a PNG

**Platform note:** iOS can read WebP but its image APIs cannot write it, so on iOS the versions are stored as PNG until a WebP encoder is added.
Every platform reads either format whatever platform wrote it.

### Requirement: Pictures are stored in the application's local file system
The system SHALL store picture files in the application's private storage, never in the photo library or another
shared location, and SHALL keep only a picture identifier with the vehicle's data, never a file path. Pictures SHALL
survive closing and reopening the app, and SHALL be shown and changed without a network connection. Replacing or
removing a picture SHALL delete its old files, and files no vehicle refers to (left by an interrupted save, say) SHALL
be deleted when the app starts.

#### Scenario: Survives a restart
- **WHEN** the user saves a vehicle with a picture, closes the app completely and opens it again
- **THEN** the vehicle's picture is still shown, in the list and on the details screen

#### Scenario: Offline use
- **WHEN** the device has no network connection and the user adds a picture to a vehicle
- **THEN** every step succeeds and no network error is shown

#### Scenario: Not in the photo library
- **WHEN** the user saves a vehicle with a picture
- **THEN** no new photo appears in the device's photo library

#### Scenario: Replaced pictures are deleted
- **WHEN** the user changes a vehicle's picture and saves
- **THEN** the files of the earlier picture no longer exist

#### Scenario: Unused files are cleaned up
- **WHEN** the app starts and the picture storage holds files that no vehicle refers to
- **THEN** those files are deleted and the pictures of vehicles are kept

### Requirement: A picture is saved with the vehicle or not at all
The system SHALL save a new vehicle's picture together with the vehicle and its initial odometer event, or none of
them, and SHALL apply a changed or removed picture together with the edit's other changes, or none of them. A
confirmed but unsaved picture SHALL be kept when the screen is rotated or recreated, and discarded with its files when
the user leaves the add or edit screen without saving. A failed save SHALL leave no unused picture files, and the
vehicle's saved picture as it was.

#### Scenario: Add a vehicle with a picture
- **WHEN** the user adds a vehicle, adds and crops a picture and saves
- **THEN** the vehicle appears in the list with that picture and its details screen shows it

#### Scenario: Cancel adding
- **WHEN** the user adds and crops a picture on the add screen and leaves the screen without saving
- **THEN** no vehicle is added and no picture files are left

#### Scenario: Rotate with an unsaved picture
- **WHEN** the user has confirmed a crop on the add or edit screen and rotates the device
- **THEN** the form still shows that picture

#### Scenario: A failed save
- **WHEN** saving a vehicle with a new picture fails
- **THEN** no vehicle is added or changed, the vehicle's earlier picture is still in use and no unused picture files are left

### Requirement: Change and remove a picture
The system SHALL let the user change or remove the picture of an existing vehicle from its edit screen. Changing SHALL go through
the photo picker and the crop like adding does. Removing SHALL make the vehicle have no picture once the edit is saved. Leaving the edit screen without saving
SHALL keep the vehicle's saved picture. Changing the picture SHALL NOT change the vehicle's odometer, unit or log.

#### Scenario: Change the picture
- **WHEN** the user edits a vehicle that has a picture, chooses and crops another photo and saves
- **THEN** the vehicle's list entry and details screen show the new picture

#### Scenario: Remove the picture
- **WHEN** the user taps "Remove picture" on the edit screen of a vehicle with a picture and saves
- **THEN** the vehicle has no picture: the list and the details screen show the placeholder

#### Scenario: Remove without saving
- **WHEN** the user taps "Remove picture" and leaves the edit screen without saving
- **THEN** the vehicle keeps its picture

#### Scenario: Only the picture changes
- **WHEN** the user changes only the picture of a vehicle and saves
- **THEN** the vehicle's name, plate, odometer, unit and log are unchanged

### Requirement: The small version shows the vehicle in lists and pickers, the large version in full screen views
The system SHALL show a vehicle's small picture wherever the vehicle is shown as an item among others (the vehicle list, and any
picker of vehicles), and SHALL show its large picture on views that show the vehicle full screen (the vehicle details screen). The
system SHALL NOT use the large version in a list or a picker, and SHALL show the placeholder when the vehicle has no picture or
its files cannot be read.

#### Scenario: The list shows the small picture
- **WHEN** the vehicle list contains a vehicle with a picture
- **THEN** its item shows the small version of the picture

#### Scenario: The details screen shows the large picture
- **WHEN** the user opens the details screen of a vehicle with a picture
- **THEN** the screen shows the large version of the picture

#### Scenario: Missing files
- **WHEN** a vehicle refers to a picture whose files cannot be read
- **THEN** the placeholder is shown in place of the picture and everything else works

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

### Requirement: Tapping the picture preview starts choosing a picture
Tapping the picture preview on the add or edit vehicle screen SHALL start choosing a picture; the system SHALL offer no
separate button for it. The preview SHALL show a small camera mark in its corner, the same camera with or without a
picture, on top of the picture or the placeholder, and SHALL NOT show an edit (pen) mark.

#### Scenario: Changing a picture by tapping it
- **WHEN** the user taps the preview showing a vehicle's picture on its edit screen
- **THEN** the system chooser of where the photo comes from is shown, as when adding a picture

### Requirement: An image that can't be used changes nothing
Leaving the system chooser, or the app chosen in it, without providing an image SHALL change nothing. When the image
returned cannot be read as an image, the system SHALL show a message that the picture could not be opened and SHALL
keep the vehicle's picture as it was.

#### Scenario: A file that is not an image when changing a picture
- **WHEN** on the edit screen of a vehicle with a picture, the user taps "Change picture" and the chosen app returns a
  file that cannot be decoded as an image
- **THEN** the system shows that the picture could not be opened, and the preview still shows the vehicle's picture

### Requirement: Permissions are asked for only when needed
The system SHALL ask for a permission only when the user triggers an action that needs it, at that moment, and SHALL
NOT ask for permissions the chosen way does not need: choosing a photo, file or other image app needs none. Taking a
photo with the camera needs camera access on both platforms. It SHALL be asked for when the user chooses the camera
and the app does not hold it. If it is refused, the system SHALL show the missing-permission message (`app-shell`)
and change nothing else.

#### Scenario: Opening the chooser on Android asks for nothing
- **WHEN** on Android the user taps "Add picture" on the add vehicle screen
- **THEN** the system chooser opens directly, with no permission prompt before it

#### Scenario: Choosing a photo app on Android asks for nothing
- **WHEN** on Android the user chooses a photo or file app in the chooser, whether or not camera access has been allowed
- **THEN** that app opens with no permission prompt

#### Scenario: Choosing the camera app without camera access on Android
- **WHEN** on Android the user chooses a camera app in the chooser and the app has not been allowed camera access
- **THEN** the system asks for camera access before the camera app opens, and the chosen camera app opens when it is allowed

#### Scenario: Camera access refused on Android
- **WHEN** on Android the user refuses camera access after choosing a camera app
- **THEN** no camera app opens, and the screen the user came from is shown as it was, with the missing-permission message and its "Open settings" action

#### Scenario: Camera prompt dismissed on Android
- **WHEN** on Android the user chooses a camera app and leaves the camera-access prompt without allowing it
- **THEN** no camera app opens, and the screen the user came from shows the missing-permission message, as for a refusal

#### Scenario: A refusal Android remembers
- **WHEN** on Android the user has refused camera access so that the system no longer asks, and chooses a camera app
- **THEN** no prompt is shown and no camera app opens, and the screen shows the missing-permission message and its "Open settings" action

#### Scenario: Camera access already allowed on Android
- **WHEN** on Android camera access has been allowed (for example for the live scanner) and the user chooses a camera app
- **THEN** the camera app opens with no permission prompt

#### Scenario: Camera access allowed from the message
- **WHEN** the missing-permission message is shown, the user taps "Open settings", allows camera access there and comes back
- **THEN** the message is gone, and choosing a camera app in the chooser opens it with no permission prompt

#### Scenario: Camera access withdrawn on Android
- **WHEN** on Android the user allowed camera access, then turned it off in the device settings, and chooses a camera app again
- **THEN** the system asks for camera access again before the camera app opens

### Requirement: The crop is confirmed or cancelled from the top app bar
The crop screen SHALL confirm the crop with "Use photo", a text action in its top app bar, and SHALL cancel by back
navigation (the app bar's back arrow or the system's back), which SHALL discard the chosen photo and leave the form as
it was. The top app bar SHALL follow the app's theme like the other screens' and be laid out as Material Design
specifies: a navigation icon, a title and a text action.

#### Scenario: The crop screen's app bar
- **WHEN** the crop screen is open
- **THEN** its top app bar shows the back arrow, a title and "Use photo" as a text action, in the app bar colors the
  other screens use

### Requirement: The crop can be adjusted without gestures
The crop screen SHALL let the user zoom without a gesture, with labelled buttons for zooming in and out, and return to
the start with a labelled "Reset" button; each button SHALL act once per tap. A hardware keyboard SHALL move the photo
with the arrow keys and zoom it with plus and minus. There SHALL be no buttons for moving the photo: the user drags it.

#### Scenario: Zoom with the keyboard
- **WHEN** the user presses plus and then minus on a hardware keyboard
- **THEN** the frame first covers a smaller part of the photo and then the part it covered before

### Requirement: The photo can be turned on the crop screen
The crop screen SHALL let the user turn the photo a quarter turn clockwise with a labelled "Rotate photo" button, which
SHALL keep the frame over the same part of the photo, and the picture SHALL be made from the photo as turned.

#### Scenario: Two turns
- **WHEN** the user taps "Rotate photo" twice
- **THEN** the photo is shown upside down, with the frame over the same part of it, still inside the photo

### Requirement: The crop screen is edge to edge
The crop screen SHALL fill the whole window, edge to edge: its black background and the photo SHALL reach behind the
system bars, while its buttons SHALL stay clear of them.

#### Scenario: The buttons stay clear of the navigation bar
- **WHEN** the crop screen is open in portrait on a device with a navigation bar
- **THEN** the black background reaches behind the navigation bar, and the zoom, turn and "Reset" buttons are above it

### Requirement: The crop survives a rotation and a process restart
The zoom and the position of the photo under the frame SHALL survive a rotation of the device and a restart of the
app's process while the crop screen is open.

#### Scenario: The crop survives a process restart
- **WHEN** the user has zoomed in and moved the photo, and the app's process is stopped and restored with the crop
  screen open
- **THEN** the crop screen shows the same part of the photo under the frame
